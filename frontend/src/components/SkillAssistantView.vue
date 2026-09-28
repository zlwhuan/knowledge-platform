<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../services/api'
import { pushSkillDetail, popOverlay, getUiState, onUiState } from '../services/uiHistory'

const skills = ref([])
const selectedSkillId = ref('')
const selectedSkillIds = ref([])
const readiness = ref(null)
const loadingSkills = ref(false)
const sending = ref(false)
const input = ref('')
const messages = ref([])
const chatBodyRef = ref(null)
const stopController = ref(null)
const textareaHeight = ref(88)
const draggingInput = ref(false)

// 临时附件 / 语音 / 模型 / 上下文
const sessionAttachments = ref([])
// 上传中/待发送的附件（悬浮在输入框上方，向量化完成后才能发送）
const pendingAttachments = ref([])
const uploading = ref(false)
const fileInputRef = ref(null)
const listening = ref(false)
const recognition = ref(null)
const voiceApplyingRef = ref(null)
const voiceOnManualEdit = ref(null)
const modelName = ref(localStorage.getItem('skill-model-name') || 'MiMo V2.6 Flash')
const modelNameOptions = ref(['MiMo V2.6 Flash', 'MiMo V2.6 Pro', 'MiMo V2.5'])
const skillPickerOpen = ref(false)
const skillPickerRef = ref(null)
const skillToggleRef = ref(null)

// 斜杠命令菜单
const slashMenuOpen = ref(false)
const slashQuery = ref('')
const slashActive = ref(0)
const slashMenuRef = ref(null)

const contextChars = computed(() => {
  let n = input.value.length
  for (const m of messages.value) n += String(m.content || '').length
  return n
})
// 中英混合粗估：中文约 1.5 字/token，英文约 4 字/token；统一按 2 字/token 保守估算
const CONTEXT_TOKEN_LIMIT = 1_000_000
const contextTokens = computed(() => Math.ceil(contextChars.value / 2))
const contextPct = computed(() =>
  Math.min(100, (contextTokens.value / CONTEXT_TOKEN_LIMIT) * 100)
)
const contextRingColor = computed(() => {
  const p = contextPct.value
  if (p >= 90) return 'var(--el-color-danger)'
  if (p >= 70) return 'var(--el-color-warning)'
  return 'var(--el-color-primary)'
})

// ---- 会话管理 ----
const sessions = ref([])
const currentSessionId = ref(null)
const loadingSessions = ref(false)
const LAST_SESSION_KEY = 'skill-last-session-id'
// 切页回来后，答案还没落库时显示「思考中」占位
const waitingAnswer = ref(false)

const emit = defineEmits(['open-preview', 'open-item'])

// 侧边详情面板
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailItem = ref(null)

const selectedSkill = computed(() => {
  if (selectedSkillIds.value.length === 1) {
    return skills.value.find((s) => s.id === selectedSkillIds.value[0]) || null
  }
  if (selectedSkillIds.value.length > 1) {
    return {
      id: 'combined',
      name: selectedSkillIds.value
        .map((id) => skills.value.find((s) => s.id === id)?.name || id)
        .join(' + '),
      description: '多技能组合',
    }
  }
  return null
})

function toggleSkill(id) {
  // 单选：点选即设为当前技能并收起面板
  if (selectedSkillIds.value[0] === id) {
    selectedSkillIds.value = []
    selectedSkillId.value = ''
  } else {
    selectedSkillIds.value = [id]
    selectedSkillId.value = id
  }
  skillPickerOpen.value = false
}

function toggleSkillPicker() {
  skillPickerOpen.value = !skillPickerOpen.value
}

function clearSkills() {
  selectedSkillIds.value = []
  selectedSkillId.value = ''
  skillPickerOpen.value = false
}

function onDocClickForSkillPicker(e) {
  if (!skillPickerOpen.value) return
  // 点「技能」按钮本身走 toggle，不算外部点击
  if (skillToggleRef.value && skillToggleRef.value.contains(e.target)) return
  const root = skillPickerRef.value
  if (root && !root.contains(e.target)) {
    skillPickerOpen.value = false
  }
}

function onDocClickForSlash(e) {
  if (!slashMenuOpen.value) return
  if (slashMenuRef.value && !slashMenuRef.value.contains(e.target)) {
    slashMenuOpen.value = false
  }
}

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
  tools: ['rag_search'],
  scopeCategoryIds: [],
  scopeItemIds: [],
  sortOrder: 0,
  enabled: true,
  ownerUsername: '',
})

// 技能管理：公共/个人 Tab + 复制模板
const skillManageTab = ref('public')
const skillCopyFrom = ref('')

// 技能可选工具（与后端 SkillChatService.buildTools 对齐）
const skillToolOptions = [
  { value: 'rag_search', label: 'rag_search', desc: '知识库混合检索（向量+BM25），带出处' },
  { value: 'get_item', label: 'get_item', desc: '按条目 ID 读完整详情/正文' },
  { value: 'get_param_table', label: 'get_param_table', desc: '读参数表（Markdown 表格结构化）' },
]
const skillToolList = computed({
  get: () => skillForm.tools,
  set: (v) => { skillForm.tools = v },
})

// 技能绑定：分类树 / 知识条目
const skillCategoryOptions = ref([])
const skillItemOptions = ref([])
const skillItemLoading = ref(false)

async function loadSkillCategoryOptions() {
  if (skillCategoryOptions.value.length) return
  try {
    const { data } = await api.get('/categories')
    const list = data?.data || []
    const flat = []
    const walk = (nodes, prefix) => {
      for (const n of nodes || []) {
        const label = prefix ? `${prefix} / ${n.name}` : n.name
        flat.push({ id: n.id, label })
        if (n.children?.length) walk(n.children, label)
      }
    }
    walk(list, '')
    skillCategoryOptions.value = flat
  } catch {
    skillCategoryOptions.value = []
  }
}

async function searchSkillItems(q) {
  skillItemLoading.value = true
  try {
    const { data } = await api.get('/items', {
      params: { keyword: q || '' },
    })
    const rows = data?.data || []
    skillItemOptions.value = (Array.isArray(rows) ? rows : []).slice(0, 30).map((r) => ({
      id: r.id,
      title: r.title || `条目 ${r.id}`,
    }))
  } catch {
    skillItemOptions.value = []
  } finally {
    skillItemLoading.value = false
  }
}

async function loadAllSkills() {
  skillManageLoading.value = true
  try {
    const { data } = await api.get('/skills/admin', { params: { scope: 'all' } })
    allSkills.value = data?.data || []
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载技能列表失败（需要管理员）')
  } finally {
    skillManageLoading.value = false
  }
}

const publicSkills = computed(() => allSkills.value.filter((s) => !s.ownerUsername))
const personalSkills = computed(() => allSkills.value.filter((s) => !!s.ownerUsername))
const tabSkills = computed(() =>
  skillManageTab.value === 'public' ? publicSkills.value : personalSkills.value
)

function openSkillManage() {
  skillManageOpen.value = true
  skillManageTab.value = 'public'
  loadAllSkills()
  loadSkillCategoryOptions()
}

function openSkillCreate() {
  Object.assign(skillForm, {
    dbId: null,
    id: '',
    name: '',
    description: '',
    icon: '',
    systemPrompt: '',
    tools: ['rag_search'],
    scopeCategoryIds: [],
    scopeItemIds: [],
    sortOrder: (allSkills.value.length || 0) * 10,
    enabled: true,
    ownerUsername: skillManageTab.value === 'personal' ? currentUsername() : '',
  })
  skillCopyFrom.value = ''
  skillFormOpen.value = true
}

function currentUsername() {
  try {
    const raw = localStorage.getItem('knowledge-platform-auth')
    return raw ? (JSON.parse(raw)?.user?.username || '') : ''
  } catch { return '' }
}

/** 从公共技能复制（覆盖表单，编码需另填） */
function applyCopyFrom() {
  const src = publicSkills.value.find((s) => (s.dbId ?? s.id) === skillCopyFrom.value || s.id === skillCopyFrom.value)
  if (!src) return
  Object.assign(skillForm, {
    name: src.name ? `${src.name}-副本` : '',
    description: src.description || '',
    icon: src.icon || '',
    systemPrompt: src.systemPrompt || '',
    tools: Array.isArray(src.tools) ? [...src.tools] : ['rag_search'],
    scopeCategoryIds: src.scopeCategoryIds || [],
    scopeItemIds: src.scopeItemIds || [],
    enabled: true,
    ownerUsername: skillForm.ownerUsername || currentUsername(),
  })
  if (!skillForm.id) {
    skillForm.id = `${src.id || 'skill'}-copy`
  }
}

function openSkillEdit(row) {
  Object.assign(skillForm, {
    dbId: row.dbId ?? row.id ?? null,
    id: row.id || '',
    name: row.name || '',
    description: row.description || '',
    icon: row.icon || '',
    systemPrompt: row.systemPrompt || '',
    tools: Array.isArray(row.tools) ? [...row.tools] : String(row.tools || 'rag_search').split(',').map((s) => s.trim()).filter(Boolean),
    scopeCategoryIds: row.scopeCategoryIds || [],
    scopeItemIds: row.scopeItemIds || [],
    sortOrder: row.sortOrder ?? 0,
    enabled: row.enabled !== false,
    ownerUsername: row.ownerUsername || '',
  })
  skillCopyFrom.value = ''
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
      tools: Array.isArray(skillForm.tools) ? skillForm.tools.filter(Boolean) : [],
      scopeCategoryIds: skillForm.scopeCategoryIds || [],
      scopeItemIds: skillForm.scopeItemIds || [],
      sortOrder: Number(skillForm.sortOrder) || 0,
      enabled: skillForm.enabled !== false,
      ownerUsername: skillForm.ownerUsername || '',
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
    // 默认不预选技能，由用户显式选择
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载技能列表失败')
  } finally {
    loadingSkills.value = false
  }
}

function chooseSkill(id) {
  // 兼容旧调用：等价于切换技能
  toggleSkill(id)
  messages.value = []
  currentSessionId.value = null
}

async function loadSessions() {
  loadingSessions.value = true
  try {
    const { data } = await api.get('/skills/sessions')
    sessions.value = data?.data || []
  } catch {
    sessions.value = []
  } finally {
    loadingSessions.value = false
  }
}

async function createSession() {
  try {
    const { data } = await api.post('/skills/sessions', {
      skillId: selectedSkillIds.value[0] || selectedSkillId.value || '',
      title: '新会话',
    })
    const s = data?.data
    if (s?.id) {
      currentSessionId.value = s.id
      sessionStorage.setItem(LAST_SESSION_KEY, String(s.id))
      messages.value = []
      sessionAttachments.value = []
      await loadSessions()
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '新建会话失败')
  }
}

async function openSession(s) {
  currentSessionId.value = s.id
  sessionStorage.setItem(LAST_SESSION_KEY, String(s.id))
  sessionAttachments.value = []
  pendingAttachments.value = []
  skillPickerOpen.value = false
  if (s.skillId && s.skillId !== 'combined') {
    selectedSkillIds.value = [s.skillId]
    selectedSkillId.value = s.skillId
  }
  messages.value = []
  try {
    const [msgRes, attRes] = await Promise.all([
      api.get(`/skills/sessions/${s.id}/messages`),
      api.get(`/skills/sessions/${s.id}/attachments`).catch(() => null),
    ])
    const list = msgRes?.data?.data || []
    const atts = (attRes?.data?.data || []).map((a) => ({
      role: 'user',
      type: 'attachment',
      filename: a.filename,
      chars: a.chars || 0,
      previewKey: `${s.id}_${a.id}`,
      content: '',
      sources: [],
      traces: [],
      tracesOpen: false,
      elapsedMs: 0,
      createdAt: a.createdAt || '',
    }))
    // 按时间线合并：附件插在它上传时刻的位置，而不是全部堆到最上
    const msgs = list.map((m) => ({
      role: m.role,
      type: 'text',
      content: m.content || '',
      sources: m.sources || [],
      traces: m.traces || [],
      tracesOpen: false,
      elapsedMs: 0,
      statusText: '',
      error: false,
      streaming: false,
      createdAt: m.createdAt || '',
    }))
    messages.value = [...msgs, ...atts].sort((a, b) => {
      const ta = a.createdAt ? Date.parse(a.createdAt) : 0
      const tb = b.createdAt ? Date.parse(b.createdAt) : 0
      return ta - tb
    })
    sessionAttachments.value = (attRes?.data?.data || []).map((a) => ({
      id: a.id,
      filename: a.filename,
      chars: a.chars || 0,
    }))
    scrollToBottom()
    // 最后一条是用户消息 → 后端可能还在生成，轮询补拉
    maybePollPendingAnswer(s.id)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载会话失败')
  }
}

/** 切页回来时：若回答还没落库，轮询直到出现或超时 */
function maybePollPendingAnswer(sessionId) {
  const last = messages.value[messages.value.length - 1]
  if (!last || last.role !== 'user' || last.type === 'attachment') {
    waitingAnswer.value = false
    return
  }
  waitingAnswer.value = true
  let tries = 0
  const timer = setInterval(async () => {
    tries += 1
    if (currentSessionId.value !== sessionId || tries > 30) {
      clearInterval(timer)
      waitingAnswer.value = false
      return
    }
    try {
      const { data } = await api.get(`/skills/sessions/${sessionId}/messages`)
      const list = data?.data || []
      const lastMsg = list[list.length - 1]
      if (lastMsg && lastMsg.role === 'assistant') {
        clearInterval(timer)
        waitingAnswer.value = false
        await openSession({ id: sessionId, skillId: selectedSkillId.value })
      }
    } catch {
      clearInterval(timer)
      waitingAnswer.value = false
    }
  }, 2000)
}

async function deleteSession(s, ev) {
  ev?.stopPropagation?.()
  try {
    await ElMessageBox.confirm(`删除会话「${s.title || '未命名'}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await api.delete(`/skills/sessions/${s.id}`)
    if (currentSessionId.value === s.id) {
      currentSessionId.value = null
      messages.value = []
      sessionStorage.removeItem(LAST_SESSION_KEY)
    }
    await loadSessions()
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
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

/** 强制停止当前流式对话 */
function stopStream() {
  if (stopController.value) {
    stopController.value.abort()
    stopController.value = null
  }
  sending.value = false
}

/** 计算 textarea 光标在内容中的 y 坐标（含自动换行） */
function getCaretY(textarea, position) {
  const style = window.getComputedStyle(textarea)
  const mirror = document.createElement('div')
  const props = [
    'fontFamily', 'fontSize', 'fontWeight', 'fontStyle', 'letterSpacing',
    'lineHeight', 'textTransform', 'wordSpacing', 'textIndent',
    'paddingTop', 'paddingLeft', 'paddingRight', 'paddingBottom',
    'borderTopWidth', 'borderLeftWidth', 'boxSizing', 'whiteSpace',
    'wordWrap', 'overflowWrap', 'tabSize',
  ]
  for (const p of props) mirror.style[p] = style[p]
  mirror.style.position = 'absolute'
  mirror.style.visibility = 'hidden'
  mirror.style.pointerEvents = 'none'
  mirror.style.top = '0'
  mirror.style.left = '-9999px'
  mirror.style.width = textarea.clientWidth + 'px'
  mirror.style.whiteSpace = 'pre-wrap'
  mirror.style.wordWrap = 'break-word'
  mirror.textContent = textarea.value.substring(0, position)
  const span = document.createElement('span')
  span.textContent = '​'
  mirror.appendChild(span)
  document.body.appendChild(mirror)
  const y = span.offsetTop
  const h = span.offsetHeight || parseFloat(style.lineHeight) || 20
  document.body.removeChild(mirror)
  return { y, h }
}

/** 让光标所在行始终可见（换行/移动光标时滚动条跟随） */
function scrollCaretIntoView(el) {
  if (!el || el.tagName !== 'TEXTAREA') return
  const pos = el.selectionEnd ?? el.value.length
  const { y, h } = getCaretY(el, pos)
  const viewTop = el.scrollTop
  const viewBottom = el.scrollTop + el.clientHeight
  if (y < viewTop) {
    el.scrollTop = Math.max(0, y - 4)
  } else if (y + h > viewBottom) {
    el.scrollTop = y + h - el.clientHeight + 4
  }
}

function syncCaretScroll(e) {
  scrollCaretIntoView(e.target)
}

/** 输入框快捷键：Enter 发送 / Ctrl+Enter 换行 / ↑↓ 选命令 / Esc 关菜单 */
function onComposerKeydown(e) {
  // 斜杠菜单打开时优先处理导航
  if (slashMenuOpen.value) {
    const cmds = filteredSlashCommands.value
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      if (cmds.length) slashActive.value = (slashActive.value + 1) % cmds.length
      return
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      if (cmds.length) slashActive.value = (slashActive.value - 1 + cmds.length) % cmds.length
      return
    }
    if (e.key === 'Enter' && !e.ctrlKey && !e.metaKey && !e.shiftKey) {
      e.preventDefault()
      const cmd = cmds[slashActive.value]
      if (cmd) runSlashCommand(cmd)
      return
    }
    if (e.key === 'Escape') {
      e.preventDefault()
      slashMenuOpen.value = false
      return
    }
  }

  // Ctrl/Cmd+Enter：换行（不发送）
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    const el = e.target
    const start = el.selectionStart ?? input.value.length
    const end = el.selectionEnd ?? start
    input.value = input.value.slice(0, start) + '\n' + input.value.slice(end)
    nextTick(() => {
      el.selectionStart = el.selectionEnd = start + 1
      scrollCaretIntoView(el)
    })
    return
  }

  // Shift+Enter：原生换行，换完滚动到光标
  if (e.key === 'Enter' && e.shiftKey) {
    nextTick(() => scrollCaretIntoView(e.target))
    return
  }

  // Enter（无修饰键）：发送
  if (e.key === 'Enter' && !e.shiftKey && !e.altKey) {
    e.preventDefault()
    send()
    return
  }

  // 方向键/翻页后把光标行滚进可视区
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp' || e.key === 'PageDown' || e.key === 'PageUp' || e.key === 'Home' || e.key === 'End') {
    nextTick(() => scrollCaretIntoView(e.target))
  }
}

function onComposerInput(e) {
  // 录音中手动改字 → 更新语音基线（程序写入 input 不算手动）
  const isVoiceApply = typeof voiceApplyingRef.value === 'function' && voiceApplyingRef.value()
  if (!isVoiceApply && typeof voiceOnManualEdit.value === 'function') {
    voiceOnManualEdit.value()
  }
  const val = input.value
  // 仅在「行首且只敲了 /…」时唤起命令菜单
  if (val === '/' || (val.startsWith('/') && !val.includes('\n') && val.length <= 40)) {
    slashQuery.value = val.slice(1)
    slashMenuOpen.value = true
    slashActive.value = 0
  } else {
    slashMenuOpen.value = false
  }
  // 输入后光标行保持可见
  if (e?.target) scrollCaretIntoView(e.target)
}

const slashCommands = computed(() => {
  return skills.value.map((s) => ({
    key: `skill:${s.id}`,
    label: s.name,
    desc: s.description || '切换到该技能',
    kind: 'skill',
    payload: s,
  }))
})

const filteredSlashCommands = computed(() => {
  const q = slashQuery.value.trim().toLowerCase()
  if (!q) return slashCommands.value
  return slashCommands.value.filter(
    (c) => c.label.toLowerCase().includes(q) || c.desc.toLowerCase().includes(q)
  )
})

function runSlashCommand(cmd) {
  slashMenuOpen.value = false
  input.value = ''
  if (cmd.kind === 'skill') {
    toggleSkill(cmd.payload.id)
  }
}

function clampInputHeight(h) {
  const min = 72 // 约两行
  const max = Math.round(window.innerHeight * 0.5)
  return Math.max(min, Math.min(max, h))
}

function startInputDrag(e) {
  e.preventDefault()
  draggingInput.value = true
  const startY = e.clientY
  const startH = textareaHeight.value
  const onMove = (ev) => {
    textareaHeight.value = clampInputHeight(startH - (ev.clientY - startY))
  }
  const onUp = () => {
    draggingInput.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

async function ensureSession() {
  if (currentSessionId.value) return currentSessionId.value
  try {
    const title = (input.value.trim() || '新会话').slice(0, 10)
    const { data } = await api.post('/skills/sessions', {
      skillId: selectedSkillIds.value[0] || selectedSkillId.value || '',
      title,
    })
    const s = data?.data
    if (s?.id) {
      currentSessionId.value = s.id
      await loadSessions()
      return s.id
    }
  } catch {
    // 建会话失败也继续对话，只是不落库
  }
  return null
}

function pickFile() {
  fileInputRef.value?.click()
}

async function onFileChange(e) {
  const file = e.target?.files?.[0]
  e.target.value = ''
  if (!file) return
  const sid = await ensureSession()
  if (!sid) {
    ElMessage.warning('请先开始会话')
    return
  }

  const pend = reactive({
    key: `pend_${Date.now()}`,
    filename: file.name,
    chars: 0,
    progress: 8,
    status: 'processing', // processing | ready | error
    message: '抽取文本中…',
    attachmentId: null,
    sessionId: sid,
  })
  pendingAttachments.value.push(pend)
  uploading.value = true

  // 伪进度：抽取+向量化耗时不定，最后由响应拉满
  const tick = setInterval(() => {
    if (pend.progress < 92) {
      pend.progress = Math.min(92, pend.progress + (pend.progress < 40 ? 7 : 3))
      if (pend.progress > 35 && pend.status === 'processing') pend.message = '向量化入库中…'
    }
  }, 280)

  try {
    const form = new FormData()
    form.append('file', file)
    const { data } = await api.post(`/skills/sessions/${sid}/attachments`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000,
    })
    if (data?.success === false) {
      pend.status = 'error'
      pend.message = data?.message || '附件处理失败'
      pend.progress = 100
      ElMessage.error(pend.message)
      return
    }
    pend.progress = 100
    pend.status = 'ready'
    pend.message = '已就绪'
    pend.chars = data?.data?.chars || 0
    pend.attachmentId = data?.data?.attachmentId || String(Date.now())
  } catch (err) {
    pend.status = 'error'
    pend.progress = 100
    pend.message = err?.response?.data?.message || '附件上传失败'
    ElMessage.error(pend.message)
  } finally {
    clearInterval(tick)
    uploading.value = pendingAttachments.value.some((p) => p.status === 'processing')
  }
}

function removePendingAttachment(pend) {
  pendingAttachments.value = pendingAttachments.value.filter((p) => p.key !== pend.key)
  uploading.value = pendingAttachments.value.some((p) => p.status === 'processing')
}

/** 把已就绪附件写入对话时间线（发送时才出现） */
function flushPendingAttachments() {
  const ready = pendingAttachments.value.filter((p) => p.status === 'ready')
  for (const p of ready) {
    sessionAttachments.value.push({
      id: p.attachmentId,
      filename: p.filename,
      chars: p.chars,
    })
    messages.value.push({
      role: 'user',
      type: 'attachment',
      filename: p.filename,
      chars: p.chars,
      previewKey: `${p.sessionId}_${p.attachmentId}`,
      content: '',
      sources: [],
      traces: [],
      tracesOpen: false,
      elapsedMs: 0,
      createdAt: new Date().toISOString(),
    })
  }
  pendingAttachments.value = pendingAttachments.value.filter((p) => p.status !== 'ready')
  uploading.value = pendingAttachments.value.some((p) => p.status === 'processing')
  return ready.length
}

function openAttachmentPreview(msg) {
  if (!msg?.previewKey) return
  emit('open-preview', {
    id: msg.previewKey,
    isSessionAttachment: true,
    originalFileName: msg.filename,
  })
}

function removeSessionAttachment(att) {
  sessionAttachments.value = sessionAttachments.value.filter((a) => a.id !== att.id)
  if (currentSessionId.value && att.id) {
    api.delete(`/skills/sessions/${currentSessionId.value}/attachments/${att.id}`).catch(() => {})
  }
}

function toggleVoice() {
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SR) {
    ElMessage.warning('当前浏览器不支持语音输入')
    return
  }
  if (listening.value) {
    recognition.value?.stop?.()
    return
  }
  const rec = new SR()
  rec.lang = 'zh-CN'
  rec.interimResults = true
  rec.continuous = true

  // 基线 = 开始录音时已有内容；识别结果追加，不重置
  let base = input.value
  let finalAcc = ''
  let interim = ''
  let applyingVoice = false

  const applyVoice = () => {
    applyingVoice = true
    input.value = [base, finalAcc, interim].filter(Boolean).join(' ')
    nextTick(() => {
      applyingVoice = false
      const el = skillTextareaEl()
      if (el) {
        el.focus()
        el.selectionStart = el.selectionEnd = input.value.length
        scrollCaretIntoView(el)
      }
    })
  }

  voiceApplyingRef.value = () => applyingVoice
  voiceOnManualEdit.value = () => {
    // 录音中用户手动改字：把当前框内内容收作新基线，后续语音继续追加
    if (listening.value && !applyingVoice) {
      base = input.value
      finalAcc = ''
      interim = ''
    }
  }

  rec.onresult = (ev) => {
    interim = ''
    for (let i = ev.resultIndex; i < ev.results.length; i++) {
      const t = ev.results[i][0].transcript
      if (ev.results[i].isFinal) {
        finalAcc += t
      } else {
        interim += t
      }
    }
    applyVoice()
  }
  rec.onend = () => {
    listening.value = false
    // 收尾：临时串并入正文
    if (interim) {
      finalAcc += interim
      interim = ''
      input.value = [base, finalAcc].filter(Boolean).join(' ')
    }
    voiceOnManualEdit.value = null
    voiceApplyingRef.value = null
  }
  rec.onerror = () => {
    listening.value = false
    voiceOnManualEdit.value = null
    voiceApplyingRef.value = null
    ElMessage.warning('语音识别中断')
  }
  rec.start()
  recognition.value = rec
  listening.value = true
  // 光标保持在输入框
  nextTick(() => skillTextareaEl()?.focus())
}

function skillTextareaEl() {
  return document.querySelector('.skill-textarea textarea')
}

function setModel(name) {
  modelName.value = name
  localStorage.setItem('skill-model-name', name)
}

/** SSE 流式发送：token 逐字输出，sources 收尾 */
async function send() {
  // 向量化未完成时禁止发送
  const processing = pendingAttachments.value.filter((p) => p.status === 'processing')
  if (processing.length) {
    ElMessage.warning('附件向量化中，请稍候')
    return
  }
  const readyCount = pendingAttachments.value.filter((p) => p.status === 'ready').length
  const text = input.value.trim()
  // 允许「只发附件、不说话」
  if (!text && !readyCount) return
  if (readiness.value && readiness.value.llmConfigured === false) {
    ElMessage.warning('模型未配置：请设置 LLM_API_BASE_URL / LLM_API_KEY / LLM_MODEL')
    return
  }

  // 附件先入对话时间线，再发消息
  flushPendingAttachments()

  const history = messages.value
    .filter((m) => m.type !== 'attachment')
    .map((m) => ({ role: m.role, content: m.content }))
  const startedAt = Date.now()
  await ensureSession()
  const botMsg = reactive({
    role: 'assistant',
    content: '',
    statusText: '',
    statusShown: false,
    sources: [],
    error: false,
    streaming: true,
    stopped: false,
    traces: [],
    tracesOpen: false,
    startedAt,
    elapsedMs: 0,
  })
  if (text) {
    messages.value.push({ role: 'user', type: 'text', content: text, sources: [], traces: [], tracesOpen: false, elapsedMs: 0 })
  }
  messages.value.push(botMsg)
  input.value = ''
  sending.value = true
  const controller = new AbortController()
  stopController.value = controller
  const tickTimer = setInterval(() => {
    if (!botMsg.streaming) {
      clearInterval(tickTimer)
      return
    }
    botMsg.elapsedMs = Date.now() - startedAt
  }, 500)
  scrollToBottom()

  try {
    const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api$/, '')
    const resp = await fetch(`${base}/api/skills/chat/stream`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        skillId: selectedSkillIds.value[0] || selectedSkillId.value,
        skillIds: selectedSkillIds.value,
        message: text,
        history,
        sessionId: currentSessionId.value,
        model: modelName.value,
        attachments: sessionAttachments.value.map((a) => ({ id: a.id, filename: a.filename })),
      }),
      signal: controller.signal,
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
          botMsg.content += data
          scrollToBottom()
        } else if (eventName === 'status' && typeof data === 'string') {
          if (!botMsg.statusShown) {
            botMsg.statusText = data
            botMsg.statusShown = true
          }
          botMsg.traces.push({
            type: 'status',
            label: data,
            detail: '',
            elapsedMs: Date.now() - startedAt,
          })
          scrollToBottom()
        } else if (eventName === 'trace' && data && typeof data === 'object') {
          botMsg.traces.push({
            type: data.type || 'step',
            label: data.label || '',
            detail: data.detail || '',
            elapsedMs: data.elapsedMs ?? (Date.now() - startedAt),
          })
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
    botMsg.elapsedMs = Date.now() - startedAt
    clearInterval(tickTimer)
    loadSessions()
  } catch (e) {
    if (e?.name === 'AbortError') {
      botMsg.stopped = true
      botMsg.streaming = false
      botMsg.elapsedMs = Date.now() - startedAt
      if (!botMsg.content) botMsg.content = '（已停止）'
      else botMsg.content += '\n\n[已强制停止]'
      botMsg.traces.push({ type: 'status', label: '用户强制停止', detail: '', elapsedMs: botMsg.elapsedMs })
    } else {
      botMsg.error = true
      botMsg.content = `⚠️ ${e?.message || '对话失败'}`
      botMsg.streaming = false
      botMsg.elapsedMs = Date.now() - startedAt
    }
    clearInterval(tickTimer)
  } finally {
    sending.value = false
    stopController.value = null
    scrollToBottom()
  }
}

function fmtElapsed(ms) {
  if (ms == null || Number.isNaN(ms)) return '0s'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

function clearChat() {
  messages.value = []
}

function sourceLabel(src) {
  if (src.source_kind === 'session_attachment' || String(src.path || '').startsWith('sessions/')) {
    return src.filename || src.title || '会话附件'
  }
  if (src.source_kind === 'attachment' || String(src.path || '').startsWith('attachments/')) {
    return src.filename || src.locator || src.title || '附件'
  }
  // 知识条目：直接显示标题
  return src.title || src.locator || '知识条目'
}

function parseSourceRef(src) {
  const path = String(src.path || '')
  // 会话临时附件：无知识条目可打开
  if (src.source_kind === 'session_attachment' || path.startsWith('sessions/')) {
    return { kind: 'session_attachment' }
  }
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

/** 出处点击：附件 → 打开预览；条目 → 全屏详情弹窗 */
async function openItemPanel(src) {
  const ref = parseSourceRef(src)
  if (ref.kind === 'session_attachment') {
    ElMessage.info(`会话临时附件「${src.filename || src.title || '附件'}」：内容仅保存在本会话`)
    return
  }
  if (ref.kind === 'attachment' && ref.attachmentId) {
    emit('open-preview', { id: ref.attachmentId })
    return
  }
  if (ref.kind === 'item' && ref.itemId) {
    // 复用知识库详情弹窗（全屏），不再用侧边栏
    emit('open-item', ref.itemId)
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
  loadSessions().then(() => {
    // 默认打开上次会话；没有记录则选最近一个，进来就能看到内容
    const lastId = sessionStorage.getItem(LAST_SESSION_KEY)
    const hit = (lastId && sessions.value.find((s) => String(s.id) === String(lastId)))
      || sessions.value[0]
    if (hit) openSession(hit)
  })
  document.addEventListener('mousedown', onDocClickForSkillPicker)
  document.addEventListener('mousedown', onDocClickForSlash)
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
  document.removeEventListener('mousedown', onDocClickForSkillPicker)
  document.removeEventListener('mousedown', onDocClickForSlash)
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

/** 会话列表时间：更短、更有上下文感 */
function sessionTimeLabel(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const now = new Date()
  const hm = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  const sameDay = date.toDateString() === now.toDateString()
  if (sameDay) {
    const diffMin = Math.floor((now - date) / 60000)
    if (diffMin < 1) return '刚刚'
    if (diffMin < 60) return `${diffMin} 分钟前`
    return `今天 ${hm}`
  }
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return `昨天 ${hm}`
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${hm}`
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
  if (!markdown) return ''
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
            <el-button size="small" @click="openSkillManage">管理技能</el-button>
            <el-button size="small" @click="clearChat">清空对话</el-button>
          </div>
        </div>
      </template>

      <div class="skill-body" :class="{ 'has-detail': detailOpen }">
        <aside class="skill-list">
          <div class="skill-side-tabs">
            <span class="skill-side-title">历史会话</span>
            <el-tooltip content="新建会话" placement="right">
              <button type="button" class="skill-side-new" @click="createSession">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
              </button>
            </el-tooltip>
          </div>

          <div v-loading="loadingSessions" class="skill-list-inner">
            <button
              v-for="s in sessions"
              :key="s.id"
              type="button"
              class="skill-card skill-session-card"
              :class="{ active: s.id === currentSessionId }"
              @click="openSession(s)"
            >
              <div class="skill-session-title" :title="s.title">{{ s.title || '未命名会话' }}</div>
              <div class="skill-card-desc">
                <span class="skill-session-meta">{{ s.messageCount }} 条 · {{ sessionTimeLabel(s.updatedAt) }}</span>
                <button type="button" class="skill-session-del" title="删除会话" @click="deleteSession(s, $event)">×</button>
              </div>
            </button>
            <div v-if="!sessions.length && !loadingSessions" class="skill-empty">暂无历史会话</div>
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
              <div class="skill-msg-role sr-only">{{ msg.role === 'user' ? '我' : '助手' }}</div>
              <div class="skill-msg-bubble" :class="{ 'is-error': msg.error, 'is-attach': msg.type === 'attachment' }">
                <template v-if="msg.type === 'attachment'">
                  <button type="button" class="skill-att-card" title="点击预览" @click="openAttachmentPreview(msg)">
                    <span class="skill-att-card-icon">📎</span>
                    <span class="skill-att-card-body">
                      <span class="skill-att-card-name">{{ msg.filename }}</span>
                      <span class="skill-att-card-meta">{{ msg.chars ? msg.chars + ' 字' : '' }} · 点击预览</span>
                    </span>
                  </button>
                </template>
                <template v-else>
                <div v-if="msg.role !== 'user' && msg.traces" class="skill-process">
                  <button type="button" class="skill-process-toggle" @click="msg.tracesOpen = !msg.tracesOpen">
                    <span class="skill-process-arrow">{{ msg.tracesOpen ? '▾' : '▸' }}</span>
                    处理过程
                    <span class="skill-process-time">{{ fmtElapsed(msg.elapsedMs) }}</span>
                    <span class="skill-process-count">{{ (msg.traces || []).length }} 步</span>
                  </button>
                  <div v-if="msg.tracesOpen && msg.traces.length" class="skill-process-body">
                    <div v-for="(tr, ti) in msg.traces" :key="ti" class="skill-process-row" :class="`is-${tr.type}`">
                      <span class="skill-process-t">{{ fmtElapsed(tr.elapsedMs) }}</span>
                      <span class="skill-process-label">{{ tr.label }}</span>
                      <span v-if="tr.detail" class="skill-process-detail" :title="tr.detail">{{ tr.detail }}</span>
                    </div>
                  </div>
                </div>
                <div v-if="msg.statusText" class="skill-msg-status">{{ msg.statusText }}</div>
                <div class="skill-msg-text markdown-body" v-html="renderMarkdown(msg.content || '') + (msg.streaming ? '<span class=\'skill-cursor\'>▍</span>' : '')"></div>
                <div v-if="msg.sources && msg.sources.length" class="skill-sources">
                  <div class="skill-sources-title">出处</div>
                  <div class="skill-sources-row">
                    <button
                      v-for="(src, si) in msg.sources"
                      :key="si"
                      type="button"
                      class="skill-source-item skill-source-link"
                      @click="openItemPanel(src)"
                    >
                      <svg class="skill-source-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                      <span class="skill-source-label">{{ sourceLabel(src) }}</span>
                    </button>
                  </div>
                </div>
                </template>
              </div>
            </div>

            <div v-if="(sending || waitingAnswer) && !messages.some((m) => m.streaming)" class="skill-msg is-bot">
              <div class="skill-msg-role sr-only">助手</div>
              <div class="skill-msg-bubble skill-msg-thinking">思考中…</div>
            </div>
          </div>

          <div class="skill-composer">
            <div
              class="skill-input-resize"
              title="拖拽调整输入框高度"
              @mousedown="startInputDrag"
            ></div>

            <!-- 技能悬浮面板：输入框上方，单选 -->
            <div ref="skillPickerRef" class="skill-float-wrap">
              <transition name="skill-panel-fade">
                <div v-if="skillPickerOpen" class="skill-picker-panel">
                  <div class="skill-picker-grid">
                    <button
                      v-for="skill in skills"
                      :key="skill.id"
                      type="button"
                      class="skill-picker-card"
                      :class="{ active: selectedSkillId === skill.id }"
                      @click="toggleSkill(skill.id)"
                    >
                      <div class="skill-picker-name">
                        <span v-if="selectedSkillId === skill.id" class="skill-picker-check">✓</span>
                        {{ skill.name }}
                      </div>
                      <div v-if="skill.description" class="skill-picker-desc">{{ skill.description }}</div>
                    </button>
                    <div v-if="!skills.length" class="skill-picker-empty">暂无可用技能</div>
                  </div>
                </div>
              </transition>
            </div>

            <!-- 待发送附件：向量化进度，完成后点发送才进对话 -->
            <div v-if="pendingAttachments.length" class="skill-pending-atts">
              <div v-for="p in pendingAttachments" :key="p.key" class="skill-pending-item">
                <div class="skill-pending-name" :title="p.filename">📎 {{ p.filename }}</div>
                <div class="skill-pending-msg">{{ p.message }}</div>
                <div class="skill-pending-bar">
                  <div
                    class="skill-pending-fill"
                    :class="`is-${p.status}`"
                    :style="{ width: `${p.progress}%` }"
                  ></div>
                </div>
                <button
                  type="button"
                  class="skill-pending-del"
                  title="移除"
                  @click="removePendingAttachment(p)"
                >×</button>
              </div>
            </div>

            <el-input
              v-model="input"
              type="textarea"
              :rows="3"
              placeholder="输入问题，Enter 发送 / Ctrl+Enter 换行；输入 / 选择技能"
              :disabled="sending"
              class="skill-textarea"
              :style="{ '--skill-th': textareaHeight + 'px' }"
              @keydown="onComposerKeydown"
              @input="onComposerInput"
              @keyup="syncCaretScroll"
              @click="syncCaretScroll"
            />

            <!-- 斜杠命令菜单：只列技能 -->
            <div ref="slashMenuRef" class="skill-float-wrap skill-slash-wrap">
              <transition name="skill-panel-fade">
                <div v-if="slashMenuOpen" class="skill-picker-panel skill-slash-panel">
                  <div class="skill-picker-head">
                    <span>{{ slashQuery ? `匹配「${slashQuery}」` : '选择技能' }}</span>
                  </div>
                  <div class="skill-slash-list">
                    <button
                      v-for="(cmd, ci) in filteredSlashCommands"
                      :key="cmd.key"
                      type="button"
                      class="skill-slash-item"
                      :class="{ active: ci === slashActive }"
                      @mouseenter="slashActive = ci"
                      @mousedown.prevent="runSlashCommand(cmd)"
                    >
                      <span class="skill-slash-cmd">{{ cmd.label }}</span>
                      <span class="skill-slash-desc">{{ cmd.desc }}</span>
                    </button>
                    <div v-if="!filteredSlashCommands.length" class="skill-picker-empty">无匹配命令</div>
                  </div>
                </div>
              </transition>
            </div>

            <div class="skill-toolbar">
              <div class="skill-toolbar-left">
                <button type="button" class="skill-tool-btn skill-clip-btn" title="添加临时附件（仅本会话）" :disabled="uploading" @click="pickFile">
                  <svg v-if="!uploading" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
                  </svg>
                  <span v-else>…</span>
                </button>
                <input ref="fileInputRef" type="file" style="display:none" accept=".md,.txt,.docx,.pdf,.xlsx,.xls,.pptx,.html,.htm,.png,.jpg,.jpeg" @change="onFileChange" />
                <button
                  ref="skillToggleRef"
                  type="button"
                  class="skill-tool-btn skill-skill-btn"
                  :class="{ active: skillPickerOpen || !!selectedSkillId }"
                  title="选择技能"
                  @click="toggleSkillPicker"
                >
                  {{ selectedSkill ? selectedSkill.name : '技能' }}
                  <span
                    v-if="selectedSkillId"
                    class="skill-skill-clear"
                    title="取消技能"
                    role="button"
                    @click.stop="clearSkills"
                  >×</span>
                </button>
                <span class="skill-tool-hint"></span>
              </div>
              <div class="skill-toolbar-right">
                <span
                  class="skill-ctx-ring"
                  :title="`上下文约 ${contextTokens.toLocaleString()} / ${CONTEXT_TOKEN_LIMIT.toLocaleString()} tokens`"
                >
                  <svg width="22" height="22" viewBox="0 0 22 22">
                    <circle cx="11" cy="11" r="8.5" fill="none" stroke="var(--el-fill-color)" stroke-width="3" />
                    <circle
                      cx="11" cy="11" r="8.5" fill="none"
                      :stroke="contextRingColor"
                      stroke-width="3"
                      stroke-linecap="round"
                      :stroke-dasharray="`${(contextPct / 100) * 53.4} 53.4`"
                      transform="rotate(-90 11 11)"
                    />
                  </svg>
                </span>

                <el-dropdown trigger="click" @command="setModel">
                  <button type="button" class="skill-tool-btn skill-model-btn">
                    {{ modelName }} ▾
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="m in modelNameOptions" :key="m" :command="m">{{ m }}</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>

                <button
                  type="button"
                  class="skill-tool-btn skill-mic-btn"
                  :class="{ active: listening }"
                  :title="listening ? '停止录音' : '语音输入'"
                  @click="toggleVoice"
                >
                  🎤
                </button>

                <button
                  type="button"
                  class="skill-send-btn"
                  :disabled="!sending && (!input.trim() && !pendingAttachments.some((p) => p.status === 'ready')) || (!sending && pendingAttachments.some((p) => p.status === 'processing'))"
                  :class="{ loading: sending, 'is-stop': sending }"
                  :title="sending ? '停止生成' : '发送'"
                  @click="sending ? stopStream() : send()"
                >
                  <svg v-if="!sending" width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path
                      d="M12 5.5v11"
                      stroke="currentColor"
                      stroke-width="3"
                      stroke-linecap="round"
                    />
                    <path
                      d="M7.5 10L12 5.5 16.5 10"
                      stroke="currentColor"
                      stroke-width="3"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                  <span v-else class="skill-stop-icon"></span>
                </button>
              </div>
            </div>
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
    <el-dialog v-model="skillManageOpen" title="管理技能" width="920px" destroy-on-close class="skill-manage-dialog">
      <div class="skill-manage-toolbar">
        <el-radio-group v-model="skillManageTab" size="small">
          <el-radio-button value="public">公共技能</el-radio-button>
          <el-radio-button value="personal">个人技能</el-radio-button>
        </el-radio-group>
        <div class="skill-manage-actions">
          <el-button type="primary" size="small" @click="openSkillCreate">
            {{ skillManageTab === 'personal' ? '新建个人技能' : '新建公共技能' }}
          </el-button>
          <el-button size="small" @click="loadAllSkills">刷新</el-button>
        </div>
      </div>
      <el-table :data="tabSkills" v-loading="skillManageLoading" size="small" stripe class="skill-manage-table">
        <el-table-column prop="id" label="编码" width="130" show-overflow-tooltip />
        <el-table-column prop="name" label="名称" width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="归属" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.ownerUsername" size="small" type="warning">{{ row.ownerUsername }}</el-tag>
            <el-tag v-else size="small" type="info">公共</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="工具" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="skill-tools-cell">{{ (row.tools || []).join('、') || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="64" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openSkillEdit(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="deleteSkill(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="skill-manage-empty">
            {{ skillManageTab === 'personal' ? '暂无个人技能，点右上角新建，或从公共技能复制' : '暂无公共技能' }}
          </div>
        </template>
      </el-table>
    </el-dialog>

    <el-dialog v-model="skillFormOpen" :title="skillForm.dbId ? '编辑技能' : '新增技能'" width="760px" destroy-on-close class="skill-form-dialog">
      <el-form label-width="100px" label-position="left" class="skill-form">
        <template v-if="!skillForm.dbId && skillManageTab === 'personal'">
          <el-form-item label="复制模板">
            <el-select
              v-model="skillCopyFrom"
              clearable
              filterable
              placeholder="可从公共技能复制后修改"
              style="width: 100%"
              @change="applyCopyFrom"
            >
              <el-option
                v-for="s in publicSkills"
                :key="s.dbId || s.id"
                :label="s.name"
                :value="s.dbId || s.id"
              >
                <span>{{ s.name }}</span>
                <span class="skill-tool-opt-desc">{{ s.id }}</span>
              </el-option>
            </el-select>
            <div class="skill-form-hint">选择后自动带出提示词/工具/绑定，编码需保持唯一</div>
          </el-form-item>
        </template>

        <div class="skill-form-row">
          <el-form-item label="技能编码" required class="skill-form-col">
            <el-input v-model="skillForm.id" placeholder="如 product-qa" :disabled="!!skillForm.dbId" />
          </el-form-item>
          <el-form-item label="名称" required class="skill-form-col">
            <el-input v-model="skillForm.name" placeholder="如 产品知识问答" />
          </el-form-item>
        </div>

        <el-form-item label="描述">
          <el-input v-model="skillForm.description" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="一句话说明用途" />
        </el-form-item>

        <el-form-item label="归属">
          <el-radio-group :model-value="skillForm.ownerUsername ? 'personal' : 'public'" @update:model-value="(v) => { skillForm.ownerUsername = v === 'personal' ? (currentUsername() || 'me') : '' }" :disabled="!!skillForm.dbId">
            <el-radio value="public">公共技能</el-radio>
            <el-radio value="personal">个人技能（{{ currentUsername() || '当前账号' }}）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="系统提示词" required>
          <el-input v-model="skillForm.systemPrompt" type="textarea" :rows="8" placeholder="技能行为说明，会作为 system 角色发给模型" />
        </el-form-item>

        <el-form-item label="工具">
          <el-select
            v-model="skillToolList"
            multiple
            clearable
            placeholder="选择技能可用的工具，可不选"
            style="width: 100%"
          >
            <el-option
              v-for="t in skillToolOptions"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            >
              <span>{{ t.label }}</span>
              <span class="skill-tool-opt-desc">{{ t.desc }}</span>
            </el-option>
          </el-select>
          <div class="skill-form-hint">留空 = 不调用任何工具（纯按提示词回答）</div>
        </el-form-item>
        <el-form-item label="绑定分类">
          <el-select
            v-model="skillForm.scopeCategoryIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="留空 = 全库检索；选中后仅在这些分类树下检索"
            style="width: 100%"
          >
            <el-option
              v-for="c in skillCategoryOptions"
              :key="c.id"
              :label="c.label"
              :value="String(c.id)"
            />
          </el-select>
          <div class="skill-form-hint">支持分类树：选「手术麻醉」会包含其子分类</div>
        </el-form-item>
        <el-form-item label="绑定条目">
          <el-select
            v-model="skillForm.scopeItemIds"
            multiple
            filterable
            remote
            clearable
            collapse-tags
            collapse-tags-tooltip
            :remote-method="searchSkillItems"
            :loading="skillItemLoading"
            placeholder="留空 = 不限；可搜索知识条目"
            style="width: 100%"
          >
            <el-option
              v-for="it in skillItemOptions"
              :key="it.id"
              :label="it.title"
              :value="String(it.id)"
            />
          </el-select>
          <div class="skill-form-hint">与分类是「或」关系：命中分类树或指定条目任一即可</div>
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
.skill-side-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
  align-items: center;
}
.skill-side-title {
  flex: 1;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}
.skill-side-new {
  width: 28px;
  height: 28px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  border-radius: 8px;
  cursor: pointer;
  color: var(--el-text-color-regular);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.skill-side-new:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.skill-session-title {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.skill-session-card .skill-card-desc {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.skill-session-meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
.skill-session-del {
  border: none;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  font-size: 14px;
  padding: 0 4px;
}
.skill-session-del:hover {
  color: var(--el-color-danger);
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
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
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
.skill-msg-bubble.is-attach {
  padding: 6px;
  background: transparent;
  white-space: normal;
}
.skill-att-card {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  text-align: left;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: border-color 0.15s ease;
}
.skill-att-card:hover {
  border-color: var(--el-color-primary);
}
.skill-att-card-icon {
  font-size: 18px;
}
.skill-att-card-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.skill-att-card-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.skill-att-card-meta {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.skill-form-hint {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  margin-top: 2px;
}
.skill-manage-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.skill-manage-actions {
  display: flex;
  gap: 8px;
}
.skill-manage-table {
  width: 100%;
}
.skill-manage-empty {
  padding: 24px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.skill-tools-cell {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.skill-form-row {
  display: flex;
  gap: 16px;
}
.skill-form-col {
  flex: 1;
  min-width: 0;
}
.skill-form .el-form-item {
  margin-bottom: 14px;
}
.skill-tool-opt-desc {
  margin-left: 10px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
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
.skill-process {
  margin-bottom: 8px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
}
.skill-process-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: none;
  background: var(--el-fill-color-light);
  cursor: pointer;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.skill-process-toggle:hover {
  color: var(--el-color-primary);
}
.skill-process-time {
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}
.skill-process-count {
  opacity: 0.8;
}
.skill-process-body {
  max-height: 180px;
  overflow: auto;
  padding: 6px 10px 8px;
  background: var(--el-fill-color-blank);
}
.skill-process-row {
  display: flex;
  gap: 8px;
  font-size: 11px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}
.skill-process-t {
  flex: 0 0 48px;
  font-variant-numeric: tabular-nums;
  opacity: 0.85;
}
.skill-process-label {
  flex: 0 0 auto;
  color: var(--el-text-color-regular);
}
.skill-process-row.is-tool .skill-process-label {
  color: var(--el-color-warning);
}
.skill-process-row.is-tool_result .skill-process-label {
  color: var(--el-color-success);
}
.skill-process-row.is-error .skill-process-label {
  color: var(--el-color-danger);
}
.skill-process-detail {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: 0.75;
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
  margin-bottom: 6px;
}
.skill-sources-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
}
.skill-source-item {
  font-size: 12px;
}
.skill-source-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  text-align: left;
  padding: 2px 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--el-color-primary);
}
.skill-source-icon {
  flex-shrink: 0;
  opacity: 0.75;
}
.skill-source-label {
  text-decoration: underline;
  text-underline-offset: 3px;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.skill-source-link:hover {
  color: var(--el-color-primary-light-3);
}
.skill-source-link:hover .skill-source-icon {
  opacity: 1;
}
.skill-composer {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 8px;
  position: relative;
}
.skill-input-resize {
  position: absolute;
  top: -4px;
  left: 0;
  right: 0;
  height: 8px;
  cursor: ns-resize;
  z-index: 2;
  background: transparent;
  border: none;
  outline: none;
}
.skill-input-resize:hover {
  background: transparent;
}
.skill-att-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}
.skill-att-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  background: var(--el-fill-color);
  border: 1px solid var(--el-border-color-lighter);
}
.skill-att-chip button {
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--el-text-color-secondary);
}
.skill-textarea :deep(.el-textarea__inner) {
  height: var(--skill-th, 88px) !important;
  min-height: 72px;
  max-height: 50vh;
  resize: none;
  border-radius: 12px;
  overflow-y: auto;
  overflow-x: hidden;
  box-shadow: none;
}
.skill-textarea :deep(.el-textarea__inner:focus),
.skill-textarea :deep(.el-textarea__inner:hover) {
  box-shadow: none;
  outline: none;
}
.skill-msg-thinking {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.skill-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}
.skill-toolbar-left,
.skill-toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.skill-tool-btn {
  min-width: 32px;
  height: 32px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  border-radius: 16px;
  cursor: pointer;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.skill-tool-btn:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.skill-tool-btn.active {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
}
.skill-float-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.skill-float-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.skill-float-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.skill-chip {
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-regular);
  border-radius: 14px;
  padding: 3px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.skill-chip:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}
.skill-chip.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  color: #fff;
  font-weight: 600;
}
.skill-float-wrap {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(100% + 6px);
  z-index: 30;
  pointer-events: none;
}
.skill-picker-panel {
  pointer-events: auto;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  padding: 12px 14px;
  margin-bottom: 4px;
}
.skill-picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 10px;
}
.skill-picker-head-compact {
  justify-content: flex-end;
  margin-bottom: 6px;
}
.skill-picker-clear {
  border: none;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 12px;
}
.skill-picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
}
.skill-picker-card {
  text-align: left;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.skill-picker-card:hover {
  border-color: var(--el-color-primary-light-5);
}
.skill-picker-card.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.skill-picker-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.skill-picker-check {
  color: var(--el-color-primary);
}
.skill-picker-desc {
  margin-top: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.skill-picker-empty {
  grid-column: 1 / -1;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  padding: 12px;
}
.skill-slash-wrap {
  bottom: calc(100% + 6px);
}
.skill-slash-panel {
  max-height: 280px;
  overflow: auto;
}
.skill-pending-atts {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.skill-pending-item {
  position: relative;
  width: fit-content;
  max-width: 220px;
  min-width: 120px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  padding: 8px 22px 8px 10px;
  background: var(--el-fill-color-blank);
}
.skill-pending-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.skill-pending-msg {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.skill-pending-del {
  position: absolute;
  top: 4px;
  right: 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1;
  padding: 0 2px;
}
.skill-pending-del:hover {
  color: var(--el-color-danger);
}
.skill-pending-bar {
  height: 4px;
  border-radius: 2px;
  background: var(--el-fill-color);
  overflow: hidden;
  margin-top: 6px;
}
.skill-pending-fill {
  height: 100%;
  border-radius: 2px;
  background: var(--el-color-primary);
  transition: width 0.25s ease;
}
.skill-pending-fill.is-ready {
  background: var(--el-color-success);
}
.skill-pending-fill.is-error {
  background: var(--el-color-danger);
}
.skill-slash-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.skill-slash-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
  border: none;
  background: transparent;
  border-radius: 8px;
  padding: 8px 10px;
  cursor: pointer;
}
.skill-slash-item:hover,
.skill-slash-item.active {
  background: var(--el-color-primary-light-9);
}
.skill-slash-cmd {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  flex-shrink: 0;
}
.skill-slash-desc {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.skill-send-btn.is-stop {
  background: var(--el-color-danger);
}
.skill-panel-fade-enter-active,
.skill-panel-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.skill-panel-fade-enter-from,
.skill-panel-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
.skill-selected-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}
.skill-selected-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
  color: var(--el-color-primary);
}
.skill-selected-chip button {
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--el-color-primary);
  font-size: 13px;
  line-height: 1;
}
.skill-skill-btn {
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.skill-skill-clear {
  font-size: 14px;
  line-height: 1;
  opacity: 0.75;
  margin-left: 2px;
}
.skill-skill-clear:hover {
  opacity: 1;
}
.skill-tool-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.skill-clip-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.skill-ctx-ring {
  display: inline-flex;
  align-items: center;
  line-height: 0;
}
.skill-tool-ctx {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.skill-model-btn {
  padding: 0 12px;
}
.skill-send-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.skill-send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.skill-send-btn.loading {
  background: var(--el-color-danger);
}
.skill-stop-icon {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  background: #fff;
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
.skill-detail-att-icon.is-pdf { background: var(--bg-soft); color: var(--sem-danger); }
.skill-detail-att-icon.is-docx,
.skill-detail-att-icon.is-doc { background: var(--bg-hover); color: var(--t-muted); }
.skill-detail-att-icon.is-xlsx,
.skill-detail-att-icon.is-xls,
.skill-detail-att-icon.is-xlsm { background: var(--bg-soft); color: var(--sem-success); }
.skill-detail-att-icon.is-pptx,
.skill-detail-att-icon.is-ppt { background: var(--bg-hover); color: var(--sem-warning); }
.skill-detail-att-icon.is-png,
.skill-detail-att-icon.is-jpg,
.skill-detail-att-icon.is-jpeg,
.skill-detail-att-icon.is-gif { background: #e7ddff; color: #7c4dff; }
.skill-detail-att-icon.is-zip,
.skill-detail-att-icon.is-rar,
.skill-detail-att-icon.is-7z { background: var(--bg-soft); color: var(--sem-warning); }
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
