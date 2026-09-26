<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../services/api'
import { pushSkillDetail, popOverlay, getUiState, onUiState } from '../services/uiHistory'

const skills = ref([])
const selectedSkillId = ref('')
const readiness = ref(null)
const loadingSkills = ref(false)
const sending = ref(false)
const input = ref('')
const messages = ref([])
const chatBodyRef = ref(null)

const emit = defineEmits(['open-preview', 'open-item'])

// 侧边详情面板
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailItem = ref(null)

const selectedSkill = computed(() => skills.value.find((s) => s.id === selectedSkillId.value) || null)

const isAdmin = computed(() => {
  try {
    const raw = localStorage.getItem('knowledge-platform-auth')
    const parsed = raw ? JSON.parse(raw) : null
    return parsed?.user?.role === 'ADMIN'
  } catch {
    return false
  }
})

// ---- 技能管理（管理员）----
const skillManageOpen = ref(false)
const skillManageLoading = ref(false)
const allSkills = ref([])
const skillFormOpen = ref(false)
const skillSaving = ref(false)
const skillForm = reactive({
  dbId: null,
  id: '',
  name: '',
  description: '',
  icon: '',
  systemPrompt: '',
  tools: 'rag_search',
  sortOrder: 0,
  enabled: true,
})

async function loadAllSkills() {
  skillManageLoading.value = true
  try {
    const { data } = await api.get('/skills/admin')
    allSkills.value = data?.data || []
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载技能列表失败（需要管理员）')
  } finally {
    skillManageLoading.value = false
  }
}

function openSkillManage() {
  skillManageOpen.value = true
  loadAllSkills()
}

function openSkillCreate() {
  Object.assign(skillForm, {
    dbId: null,
    id: '',
    name: '',
    description: '',
    icon: '',
    systemPrompt: '',
    tools: 'rag_search',
    sortOrder: (allSkills.value.length || 0) * 10,
    enabled: true,
  })
  skillFormOpen.value = true
}

function openSkillEdit(row) {
  Object.assign(skillForm, {
    dbId: row.dbId ?? row.id ?? null,
    id: row.id || '',
    name: row.name || '',
    description: row.description || '',
    icon: row.icon || '',
    systemPrompt: row.systemPrompt || '',
    tools: Array.isArray(row.tools) ? row.tools.join(',') : (row.tools || 'rag_search'),
    sortOrder: row.sortOrder ?? 0,
    enabled: row.enabled !== false,
  })
  skillFormOpen.value = true
}

async function saveSkill() {
  if (!skillForm.id?.trim()) return ElMessage.warning('请填写技能编码')
  if (!skillForm.name?.trim()) return ElMessage.warning('请填写技能名称')
  if (!skillForm.systemPrompt?.trim()) return ElMessage.warning('请填写系统提示词')
  skillSaving.value = true
  try {
    const payload = {
      skillKey: skillForm.id.trim(),
      name: skillForm.name.trim(),
      description: skillForm.description,
      icon: skillForm.icon,
      systemPrompt: skillForm.systemPrompt,
      tools: String(skillForm.tools || 'rag_search').split(',').map((s) => s.trim()).filter(Boolean),
      sortOrder: Number(skillForm.sortOrder) || 0,
      enabled: skillForm.enabled !== false,
    }
    if (skillForm.dbId) {
      await api.put(`/skills/${skillForm.dbId}`, payload)
    } else {
      await api.post('/skills', payload)
    }
    ElMessage.success('技能已保存')
    skillFormOpen.value = false
    await loadAllSkills()
    await loadSkills()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    skillSaving.value = false
  }
}

async function deleteSkill(row) {
  try {
    await ElMessageBox.confirm(`确定删除技能「${row.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await api.delete(`/skills/${row.dbId ?? row.id}`)
    ElMessage.success('已删除')
    await loadAllSkills()
    await loadSkills()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

async function loadSkills() {
  loadingSkills.value = true
  try {
    const [skillRes, readyRes] = await Promise.all([
      api.get('/skills'),
      api.get('/skills/readiness').catch(() => ({ data: { data: { llmConfigured: false } } })),
    ])
    skills.value = skillRes?.data?.data || []
    readiness.value = readyRes?.data?.data || null
    if (!selectedSkillId.value && skills.value.length) {
      selectedSkillId.value = skills.value[0].id
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载技能列表失败')
  } finally {
    loadingSkills.value = false
  }
}

function chooseSkill(id) {
  selectedSkillId.value = id
  messages.value = []
}

function scrollToBottom() {
  nextTick(() => {
    const el = chatBodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function getAuthHeaders() {
  const headers = { 'Content-Type': 'application/json' }
  try {
    const raw = localStorage.getItem('knowledge-platform-auth')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed?.token) headers['X-Auth-Token'] = parsed.token
    }
  } catch { /* ignore */ }
  return headers
}

/** SSE 流式发送：token 逐字输出，sources 收尾 */
async function send() {
  const text = input.value.trim()
  if (!text) return
  if (!selectedSkillId.value) {
    ElMessage.warning('请先选择技能')
    return
  }
  if (readiness.value && readiness.value.llmConfigured === false) {
    ElMessage.warning('模型未配置：请设置 LLM_API_BASE_URL / LLM_API_KEY / LLM_MODEL')
    return
  }

  const history = messages.value.map((m) => ({ role: m.role, content: m.content }))
  const botMsg = reactive({
    role: 'assistant',
    content: '',
    statusText: '',
    statusShown: false,
    sources: [],
    error: false,
    streaming: true,
  })
  messages.value.push({ role: 'user', content: text, sources: [] })
  messages.value.push(botMsg)
  input.value = ''
  sending.value = true
  scrollToBottom()

  try {
    const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api$/, '')
    const resp = await fetch(`${base}/api/skills/chat/stream`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        skillId: selectedSkillId.value,
        message: text,
        history,
      }),
    })
    if (!resp.ok || !resp.body) {
      throw new Error(`流式接口失败 HTTP ${resp.status}`)
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 块以空行分隔
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const part of parts) {
        let eventName = 'message'
        let dataLine = ''
        for (const line of part.split('\n')) {
          if (line.startsWith('event:')) eventName = line.slice(6).trim()
          else if (line.startsWith('data:')) dataLine += line.slice(5).trim()
        }
        if (!dataLine) continue
        let data
        try { data = JSON.parse(dataLine) } catch { data = dataLine }

        if (eventName === 'token' && typeof data === 'string') {
          // 正文单独一行，不与「正在检索」拼在一起
          botMsg.content += data
          scrollToBottom()
        } else if (eventName === 'status' && typeof data === 'string') {
          // 状态只显示一次，固定在正文上方
          if (!botMsg.statusShown) {
            botMsg.statusText = data
            botMsg.statusShown = true
          }
          scrollToBottom()
        } else if (eventName === 'sources') {
          botMsg.sources = Array.isArray(data) ? data : []
        } else if (eventName === 'error') {
          botMsg.error = true
          botMsg.content += (botMsg.content ? '\n' : '') + `⚠️ ${typeof data === 'string' ? data : JSON.stringify(data)}`
        }
      }
    }

    if (!botMsg.content) botMsg.content = '（无回答）'
    botMsg.streaming = false
  } catch (e) {
    botMsg.error = true
    botMsg.content = `⚠️ ${e?.message || '对话失败'}`
    botMsg.streaming = false
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
}

function sourceLabel(src) {
  return src.locator || [src.title, src.section, src.page ? `p.${src.page}` : ''].filter(Boolean).join(' · ')
}

function parseSourceRef(src) {
  const path = String(src.path || '')
  // knowledge_items/attachment_61 → 附件 61（不是知识条目）
  let m = path.match(/^knowledge_items\/attachment_(\d+)/)
  if (m) return { kind: 'attachment', attachmentId: Number(m[1]), itemId: null }
  // attachments/{itemId}/{attId}/{filename}
  m = path.match(/^attachments\/(\d+)\/(\d+)\//)
  if (m) return { kind: 'attachment', itemId: Number(m[1]), attachmentId: Number(m[2]) }
  // knowledge_items/9 → 条目 9
  m = path.match(/^knowledge_items\/(\d+)/)
  if (m) {
    return {
      kind: 'item',
      itemId: Number(m[1]),
      attachmentId: Number(src.attachment_id || '') || null,
    }
  }
  if (src.attachment_id && Number.isFinite(Number(src.attachment_id))) {
    return {
      kind: 'attachment',
      attachmentId: Number(src.attachment_id),
      itemId: src.item_id && /^\d+$/.test(String(src.item_id)) ? Number(src.item_id) : null,
    }
  }
  if (src.item_id && /^\d+$/.test(String(src.item_id))) {
    return { kind: 'item', itemId: Number(src.item_id), attachmentId: null }
  }
  return { kind: 'unknown' }
}

/** 出处点击：附件 → 打开预览；条目 → 侧边详情 */
async function openItemPanel(src) {
  const ref = parseSourceRef(src)
  if (ref.kind === 'attachment' && ref.attachmentId) {
    emit('open-preview', { id: ref.attachmentId })
    return
  }
  if (ref.kind === 'item' && ref.itemId) {
    await loadItemDetail(ref.itemId)
    pushSkillDetail(ref.itemId)
    return
  }
  ElMessage.info('无法定位知识条目或附件')
}

async function loadItemDetail(id) {
  detailOpen.value = true
  detailLoading.value = true
  detailItem.value = null
  try {
    const { data } = await api.get(`/items/${id}`)
    detailItem.value = data?.data?.item || data?.data || null
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载知识条目失败')
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  if (detailOpen.value) {
    detailOpen.value = false
    detailItem.value = null
    popOverlay()
  }
}

function closeDetailSilent() {
  detailOpen.value = false
  detailItem.value = null
}

let offUiState = null

onMounted(() => {
  loadSkills()
  // 深链 / 前进后退：恢复侧栏详情
  offUiState = onUiState(async (state) => {
    if (state.skillDetailId) {
      if (!detailOpen.value || detailItem.value?.id !== state.skillDetailId) {
        await loadItemDetail(state.skillDetailId)
      }
    } else if (detailOpen.value) {
      closeDetailSilent()
    }
  })
  const state = getUiState()
  if (state.skillDetailId) {
    loadItemDetail(state.skillDetailId)
  }
})

onUnmounted(() => {
  if (offUiState) offUiState()
})

function detailAttachments() {
  return detailItem.value?.attachments || []
}

function formatDateTime(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatFileSize(size) {
  if (!size && size !== 0) return '--'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function fileExt(att) {
  const name = String(att?.originalFileName || att?.storedFileName || '')
  const idx = name.lastIndexOf('.')
  return idx >= 0 ? name.slice(idx + 1).toLowerCase() : 'file'
}

/** 与知识库详情页一致的轻量 Markdown 渲染 */
function renderMarkdown(markdown) {
  if (!markdown) return '<p>暂无内容</p>'
  const lines = String(markdown).split(/\r?\n/)
  const blocks = []
  let paragraph = []
  let list = []
  let code = []
  let inCode = false
  const inline = (text) => String(text)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/!\[[^\]]*\]\(([^)]+)\)/g, '')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
  const flushParagraph = () => {
    if (paragraph.length) {
      blocks.push('<p>' + inline(paragraph.join('<br />')) + '</p>')
      paragraph = []
    }
  }
  const flushList = () => {
    if (list.length) {
      blocks.push('<ul>' + list.map((item) => '<li>' + inline(item) + '</li>').join('') + '</ul>')
      list = []
    }
  }
  const flushCode = () => {
    if (code.length) {
      blocks.push('<pre><code>' + code.join('\n').replace(/&/g, '&amp;').replace(/</g, '&lt;') + '</code></pre>')
      code = []
    }
  }
  for (const line of lines) {
    if (line.startsWith('```')) {
      flushParagraph(); flushList()
      if (inCode) flushCode()
      inCode = !inCode
      continue
    }
    if (inCode) { code.push(line); continue }
    if (!line.trim()) { flushParagraph(); flushList(); continue }
    if (line.startsWith('# ')) { flushParagraph(); flushList(); blocks.push('<h1>' + inline(line.slice(2)) + '</h1>'); continue }
    if (line.startsWith('## ')) { flushParagraph(); flushList(); blocks.push('<h2>' + inline(line.slice(3)) + '</h2>'); continue }
    if (line.startsWith('### ')) { flushParagraph(); flushList(); blocks.push('<h3>' + inline(line.slice(4)) + '</h3>'); continue }
    if (line.startsWith('- ') || line.startsWith('* ')) { flushParagraph(); list.push(line.slice(2)); continue }
    paragraph.push(line)
  }
  flushParagraph(); flushList(); flushCode()
  return blocks.join('')
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api$/, '/api')
</script>

<template>
  <section class="page-section skill-page">
    <el-card shadow="never" class="panel-card skill-layout">
      <template #header>
        <div class="skill-head">
          <div>
            <h2>技能助手</h2>
            <p>公司统一技能入口：产品问答 · 售前话术 · 参数对照 · 标书响应 · 实施支持</p>
          </div>
          <div class="skill-head-actions">
            <el-tag v-if="readiness" :type="readiness.llmConfigured ? 'success' : 'danger'" size="small">
              {{ readiness.llmConfigured ? '模型已就绪' : '模型未配置' }}
            </el-tag>
            <el-button v-if="isAdmin" size="small" @click="openSkillManage">管理技能</el-button>
            <el-button size="small" @click="clearChat">清空对话</el-button>
          </div>
        </div>
      </template>

      <div class="skill-body" :class="{ 'has-detail': detailOpen }">
        <aside class="skill-list">
          <div v-loading="loadingSkills" class="skill-list-inner">
            <button
              v-for="skill in skills"
              :key="skill.id"
              type="button"
              class="skill-card"
              :class="{ active: skill.id === selectedSkillId }"
              @click="chooseSkill(skill.id)"
            >
              <div class="skill-card-title">
                <span class="skill-dot"></span>
                {{ skill.name }}
              </div>
              <div class="skill-card-desc">{{ skill.description }}</div>
            </button>
            <div v-if="!skills.length && !loadingSkills" class="skill-empty">暂无技能</div>
          </div>
        </aside>

        <main class="skill-chat">
          <div ref="chatBodyRef" class="skill-chat-body">
            <div v-if="!messages.length" class="skill-chat-empty">
              <p v-if="selectedSkill" class="skill-welcome-title">{{ selectedSkill.name }}</p>
              <p v-if="selectedSkill" class="skill-welcome-desc">{{ selectedSkill.description }}</p>
              <p class="skill-welcome-hint">输入问题开始对话，回答会标注知识库出处；点击出处可在右侧查看条目详情。</p>
            </div>

            <div
              v-for="(msg, idx) in messages"
              :key="idx"
              class="skill-msg"
              :class="msg.role === 'user' ? 'is-user' : 'is-bot'"
            >
              <div class="skill-msg-role">{{ msg.role === 'user' ? '我' : '助手' }}</div>
              <div class="skill-msg-bubble" :class="{ 'is-error': msg.error }">
                <div v-if="msg.statusText" class="skill-msg-status">{{ msg.statusText }}</div>
                <div class="skill-msg-text">{{ msg.content }}<span v-if="msg.streaming" class="skill-cursor">▍</span></div>
                <div v-if="msg.sources && msg.sources.length" class="skill-sources">
                  <div class="skill-sources-title">出处（点击查看知识条目）</div>
                  <button
                    v-for="(src, si) in msg.sources"
                    :key="si"
                    type="button"
                    class="skill-source-item skill-source-link"
                    @click="openItemPanel(src)"
                  >
                    <span class="skill-source-label">{{ sourceLabel(src) }}</span>
                    <code v-if="src.path" class="skill-source-path">{{ src.path }}</code>
                  </button>
                </div>
              </div>
            </div>

            <div v-if="sending && !messages.some((m) => m.streaming)" class="skill-msg is-bot">
              <div class="skill-msg-role">助手</div>
              <div class="skill-msg-bubble">思考中…</div>
            </div>
          </div>

          <div class="skill-input-bar">
            <el-input
              v-model="input"
              type="textarea"
              :rows="2"
              :placeholder="selectedSkill ? `向「${selectedSkill.name}」提问…` : '请先选择左侧技能'"
              :disabled="sending || !selectedSkill"
              @keydown.enter.exact.prevent="send"
            />
            <el-button type="primary" :loading="sending" :disabled="!selectedSkill" @click="send">
              发送
            </el-button>
          </div>
        </main>

        <!-- 知识条目详情侧边面板（结构对齐知识库详情页） -->
        <aside v-if="detailOpen" class="skill-detail-panel">
          <div class="skill-detail-head">
            <div class="skill-detail-title">知识条目详情</div>
            <el-button size="small" text @click="closeDetail">关闭</el-button>
          </div>
          <div v-loading="detailLoading" class="skill-detail-body">
            <template v-if="detailItem">
              <div class="skill-detail-title-row">
                <h3>{{ detailItem.title }}</h3>
                <el-tag effect="plain">{{ detailItem.categoryName || '未分类' }}</el-tag>
              </div>
              <textarea
                class="skill-detail-summary"
                :value="detailItem.summary || '暂无摘要'"
                readonly
                aria-label="摘要"
              ></textarea>

              <el-descriptions :column="1" border size="small" class="skill-detail-desc">
                <el-descriptions-item label="类型">{{ detailItem.type || '-' }}</el-descriptions-item>
                <el-descriptions-item label="标签">{{ detailItem.tags || '未设置标签' }}</el-descriptions-item>
                <el-descriptions-item label="来源">{{ detailItem.source || '未记录来源' }}</el-descriptions-item>
                <el-descriptions-item label="分类">{{ detailItem.categoryName || '未分类' }}</el-descriptions-item>
                <el-descriptions-item label="附件数">{{ detailAttachments().length }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ formatDateTime(detailItem.updatedAt || detailItem.createdAt) }}</el-descriptions-item>
              </el-descriptions>

              <el-card v-if="detailAttachments().length" class="skill-detail-att-card" shadow="never">
                <div class="skill-detail-section-title skill-detail-att-title">
                  <span>附件</span>
                  <span class="skill-detail-att-count">{{ detailAttachments().length }}</span>
                </div>
                <div
                  v-for="att in detailAttachments()"
                  :key="att.id"
                  class="skill-detail-att-row"
                >
                  <div class="skill-detail-att-icon" :class="`is-${fileExt(att)}`">
                    {{ fileExt(att).toUpperCase().slice(0, 4) }}
                  </div>
                  <div class="skill-detail-att-info">
                    <div class="skill-detail-att-name" :title="att.originalFileName || att.storedFileName">
                      {{ att.originalFileName || att.storedFileName }}
                    </div>
                    <div class="skill-detail-att-meta">
                      {{ formatFileSize(att.fileSize) }}
                      <span v-if="att.uploadedAt"> · {{ formatDateTime(att.uploadedAt) }}</span>
                    </div>
                  </div>
                  <div class="skill-detail-att-actions">
                    <el-button link type="primary" size="small" @click="emit('open-preview', att)">预览</el-button>
                    <el-button
                      link
                      type="primary"
                      size="small"
                      tag="a"
                      :href="`${apiBaseUrl}/attachments/${att.id}/download`"
                      target="_blank"
                      rel="noopener"
                    >下载</el-button>
                  </div>
                </div>
              </el-card>

              <el-card shadow="never" class="skill-detail-md-card">
                <div class="skill-detail-section-title">正文内容</div>
                <div class="skill-detail-markdown markdown-body" v-html="renderMarkdown(detailItem.contentMarkdown || '')"></div>
              </el-card>
            </template>
            <div v-else-if="!detailLoading" class="skill-empty">未找到对应资料</div>
          </div>
        </aside>
      </div>
    </el-card>

    <!-- 技能管理 -->
    <el-dialog v-model="skillManageOpen" title="管理技能" width="860px" destroy-on-close>
      <div class="skill-manage-toolbar">
        <el-button type="primary" size="small" @click="openSkillCreate">新增技能</el-button>
        <el-button size="small" @click="loadAllSkills">刷新</el-button>
      </div>
      <el-table :data="allSkills" v-loading="skillManageLoading" size="small" stripe>
        <el-table-column prop="id" label="编码" width="140" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column label="启用" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openSkillEdit(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="deleteSkill(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="skillFormOpen" :title="skillForm.dbId ? '编辑技能' : '新增技能'" width="720px" destroy-on-close>
      <el-form label-width="96px" label-position="left">
        <el-form-item label="技能编码" required>
          <el-input v-model="skillForm.id" placeholder="如 product-qa（创建后作对话路由）" :disabled="!!skillForm.dbId" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="skillForm.name" placeholder="如 产品知识问答" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="skillForm.description" type="textarea" :rows="2" placeholder="左侧卡片上展示的一句话说明" />
        </el-form-item>
        <el-form-item label="系统提示词" required>
          <el-input v-model="skillForm.systemPrompt" type="textarea" :rows="10" placeholder="技能行为说明，会作为 system 角色发给模型" />
        </el-form-item>
        <el-form-item label="工具">
          <el-input v-model="skillForm.tools" placeholder="逗号分隔，默认 rag_search" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="skillForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="skillForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="skillFormOpen = false">取消</el-button>
        <el-button type="primary" :loading="skillSaving" @click="saveSkill">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.skill-page {
  height: calc(100vh - var(--main-pad, 16px) * 2);
  height: calc(100dvh - var(--main-pad, 16px) * 2);
  min-height: 560px;
  display: flex;
  flex-direction: column;
}
.skill-page > .skill-layout {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.skill-layout :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.skill-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.skill-head h2 {
  margin: 0;
  font-size: 18px;
}
.skill-head p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.skill-head-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.skill-body {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 14px;
  flex: 1;
  min-height: 0;
}
.skill-body.has-detail {
  grid-template-columns: 220px 1fr 360px;
}
.skill-list {
  border-right: 1px solid var(--el-border-color-lighter);
  padding-right: 10px;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.skill-list-inner {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
  min-height: 0;
  flex: 1;
}
.skill-card {
  text-align: left;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
}
.skill-card:hover {
  border-color: var(--el-color-primary-light-5);
}
.skill-card.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.skill-card-title {
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.skill-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-color-primary);
}
.skill-card-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.skill-empty {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 20px 0;
  text-align: center;
}
.skill-chat {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}
.skill-chat-body {
  flex: 1;
  overflow: auto;
  padding: 4px 2px 12px;
  min-height: 0;
}
.skill-chat-empty {
  padding: 48px 12px;
  text-align: center;
  color: var(--el-text-color-secondary);
}
.skill-welcome-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 6px;
}
.skill-welcome-desc {
  margin: 0 0 8px;
  font-size: 13px;
}
.skill-welcome-hint {
  margin: 0;
  font-size: 12px;
}
.skill-msg {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.skill-msg.is-user {
  flex-direction: row-reverse;
}
.skill-msg-role {
  flex: 0 0 40px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  padding-top: 6px;
}
.skill-msg-bubble {
  max-width: 82%;
  border-radius: 10px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.65;
}
.skill-msg.is-user .skill-msg-bubble {
  background: var(--el-color-primary-light-8);
}
.skill-msg-bubble.is-error {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}
.skill-msg-status {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}
.skill-msg-text {
  white-space: pre-wrap;
  word-break: break-word;
}
.skill-cursor {
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
  color: var(--el-color-primary);
}
@keyframes blink {
  50% { opacity: 0; }
}
.skill-sources {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed var(--el-border-color);
}
.skill-sources-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.skill-source-item {
  font-size: 12px;
  margin-bottom: 3px;
}
.skill-source-link {
  display: block;
  width: 100%;
  text-align: left;
  padding: 4px 6px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: inherit;
}
.skill-source-link:hover {
  background: var(--el-color-primary-light-9);
}
.skill-source-link:hover .skill-source-label {
  text-decoration: underline;
}
.skill-source-path {
  color: var(--el-text-color-secondary);
  word-break: break-all;
  margin-left: 6px;
}
.skill-input-bar {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 10px;
}
.skill-input-bar .el-textarea {
  flex: 1;
}

/* 右侧详情面板 */
.skill-detail-panel {
  border-left: 1px solid var(--el-border-color-lighter);
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--el-fill-color-blank);
  border-radius: 8px;
}
.skill-detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.skill-detail-title {
  font-weight: 600;
  font-size: 14px;
}
.skill-detail-body {
  flex: 1;
  overflow: auto;
  padding: 12px;
  min-height: 0;
}
.skill-detail-item-title {
  margin: 0 0 8px;
  font-size: 16px;
  line-height: 1.4;
}
.skill-detail-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.skill-detail-title-row h3 {
  margin: 0;
  font-size: 16px;
  line-height: 1.4;
}
.skill-detail-summary {
  width: 100%;
  min-height: 56px;
  resize: none;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-light);
  margin-bottom: 10px;
}
.skill-detail-desc {
  margin-bottom: 10px;
}
.skill-detail-att-card,
.skill-detail-md-card {
  margin-top: 10px;
}
.skill-detail-att-card {
  border-radius: 10px;
}
.skill-detail-att-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 10px;
}
.skill-detail-att-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.skill-detail-att-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
  transition: border-color 0.15s ease, background 0.15s ease;
}
.skill-detail-att-row:last-child {
  margin-bottom: 0;
}
.skill-detail-att-row:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}
.skill-detail-att-icon {
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2px;
  background: var(--el-color-info-light-9);
  color: var(--el-color-info);
}
.skill-detail-att-icon.is-pdf { background: #fde2e2; color: #c45656; }
.skill-detail-att-icon.is-docx,
.skill-detail-att-icon.is-doc { background: #d6e8ff; color: #3370ff; }
.skill-detail-att-icon.is-xlsx,
.skill-detail-att-icon.is-xls,
.skill-detail-att-icon.is-xlsm { background: #d9f0e0; color: #2e8b57; }
.skill-detail-att-icon.is-pptx,
.skill-detail-att-icon.is-ppt { background: #ffe2c8; color: #d26b27; }
.skill-detail-att-icon.is-png,
.skill-detail-att-icon.is-jpg,
.skill-detail-att-icon.is-jpeg,
.skill-detail-att-icon.is-gif { background: #e7ddff; color: #7c4dff; }
.skill-detail-att-icon.is-zip,
.skill-detail-att-icon.is-rar,
.skill-detail-att-icon.is-7z { background: #f0e6d2; color: #a67c2a; }
.skill-detail-att-info {
  flex: 1;
  min-width: 0;
}
.skill-detail-att-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.skill-detail-att-meta {
  margin-top: 2px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.skill-detail-att-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}
.skill-detail-att-actions .el-button {
  padding: 4px 6px;
}
.skill-detail-markdown {
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}
.skill-detail-section-title {
  font-size: 13px;
  font-weight: 600;
  margin: 12px 0 6px;
}
.skill-detail-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--el-text-color-regular);
}
.skill-detail-att {
  font-size: 13px;
  margin-bottom: 4px;
}
.skill-detail-att a {
  color: var(--el-color-primary);
}

@media (max-width: 1100px) {
  .skill-body,
  .skill-body.has-detail {
    grid-template-columns: 1fr;
  }
  .skill-list {
    border-right: none;
    padding-right: 0;
    max-height: 160px;
  }
  .skill-detail-panel {
    max-height: 280px;
  }
}
</style>
