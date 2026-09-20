<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { api } from '../services/api'

const props = defineProps({
  form: { type: Object, required: true },
  flatCategoryOptions: { type: Array, required: true },
  projects: { type: Array, required: true },
  uploadQueue: { type: Array, required: true },
  saving: { type: Boolean, default: false },
  renderMarkdown: { type: Function, required: true },
  formatFileSize: { type: Function, required: true },
  previewOpen: { type: Boolean, default: false },
  dictionaryOptions: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['back', 'save', 'files-change', 'remove-queued-file', 'open-preview', 'remove-attachment', 'dirty-change'])
const fileInputRef = ref(null)
const videoInputRef = ref(null)
const editorTheme = ref('light')
const attachmentDropActive = ref(false)
const isDirty = ref(false)
const beforeUnloadHandler = (e) => {
  if (isDirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

function markDirty() {
  if (!isDirty.value) {
    isDirty.value = true
    emit('dirty-change', true)
  }
}

watch(
  () => [props.form.title, props.form.type, props.form.categoryId, props.form.tags, props.form.source, props.form.summary, props.form.contentMarkdown, props.form.projectId],
  () => markDirty(),
  { deep: false },
)

onMounted(() => {
  window.addEventListener('beforeunload', beforeUnloadHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnloadHandler)
  document.body.classList.remove('attachment-preview-open')
})

async function handleBack() {
  if (isDirty.value) {
    try {
      await ElMessageBox.confirm('当前内容尚未保存，确定要离开吗？', '未保存的更改', { type: 'warning', confirmButtonText: '离开', cancelButtonText: '留下' })
    } catch { return }
  }
  isDirty.value = false
  emit('dirty-change', false)
  emit('back')
}
const editorToolbars = [
  'bold', 'underline', 'italic', '-',
  'title', 'strikeThrough', 'sub', 'sup', 'quote', 'unorderedList', 'orderedList', 'task', '-',
  'codeRow', 'code', 'link', 'image', 'table', 'mermaid', 'katex', '-',
  'revoke', 'next', 'save', '=',
  'pageFullscreen', 'fullscreen', 'preview', 'previewOnly', 'htmlPreview', 'catalog',
]

const apiRoot = computed(() => (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api$/, ''))

watch(
  () => props.previewOpen,
  (open) => {
    if (typeof document === 'undefined') return
    document.body.classList.toggle('attachment-preview-open', !!open)
  },
  { immediate: true },
)

function pickFiles() {
  fileInputRef.value?.click()
}

function pickVideoFiles() {
  if (!props.form.id) {
    ElMessage({ type: 'warning', message: '请先保存一次资料，再插入图片/视频', showClose: true })
    return
  }
  videoInputRef.value?.click()
}

function emitSelectedFiles(files = []) {
  const list = Array.from(files || []).filter(Boolean)
  if (!list.length) return
  emit('files-change', { target: { files: list } })
}

function onAttachmentDragOver(event) {
  event.preventDefault()
  attachmentDropActive.value = true
}

function onAttachmentDragLeave(event) {
  event.preventDefault()
  attachmentDropActive.value = false
}

function onAttachmentDrop(event) {
  event.preventDefault()
  attachmentDropActive.value = false
  emitSelectedFiles(event?.dataTransfer?.files || [])
}

async function uploadMediaFiles(rawFiles = []) {
  if (!props.form.id) {
    ElMessage({ type: 'warning', message: '请先保存一次资料，再插入图片/视频', showClose: true })
    return []
  }

  const files = Array.from(rawFiles || [])
  const uploaded = []

  for (const file of files) {
    const formData = new FormData()
    formData.append('itemId', String(props.form.id))
    formData.append('file', file)

    try {
      const { data } = await api.post('/attachments/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      const payload = data?.data || {}
      const attachment = payload.attachment || payload
      const attachmentId = attachment?.id || payload?.id || null
      const directUrl = attachment?.url || attachment?.downloadUrl || payload?.url || payload?.downloadUrl || ''
      const url = directUrl || (attachmentId ? `${apiRoot.value}/api/attachments/${attachmentId}/download` : '')
      if (url) uploaded.push({ file, url })
    } catch {
      ElMessage({ type: 'error', message: `上传失败：${file.name}`, showClose: true })
    }
  }

  return uploaded
}

async function onUploadImg(files, callback) {
  const uploaded = await uploadMediaFiles(files)
  const urls = uploaded.map((item) => item.url).filter(Boolean)
  if (urls.length) callback(urls)
}

async function onVideoFileChange(event) {
  const files = Array.from(event?.target?.files || [])
  if (!files.length) return

  const uploaded = await uploadMediaFiles(files)
  if (uploaded.length) {
    const block = uploaded
      .map(({ file, url }) => `\n<video controls style="max-width:100%;" src="${url}" title="${file.name}"></video>\n`)
      .join('\n')
    props.form.contentMarkdown = `${props.form.contentMarkdown || ''}${block}`
    ElMessage({ type: 'success', message: `已插入 ${uploaded.length} 个视频`, showClose: true })
  }

  event.target.value = ''
}
</script>

<template>
  <section class="page-section compose-page">
    <div class="compose-topbar">
      <div class="compose-topbar-title">
        <strong>{{ form.id ? '编辑资料' : '新建资料' }}</strong>
        <span>{{ form.id ? '修改知识条目内容与附件' : '填写基础信息并编写正文' }}</span>
      </div>
      <div class="compose-topbar-actions">
        <el-button class="compose-cancel-btn" size="small" @click="handleBack">取消</el-button>
        <el-button class="compose-save-btn" size="small" type="primary" :loading="saving" @click="isDirty = false; emit('save')">保存</el-button>
      </div>
    </div>

    <div class="compose-body">
      <div class="compose-main">
        <el-card shadow="never" class="panel-card compose-meta-card">
          <template #header>
            <div class="compose-card-head">
              <strong>基础信息</strong>
            </div>
          </template>
          <el-form label-position="top" class="compose-meta-form">
            <el-form-item label="标题">
              <el-input v-model="form.title" placeholder="请输入资料标题" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="form.type" placeholder="请选择资料类型" clearable style="width: 100%">
                <el-option v-for="item in (props.dictionaryOptions.knowledgeType || ['文档', '制度', '方案', '培训'])" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="category in flatCategoryOptions" :key="category.id" :label="`${'— '.repeat(category.level)}${category.name}`" :value="String(category.id)" />
              </el-select>
            </el-form-item>
            <el-form-item label="关联项目">
              <el-select v-model="form.projectId" clearable placeholder="可选关联项目" style="width: 100%">
                <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="String(project.id)" />
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
            </el-form-item>
            <el-form-item label="来源">
              <el-input v-model="form.source" placeholder="项目沉淀 / 厂家资料 / 内部整理" />
            </el-form-item>
            <el-form-item label="摘要" class="compose-span-full">
              <el-input v-model="form.summary" type="textarea" :rows="2" resize="vertical" :maxlength="1000" show-word-limit placeholder="简要说明资料内容与用途" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="panel-card compose-editor-card">
          <template #header>
            <div class="compose-card-head">
              <strong>正文</strong>
              <div class="compose-editor-actions">
                <el-button size="small" @click="editorTheme = editorTheme === 'light' ? 'dark' : 'light'">主题</el-button>
                <el-button size="small" type="primary" @click="pickVideoFiles">插入视频</el-button>
              </div>
            </div>
          </template>
          <div class="compose-editor-box">
            <MdEditor
              v-model="form.contentMarkdown"
              language="zh-CN"
              :theme="editorTheme"
              :toolbars="editorToolbars"
              :preview="true"
              :on-upload-img="onUploadImg"
              class="compose-markdown-editor"
            />
          </div>
          <input ref="videoInputRef" class="native-file-input" type="file" accept="video/*" multiple @change="onVideoFileChange" />
        </el-card>
      </div>

      <el-card
        shadow="never"
        class="panel-card compose-side-card"
        :class="{ 'is-drop-active': attachmentDropActive }"
        @dragover="onAttachmentDragOver"
        @dragleave="onAttachmentDragLeave"
        @drop="onAttachmentDrop"
      >
        <template #header>
          <div class="compose-card-head">
            <strong>附件</strong>
            <span class="compose-side-count">{{ form.attachments?.length || 0 }} / 待传 {{ uploadQueue.length }}</span>
          </div>
        </template>

        <input ref="fileInputRef" class="native-file-input" type="file" multiple @change="emit('files-change', $event)" />
        <div class="compose-drop-zone">
          <p>拖拽文件到此处</p>
          <el-button size="small" type="primary" @click="pickFiles">选择文件</el-button>
        </div>

        <div class="compose-attach-list" v-if="form.attachments?.length || uploadQueue.length">
          <div v-if="uploadQueue.length" class="compose-attach-block">
            <div class="compose-attach-label">待上传</div>
            <div v-for="file in uploadQueue" :key="`${file.name}-${file.size}`" class="compose-attach-item">
              <span class="compose-attach-name" :title="file.name">{{ file.name }}</span>
              <el-button link type="danger" size="small" @click="emit('remove-queued-file', file.name)">移除</el-button>
            </div>
          </div>
          <div v-if="form.attachments?.length" class="compose-attach-block">
            <div class="compose-attach-label">已上传</div>
            <div v-for="attachment in form.attachments" :key="attachment.id" class="compose-attach-item">
              <div class="compose-attach-meta">
                <span class="compose-attach-name" :title="attachment.originalFileName">{{ attachment.originalFileName }}</span>
                <small>{{ formatFileSize(attachment.fileSize) }}</small>
              </div>
              <div class="compose-attach-actions">
                <el-button link type="primary" size="small" @click="emit('open-preview', attachment)">预览</el-button>
                <el-button link type="danger" size="small" @click="emit('remove-attachment', { attachment, source: 'compose' })">删除</el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无附件" :image-size="48" />
      </el-card>
    </div>
  </section>
</template>
