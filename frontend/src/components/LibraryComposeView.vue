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
  <section class="page-section">
    <el-card shadow="never" class="panel-card compose-card-shell">
      <template #header>
        <div class="card-header-block">
          <div>
            <h3>{{ form.id ? '编辑资料' : '新建资料' }}</h3>
            <p>支持分类、关联项目、正文编辑和附件上传</p>
          </div>
          <div class="toolbar-actions">
            <el-button @click="handleBack">返回知识库</el-button>
            <el-button type="primary" :loading="saving" @click="isDirty = false; emit('save')">保存资料</el-button>
          </div>
        </div>
      </template>

      <section class="compose-top-layout">
        <el-form label-position="top" class="project-form-grid compose-meta-form">
          <el-form-item label="标题" class="compose-field-span-2"><el-input v-model="form.title" placeholder="请输入资料标题" /></el-form-item>
          <el-form-item label="类型"><el-select v-model="form.type" placeholder="请选择资料类型" clearable><el-option v-for="item in (props.dictionaryOptions.knowledgeType || ['文档', '制度', '方案', '培训'])" :key="item" :label="item" :value="item" /></el-select></el-form-item>
          <el-form-item label="分类"><el-select v-model="form.categoryId" placeholder="请选择分类"><el-option v-for="category in flatCategoryOptions" :key="category.id" :label="`${'-- '.repeat(category.level)}${category.name}`" :value="String(category.id)" /></el-select></el-form-item>
          <el-form-item label="关联项目"><el-select v-model="form.projectId" clearable placeholder="可选关联项目"><el-option v-for="project in projects" :key="project.id" :label="project.name" :value="String(project.id)" /></el-select></el-form-item>
          <el-form-item label="标签"><el-input v-model="form.tags" placeholder="多个标签请用逗号分隔" /></el-form-item>
          <el-form-item label="来源"><el-input v-model="form.source" placeholder="如：项目沉淀 / 厂家资料 / 内部整理" /></el-form-item>
          <el-form-item label="摘要" class="compose-field-span-3 compose-summary-field"><el-input v-model="form.summary" type="textarea" :rows="6" resize="vertical" :maxlength="1000" show-word-limit placeholder="简要说明资料内容与用途" /></el-form-item>
        </el-form>

        <el-card
          shadow="never"
          class="panel-card compose-side-card"
          :class="{ 'is-drop-active': attachmentDropActive }"
          @dragover="onAttachmentDragOver"
          @dragleave="onAttachmentDragLeave"
          @drop="onAttachmentDrop"
        >
          <template #header>
            <div class="card-header-block compose-attachment-header">
              <div>
                <h3>附件</h3>
                <p>已有 {{ form.attachments?.length || 0 }} 个，待上传 {{ uploadQueue.length }} 个</p>
              </div>
              <el-button size="small" type="primary" @click="pickFiles">上传附件</el-button>
            </div>
          </template>

          <input ref="fileInputRef" class="native-file-input" type="file" multiple @change="emit('files-change', $event)" />
          <div class="compose-attachment-drop-tip">支持把文件直接拖到此区域上传</div>

          <div class="attachment-list editor-attachments compact-attachment-list" v-if="form.attachments?.length">
            <div v-for="attachment in form.attachments" :key="attachment.id" class="attachment-row compact-attachment-row">
              <div class="compact-attachment-meta">
                <strong :title="attachment.originalFileName">{{ attachment.originalFileName }}</strong>
                <span>{{ formatFileSize(attachment.fileSize) }}</span>
              </div>
              <el-space :size="6">
                <el-button link type="primary" @click="emit('open-preview', attachment)">预览</el-button>
                <el-button link type="danger" @click="emit('remove-attachment', { attachment, source: 'compose' })">删除</el-button>
              </el-space>
            </div>
          </div>
          <el-empty v-else description="暂无附件" :image-size="40" />

          <el-alert v-if="uploadQueue.length" type="info" show-icon :closable="false" class="queue-alert">
            <template #title>待上传文件</template>
            <div class="pending-files">
              <el-tag size="small" v-for="file in uploadQueue" :key="`${file.name}-${file.size}`" closable @close="emit('remove-queued-file', file.name)">{{ file.name }}</el-tag>
            </div>
          </el-alert>
        </el-card>
      </section>

      <section class="compose-editor-layout">
        <div class="compose-editor-item">
          <div class="compose-editor-header">
            <h4>正文（Markdown + 富工具栏）</h4>
            <div style="display: flex; gap: 8px;">
              <el-button size="small" @click="editorTheme = editorTheme === 'light' ? 'dark' : 'light'">切换主题</el-button>
              <el-button size="small" type="primary" @click="pickVideoFiles">插入视频</el-button>
            </div>
          </div>
          <MdEditor
            v-model="form.contentMarkdown"
            language="zh-CN"
            :theme="editorTheme"
            :toolbars="editorToolbars"
            :preview="true"
            :on-upload-img="onUploadImg"
            class="compose-markdown-editor"
            style="height: 620px"
          />
          <input ref="videoInputRef" class="native-file-input" type="file" accept="video/*" multiple @change="onVideoFileChange" />
        </div>
      </section>
    </el-card>
  </section>
</template>
