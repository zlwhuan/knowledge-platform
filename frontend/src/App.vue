<script setup>
import { computed, defineAsyncComponent, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'

const LoginView = defineAsyncComponent(() => import('./components/LoginView.vue'))
const AppSidebar = defineAsyncComponent(() => import('./components/AppSidebar.vue'))
const AppMainContent = defineAsyncComponent(() => import('./components/AppMainContent.vue'))
const ProjectGanttView = defineAsyncComponent(() => import('./components/ProjectGanttView.vue'))
const LibraryDetailDialog = defineAsyncComponent(() => import('./components/LibraryDetailDialog.vue'))
const LibraryBatchEditDialog = defineAsyncComponent(() => import('./components/LibraryBatchEditDialog.vue'))
const AttachmentManagement = defineAsyncComponent(() => import('./components/AttachmentManagement.vue'))
import { api, attachAuthGuard, attachAuthToken } from './services/api'
import { useProjectTracker } from './composables/useProjectTracker'
import { useKnowledgeLibrary } from './composables/useKnowledgeLibrary'
import { useSystemManagement } from './composables/useSystemManagement'
import { useHomeDashboard } from './composables/useHomeDashboard'
import { useAttachmentPreview } from './composables/useAttachmentPreview'
import { useShellNavigation } from './composables/useShellNavigation'
import { useLibraryActions } from './composables/useLibraryActions'
import { useCustomerManagement } from './composables/useCustomerManagement'
import { useDictionaries } from './composables/useDictionaries'

const storageKey = 'knowledge-platform-auth'
const libraryTreeStateKey = 'knowledge-platform-library-tree'
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api$/, '/api')

const auth = reactive({ token: '', user: null })
const loginForm = reactive({ username: 'admin', password: 'Admin@123' })
const loginError = ref('')
const currentView = ref('home')
const libraryMenuOpen = ref(false)
const projectMenuOpen = ref(false)
const systemMenuOpen = ref(false)
const trainingMenuOpen = ref(false)
const libraryQuickFilter = ref('all')
const selectedItemIds = ref([])
const expandedNodes = ref(new Set())
const globalSearchResults = ref(null)
const globalSearchLoading = ref(false)
const globalSearchDialogOpen = ref(false)
const importInputRef = ref(null)

function triggerImport() {
  importInputRef.value?.click()
}

async function handleImport(event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const { data } = await api.post('/items/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const result = data?.data || {}
    ElMessage({ message: `导入完成：成功 ${result.success} 条，失败 ${result.failed} 条`, type: result.failed ? 'warning' : 'success', showClose: true, duration: 4000 })
    if (result.errors?.length) {
      console.warn('导入错误详情:', result.errors)
    }
    loadItems()
  } catch (error) {
    ElMessage({ message: '导入失败: ' + (error?.response?.data?.message || error.message), type: 'error', showClose: true })
  }
  event.target.value = ''
}

async function handleGlobalSearch(query) {
  if (!query || query.trim().length < 2) {
    globalSearchResults.value = null
    globalSearchDialogOpen.value = false
    return
  }
  globalSearchLoading.value = true
  globalSearchDialogOpen.value = true
  try {
    const { data } = await api.get('/search', { params: { q: query.trim() } })
    globalSearchResults.value = data?.data || null
  } catch {
    globalSearchResults.value = null
  } finally {
    globalSearchLoading.value = false
  }
}

const confirmDialog = reactive({
  open: false,
  title: '删除确认',
  message: '',
  danger: true,
  resolver: null,
})

const projectNavItems = [
  { key: 'project-tracker', label: '管理总览', desc: '全局项目盘点与关键指标' },
  { key: 'project-sales', label: '销售', desc: '商机立项与合同执行' },
  { key: 'project-presales', label: '售前', desc: '方案支撑与投标协同' },
  { key: 'project-delivery-ops', label: '实施运维', desc: '实施交付、验收结算、售后维保' },
  { key: 'project-finance', label: '财务', desc: '合同金额、回款与结算跟踪' },
  { key: 'project-gantt', label: '项目甘特', desc: '项目时间线可视化' },
]

const systemNavItems = [
  { key: 'settings', label: '分类配置', desc: '维护分类层级与排序' },
  { key: 'dictionary-settings', label: '字典维护', desc: '统一维护业务下拉选项' },
  { key: 'users', label: '用户管理', desc: '维护账号、角色与启停状态' },
  { key: 'roles', label: '角色管理', desc: '角色视图与功能配置' },
  { key: 'preview-settings', label: '预览配置', desc: '查看附件与文档预览策略' },
  { key: 'system-overview', label: '系统信息', desc: '查看服务与数据概览' },
  { key: 'logout', label: '退出登录', desc: '清除当前登录状态' },
]

// NOTE: This fallback is used only during bootstrap before roleConfigs load from the API.
// The backend is authoritative — once roleConfigs resolves, it takes precedence.
const currentRolePermissions = computed(() => {
  const fallback = {
    canViewLibrary: true,
    canCreateContent: auth.user?.role !== 'FINANCE',
    canEditContent: ['ADMIN', 'SALES', 'PRESALES', 'DELIVERY_OPS'].includes(auth.user?.role),
    canDeleteContent: ['ADMIN', 'SALES', 'PRESALES', 'DELIVERY_OPS'].includes(auth.user?.role),
    canManageCategories: auth.user?.role === 'ADMIN',
    canManageUsers: auth.user?.role === 'ADMIN',
    canManageRoles: auth.user?.role === 'ADMIN',
    canPreviewOffice: true,
  }
  return roleConfigs.value.find((item) => item.role === auth.user?.role) || fallback
})

const {
  trackerSubView, trackerSubMenus, projectsLoading, dashboardLoading, projectFormSaving, projectProgressSaving,
  projectFormDialogOpen, projectProgressDialogOpen, projects, projectDashboard, selectedProject,
  recentProgressRecords, projectFilters, projectForm, projectProgressForm, dashboardCards, viewProjects,
  upcomingActionItems, subViewTitle, subViewDescription, formatMoney, subViewHighlights, loadProjectDashboard, loadProjects, selectProject,
  switchTrackerSubView, openProjectEditDialog, openProjectProgressDialog,
  closeProjectFormDialog, closeProjectProgressDialog,
  saveProject, deleteProject, saveProjectProgress, deleteProjectProgress, resetProjectFilters,
} = useProjectTracker({
  confirmDelete: askDeleteConfirm,
  getCurrentUserName: () => auth.user?.displayName || auth.user?.username || '',
})

const {
  libraryLoading, categoriesLoading, saving: librarySaving, detailDialogOpen, selectedItem, selectedCategoryId, libraryFilters,
  categories, items, flatCategories, form, uploadQueue, batchEditDialog, loadCategories, loadItems, openAllLibrary, selectCategory,
  resetFilters, startCreateContent: startLibraryCreateContent, fillForm: fillLibraryForm, markAttachmentForDeletion, openDetail: openLibraryDetail,
  saveItem, deleteItem, resetBatchEditDialog, openBatchEditDialog, applyBatchEdit, handleFileChange, removeQueuedFile, bulkDelete,
  libraryPage, libraryPageSize, libraryTotal, libraryPages, changePage,
} = useKnowledgeLibrary({ confirmDelete: askDeleteConfirm })

const {
  usersLoading, categoriesSaving, usersSaving, rolesSaving, rolePermissionsLoading,
  users, roleConfigs, roleCards, categoryDraft, userDraft,
  loadUsers, loadRolePermissions, saveUser, removeUser, editUser, resetUserDraft,
  saveCategory, removeCategory, editCategory, createChildCategory, resetCategoryDraft, saveRoleConfig,
} = useSystemManagement({ confirmDelete: askDeleteConfirm })

attachAuthToken(() => auth.token)
attachAuthGuard(handleAuthExpired)

const flatCategoryOptions = computed(() => flatCategories.value.map((item) => ({
  id: String(item.id),
  parentId: item.parentId == null ? null : String(item.parentId),
  name: item.name,
  level: item.depth,
})))
const systemRoles = computed(() => roleCards.value.map((item) => ({ value: item.value, label: item.label })))
const visibleSystemNavItems = computed(() => systemNavItems.filter((item) => {
  if (item.key === 'users') return currentRolePermissions.value.canManageUsers
  if (item.key === 'roles') return currentRolePermissions.value.canManageRoles
  return true
}))

const {
  dictionariesLoading,
  dictionariesSaving,
  dictionaryGroups,
  dictionaryDraft,
  getOptions,
  loadDictionaries,
  resetDictionaryDraft,
  editDictionaryItem,
  saveDictionaryItem,
  removeDictionaryItem,
} = useDictionaries()

const {
  loading: customersLoading,
  customerSaving,
  contactSaving,
  followupSaving,
  customerDialogOpen,
  contactDialogOpen,
  followupDialogOpen,
  customers,
  selectedCustomer,
  contacts,
  followups,
  customerFilters,
  customerForm,
  contactForm,
  followupForm,
  loadCustomers,
  selectCustomer,
  openCustomerDialog,
  openContactDialog,
  openFollowupDialog,
  closeCustomerDialog,
  closeContactDialog,
  closeFollowupDialog,
  saveCustomer,
  deleteCustomer,
  saveContact,
  deleteContact,
  saveFollowup,
  deleteFollowup,
  resetCustomerFilters,
} = useCustomerManagement({ confirmDelete: askDeleteConfirm })
const systemSettings = computed(() => ({
  appName: '知识平台 CMS',
  officeCommand: '',
  onlyOfficeEnabled: false,
  serverPort: '8080',
  itemCount: items.value.length,
  categoryCount: flatCategories.value.length,
  userCount: users.value.length,
}))
const dashboardStats = computed(() => ({
  categoryCount: flatCategories.value.length,
  itemCount: items.value.length,
  attachmentCount: items.value.reduce((sum, item) => sum + (item.attachments?.length || 0), 0),
  recentUpdatedCount: items.value.filter((item) => Date.now() - new Date(item.updatedAt || item.createdAt || 0).getTime() <= 7 * 24 * 60 * 60 * 1000).length,
  missingSummaryCount: items.value.filter((item) => !String(item.summary || '').trim()).length,
  missingAttachmentCount: items.value.filter((item) => !(item.attachments?.length)).length,
}))

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

function isPreviewFrameKind(kind) {
  return ['office-word', 'office-sheet', 'office-slide', 'markdown', 'text', 'pdf'].includes(kind)
}

const filteredItems = computed(() => {
  const now = Date.now()
  const sevenDaysMs = 7 * 24 * 60 * 60 * 1000
  if (libraryQuickFilter.value === 'recent') {
    return items.value.filter((item) => now - new Date(item.updatedAt || item.createdAt || 0).getTime() <= sevenDaysMs)
  }
  return items.value
})
const selectedLibraryItems = computed(() => items.value.filter((item) => selectedItemIds.value.includes(item.id)))
const allDisplayedSelected = computed(() => filteredItems.value.length > 0 && filteredItems.value.every((item) => selectedItemIds.value.includes(item.id)))
const libraryQuickFilterOptions = computed(() => [
  { key: 'all', label: '全部资料', count: items.value.length },
  { key: 'recent', label: '最近更新', count: items.value.filter((item) => Date.now() - new Date(item.updatedAt || item.createdAt || 0).getTime() <= 7 * 24 * 60 * 60 * 1000).length },
])

const categoryChildrenMap = computed(() => {
  const map = new Map()
  flatCategories.value.forEach((item) => {
    const parentKey = item.parentId == null ? 'root' : String(item.parentId)
    if (!map.has(parentKey)) map.set(parentKey, [])
    map.get(parentKey).push(item)
  })
  return map
})

const {
  currentCategoryName,
  projectTrackerSummary,
  projectMilestones,
  projectRecordFeed,
  homeProjectFocus,
  homeActionQueue,
  homeQuickStats,
  relatedLibraryItems,
  customerFollowupAlerts,
} = useHomeDashboard({
  items,
  libraryFilters,
  flatCategoryOptions,
  projectDashboard,
  recentProgressRecords,
  viewProjects,
  selectedProject,
  customers,
  formatMoney,
  formatDateTime,
})

function renderMarkdown(markdown) {
  if (!markdown) return '<p>暂无内容</p>'
  const lines = String(markdown).split(/\r?\n/)
  const blocks = []
  let paragraph = []
  let list = []
  let code = []
  let inCode = false
  const inline = (text) => text.replace(/\`([^\`]+)\`/g, '<code>$1</code>').replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>').replace(/\*([^*]+)\*/g, '<em>$1</em>')
  const flushParagraph = () => { if (paragraph.length) { blocks.push('<p>' + inline(paragraph.join('<br />')) + '</p>'); paragraph = [] } }
  const flushList = () => { if (list.length) { blocks.push('<ul>' + list.map((item) => '<li>' + inline(item) + '</li>').join('') + '</ul>'); list = [] } }
  const flushCode = () => { if (code.length) { blocks.push('<pre><code>' + code.join('\n') + '</code></pre>'); code = [] } }
  for (const line of lines) {
    if (line.startsWith('```')) { flushParagraph(); flushList(); if (inCode) flushCode(); inCode = !inCode; continue }
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

function showToast(message, type = 'success') { ElMessage({ message, type, showClose: true, duration: 2200 }) }

function closeDeleteConfirm(result = false) {
  const resolver = confirmDialog.resolver
  confirmDialog.open = false
  confirmDialog.resolver = null
  if (resolver) resolver(result)
}

function askDeleteConfirm(message, title = '删除确认') {
  confirmDialog.title = title
  confirmDialog.message = message
  confirmDialog.danger = true
  confirmDialog.open = true
  return new Promise((resolve) => {
    confirmDialog.resolver = resolve
  })
}

const {
  previewLoading,
  preview,
  onlyOfficeMountRef,
  previewEngineLabel,
  previewHint,
  previewFrameUrl,
  canUseOnlyOfficeComponent,
  openPreview,
  reloadPreview,
  destroyOnlyOfficeEditor,
} = useAttachmentPreview({
  api,
  apiBaseUrl,
  showToast,
  formatDateTime,
  formatFileSize,
})

const {
  closeAllSubmenus,
  setCurrentView,
  openProjectView,
  toggleLibraryMenu,
  openLibraryHome,
  toggleProjectMenu,
  toggleSystemMenu,
  toggleTrainingMenu,
  openSystemView: openSystemViewByNavigation,
} = useShellNavigation({
  currentView,
  libraryMenuOpen,
  projectMenuOpen,
  systemMenuOpen,
  trainingMenuOpen,
  openAllLibrary,
  switchTrackerSubView,
})

function handleHomeOpenProjectView(payload) {
  const view = typeof payload === 'string' ? payload : (payload?.view || 'project-tracker')
  openProjectView(view)
}

async function handleHomeOpenProjectProgress(payload) {
  openProjectView('project-tracker')
  const projectId = payload?.projectId
  if (!projectId) return
  await loadProjects(projectId)
  const row = projects.value.find((item) => item.id === projectId)
  if (!row) return
  selectProject(row)
  await nextTick()
  openProjectProgressDialog(row)
}

function updateLibraryQuickFilter(value) { libraryQuickFilter.value = value; setCurrentView('library') }
function toggleSelectItem(id) { selectedItemIds.value = selectedItemIds.value.includes(id) ? selectedItemIds.value.filter((itemId) => itemId !== id) : [...selectedItemIds.value, id] }
function toggleSelectAllDisplayed() {
  if (allDisplayedSelected.value) { selectedItemIds.value = selectedItemIds.value.filter((id) => !filteredItems.value.some((item) => item.id === id)); return }
  selectedItemIds.value = [...new Set([...selectedItemIds.value, ...filteredItems.value.map((item) => item.id)])]
}
function clearSelectedItems() { selectedItemIds.value = [] }
function resetLibraryFilters() { resetFilters(); libraryQuickFilter.value = 'all'; selectedItemIds.value = [] }

function normalizeCategoryId(categoryId) {
  return categoryId == null ? null : String(categoryId)
}

function hasChildren(categoryId) {
  return (categoryChildrenMap.value.get(normalizeCategoryId(categoryId)) || []).length > 0
}

function persistExpandedNodes(next) {
  const normalized = new Set([...next].map((item) => normalizeCategoryId(item)).filter(Boolean))
  expandedNodes.value = normalized
  localStorage.setItem(libraryTreeStateKey, JSON.stringify([...normalized]))
}

function toggleNode(categoryId) {
  const normalizedId = normalizeCategoryId(categoryId)
  const next = new Set(expandedNodes.value)
  if (next.has(normalizedId)) next.delete(normalizedId)
  else next.add(normalizedId)
  persistExpandedNodes(next)
}

function isNodeVisible(node) {
  if (node.parentId == null) return true
  const parent = flatCategoryOptions.value.find((item) => item.id === node.parentId)
  return expandedNodes.value.has(node.parentId) && (!parent || isNodeVisible(parent))
}

function expandParentChain(categoryId) {
  const next = new Set(expandedNodes.value)
  let current = flatCategoryOptions.value.find((item) => item.id === normalizeCategoryId(categoryId))
  while (current?.parentId != null) {
    next.add(current.parentId)
    current = flatCategoryOptions.value.find((item) => item.id === current.parentId)
  }
  persistExpandedNodes(next)
}

function chooseCategory(categoryId) {
  currentView.value = 'library'
  libraryMenuOpen.value = true
  projectMenuOpen.value = false
  systemMenuOpen.value = false
  expandParentChain(categoryId)
  selectCategory(categoryId)
}
function clearAuthState() {
  localStorage.removeItem(storageKey)
  auth.token = ''
  auth.user = null
  currentView.value = 'home'
  selectedItemIds.value = []
  closeAllSubmenus()
}

function logout() {
  clearAuthState()
  showToast('已退出登录', 'success')
}

function handleAuthExpired(message) {
  if (!auth.token) return
  clearAuthState()
  showToast(message || '登录已失效，请重新登录', 'warning')
}

function openSystemView(target) {
  openSystemViewByNavigation(target, logout)
}
const {
  openLibraryDetail: openDetail,
  editLibraryItem,
  removeAttachment,
  createProjectRelatedItem,
} = useLibraryActions({
  api,
  items,
  selectedItem,
  form,
  detailDialogOpen,
  loadItems,
  openDetail: openLibraryDetail,
  fillForm: fillLibraryForm,
  startLibraryCreateContent,
  showToast,
  currentView,
  confirmDelete: askDeleteConfirm,
})

function startCreateContent() {
  startLibraryCreateContent(selectedCategoryId.value)
  setCurrentView('compose')
}
async function fillForm(item) {
  await editLibraryItem(item)
  setCurrentView('compose')
  detailDialogOpen.value = false
}
async function saveLibraryItem() {
  const savedId = await saveItem()
  if (savedId) {
    // 保存成功后刷新附件列表，但保持在编辑页
    const { data } = await api.get(`/items/${savedId}`).catch(() => ({ data: null }))
    if (data?.data) {
      const attachments = data.data.attachments || data.data.item?.attachments || []
      form.attachments = attachments
    }
  }
}

async function handleRemoveAttachment(payload) {
  const source = payload?.source
  const attachment = payload?.attachment || payload
  if (source === 'compose' || currentView.value === 'compose') {
    markAttachmentForDeletion(attachment)
    return
  }
  await removeAttachment(attachment)
}
async function bulkDeleteItems() {
  if (!selectedLibraryItems.value.length) return showToast('请先选择资料', 'warning')
  await bulkDelete(selectedLibraryItems.value.map((item) => item.id))
  selectedItemIds.value = []
}
async function applyLibraryBatchEdit() { await applyBatchEdit(selectedItemIds.value); selectedItemIds.value = [] }
async function removeItem(id) { await deleteItem(id); selectedItemIds.value = selectedItemIds.value.filter((itemId) => itemId !== id) }
async function saveSystemCategory() {
  const ok = await saveCategory()
  if (ok) {
    await loadCategories()
    resetCategoryDraft()
  }
}
async function deleteSystemCategory(categoryId) {
  const ok = await removeCategory(categoryId)
  if (ok) await loadCategories()
}
async function saveSystemUser() { await saveUser() }
async function deleteSystemUser(userId) { await removeUser(userId) }
async function saveSystemRole(config) { await saveRoleConfig(config) }
async function saveSystemDictionary() {
  try {
    await saveDictionaryItem()
    showToast('字典项已保存')
  } catch (error) {
    showToast(error?.response?.data?.message || '保存字典项失败', 'error')
  }
}
async function deleteSystemDictionary(id) {
  try {
    const ok = await askDeleteConfirm('确认删除该字典项吗？', '删除字典项')
    if (!ok) return
    await removeDictionaryItem(id)
    showToast('字典项已删除')
  } catch (error) {
    showToast(error?.response?.data?.message || '删除字典项失败', 'error')
  }
}

function handleGlobalEscape(event) {
  if (event.key !== 'Escape') return

  // Markdown 编辑器处于“浏览器全屏（pageFullscreen）”时：
  // ESC 只退出编辑器浏览器全屏，不触发页面级返回。
  const mdEditorFullscreen = typeof document !== 'undefined'
    ? document.querySelector('.md-editor.md-editor-fullscreen')
    : null
  if (mdEditorFullscreen) {
    event.preventDefault()
    event.stopPropagation()
    mdEditorFullscreen.classList.remove('md-editor-fullscreen')
    if (typeof document !== 'undefined' && document.body?.style?.overflow === 'hidden') {
      document.body.style.overflow = ''
    }
    return
  }

  if (preview.open) {
    preview.open = false
    return
  }
  if (detailDialogOpen.value) {
    detailDialogOpen.value = false
    return
  }
  if (confirmDialog.open) {
    closeDeleteConfirm(false)
    return
  }
  if (batchEditDialog.open) {
    resetBatchEditDialog()
    return
  }
  if (projectProgressDialogOpen.value) {
    projectProgressDialogOpen.value = false
    return
  }
  if (projectFormDialogOpen.value) {
    projectFormDialogOpen.value = false
    return
  }
  if (currentView.value === 'compose') {
    currentView.value = 'library'
  }
}

async function login() {
  loginError.value = ''
  try {
    const { data } = await api.post('/auth/login', { ...loginForm })
    auth.token = data.data?.token || ''
    auth.user = data.data?.user || null
    localStorage.setItem(storageKey, JSON.stringify({ token: auth.token, user: auth.user }))
    await bootstrap()
  } catch (error) { loginError.value = error?.response?.data?.message || '登录失败' }
}
function restoreAuth() {
  try { const raw = localStorage.getItem(storageKey); if (!raw) return; const parsed = JSON.parse(raw); auth.token = parsed.token || ''; auth.user = parsed.user || null } catch { auth.token = ''; auth.user = null }
}

function restoreLibraryTreeState() {
  try {
    const raw = localStorage.getItem(libraryTreeStateKey)
    if (!raw) return
    const parsed = JSON.parse(raw)
    expandedNodes.value = new Set((Array.isArray(parsed) ? parsed : []).map((item) => normalizeCategoryId(item)).filter(Boolean))
  } catch {
    expandedNodes.value = new Set()
  }
}

function ensureDefaultLibraryTreeState() {
  if (expandedNodes.value.size) return
  const topLevelIds = flatCategoryOptions.value
    .filter((node) => node.parentId == null)
    .map((node) => normalizeCategoryId(node.id))
    .filter(Boolean)
  persistExpandedNodes(new Set(topLevelIds))
}

async function bootstrap() {
  await Promise.all([loadProjectDashboard(), loadProjects(), loadCategories(), loadItems(), loadUsers(), loadRolePermissions(), loadCustomers(), loadDictionaries()])
  ensureDefaultLibraryTreeState()
  const activeId = libraryFilters.categoryId || selectedCategoryId.value
  if (activeId) expandParentChain(activeId)
}
const router = useRouter()
const route = useRoute()

// Sync currentView from URL on initial load and browser back/forward
const viewFromRoute = computed(() => route.meta?.view || (route.path === '/' ? 'home' : null))
watch(viewFromRoute, (view) => {
  if (view && view !== currentView.value && auth.token) {
    currentView.value = view
  }
}, { immediate: false })

onMounted(async () => {
  restoreAuth()
  restoreLibraryTreeState()
  window.addEventListener('keydown', handleGlobalEscape)
  // Set initial view from URL
  const initialView = viewFromRoute.value
  if (initialView && initialView !== 'home') {
    currentView.value = initialView
  }
  if (auth.token) await bootstrap()
})
onUnmounted(() => {
  destroyOnlyOfficeEditor()
  window.removeEventListener('keydown', handleGlobalEscape)
})
</script>

<template>
  <LoginView v-if="!auth.token" :login-form="loginForm" :login-error="loginError" @submit="login" />
  <el-container v-else class="app-shell">
    <AppSidebar
      :user-display-name="auth.user?.displayName || auth.user?.username || '当前用户'"
      :current-view="currentView"
      :library-menu-open="libraryMenuOpen"
      :project-menu-open="projectMenuOpen"
      :system-menu-open="systemMenuOpen"
      :training-menu-open="trainingMenuOpen"
      :project-nav-items="projectNavItems"
      :visible-system-nav-items="visibleSystemNavItems"
      :flat-category-options="flatCategoryOptions"
      :library-filters="libraryFilters"
      :selected-category-id="selectedCategoryId"
      :expanded-nodes="expandedNodes"
      :is-node-visible="isNodeVisible"
      :has-children="hasChildren"
      
      @go-home="setCurrentView('home')"
      @toggle-library-menu="toggleLibraryMenu"
      @open-all-library="setCurrentView('attachment-management')"
      @toggle-node="toggleNode"
      @choose-category="chooseCategory"
      @toggle-project-menu="toggleProjectMenu"
      @open-project-view="openProjectView"
      @toggle-system-menu="toggleSystemMenu"
      @toggle-training-menu="toggleTrainingMenu"
      @open-system-view="openSystemView"
      @global-search="handleGlobalSearch"
    />

    <AppMainContent
      :current-view="currentView"
      :current-role-permissions="currentRolePermissions"
      :home-quick-stats="homeQuickStats"
      :project-tracker-summary="projectTrackerSummary"
      :home-project-focus="homeProjectFocus"
      :project-record-feed="projectRecordFeed"
      :home-action-queue="homeActionQueue"
      :project-milestones="projectMilestones"
      :customer-followup-alerts="customerFollowupAlerts"
      
      :library-props="{
        filters: libraryFilters,
        dictionaryOptions: { knowledgeType: getOptions('knowledge_type').map((item) => item.itemValue) },
        flatCategoryOptions,
        projects,
        libraryQuickFilterOptions,
        libraryQuickFilter,
        loading: libraryLoading,
        filteredItems,
        items,
        canCreateContent: currentRolePermissions.canCreateContent,
        canEditContent: currentRolePermissions.canEditContent,
        canDeleteContent: currentRolePermissions.canDeleteContent,
        selectedItemIds,
        selectedItems: selectedLibraryItems,
        allDisplayedSelected,
        formatDateTime,
        libraryPage,
        libraryPageSize,
        libraryTotal,
        libraryPages,
        changePage,
      }"
      :compose-props="{ form, flatCategoryOptions, projects, uploadQueue, saving: librarySaving, renderMarkdown, formatFileSize, previewOpen: preview.open, dictionaryOptions: { knowledgeType: getOptions('knowledge_type').map((item) => item.itemValue) } }"
      :project-props="{
        trackerSubMenus,
        trackerSubView,
        subViewTitle,
        subViewDescription,
        dashboardLoading,
        dashboardCards,
        projectFilters,
        viewProjects,
        projectsLoading,
        selectedProject,
        recentProgressRecords,
        projectDashboard,
        upcomingActionItems,
        relatedItems: relatedLibraryItems,
        formatMoney,
        subViewHighlights,
        dialogs: { projectFormDialogOpen, projectProgressDialogOpen },
        saving: { projectFormSaving, projectProgressSaving },
        forms: { projectForm, projectProgressForm },
        users,
        customers,
        currentUserName: auth.user?.displayName || auth.user?.username || '',
        dictionaryOptions: {
          projectStage: getOptions('project_stage').map((item) => ({ label: item.itemLabel, value: item.itemValue })),
          projectStatus: getOptions('project_status').map((item) => ({ label: item.itemLabel, value: item.itemValue })),
          projectRisk: getOptions('project_risk').map((item) => ({ label: item.itemLabel, value: item.itemValue })),
          projectActivityType: getOptions('project_activity_type').map((item) => ({ label: item.itemLabel, value: item.itemValue })),
          projectContractStatus: getOptions('project_contract_status').map((item) => ({ label: item.itemLabel, value: item.itemValue })),
          projectPaymentStatus: getOptions('project_payment_status').map((item) => ({ label: item.itemLabel, value: item.itemValue })),
          projectAcceptanceStatus: getOptions('project_acceptance_status').map((item) => ({ label: item.itemLabel, value: item.itemValue })),
          projectServiceStatus: getOptions('project_service_status').map((item) => ({ label: item.itemLabel, value: item.itemValue })),
        },
      }"
      :category-props="{ categoriesLoading, categoriesSaving, flatCategories, categoryDraft }"
      :user-props="{ users, roles: systemRoles, roleCards, userDraft, usersLoading, usersSaving }"
      :role-props="{ roleConfigs, rolesSaving, rolePermissionsLoading }"
      :customer-props="{
        loading: customersLoading,
        customers,
        selectedCustomer,
        contacts,
        followups,
        customerFilters,
        customerForm,
        contactForm,
        followupForm,
        projects,
        customerDialogOpen,
        contactDialogOpen,
        followupDialogOpen,
        customerSaving,
        contactSaving,
        followupSaving,
        dictionaryOptions: {
          customerStatus: getOptions('customer_status').map((item) => item.itemValue),
          customerLevel: getOptions('customer_level').map((item) => item.itemValue),
          customerRegion: getOptions('customer_region').map((item) => item.itemValue),
          customerIndustry: getOptions('customer_industry').map((item) => item.itemValue),
          customerType: getOptions('customer_type').map((item) => item.itemValue),
          customerSource: getOptions('customer_source').map((item) => item.itemValue),
          customerStage: getOptions('customer_stage').map((item) => item.itemValue),
          contactDecisionLevel: getOptions('contact_decision_level').map((item) => item.itemValue),
          contactGender: getOptions('contact_gender').map((item) => item.itemValue),
          followupType: getOptions('followup_type').map((item) => item.itemValue),
          followupResult: getOptions('followup_result').map((item) => item.itemValue),
        },
      }"
      :dictionary-props="{ dictionaryGroups, dictionaryDraft, dictionariesLoading, dictionariesSaving }"
      :preview-settings-props="{ itemCount: items.length, categoryCount: flatCategories.length, systemSettings, dashboardStats }"
      :system-overview-props="{
        itemCount: systemSettings.itemCount || items.length,
        categoryCount: systemSettings.categoryCount || flatCategories.length,
        projectCount: projects.length,
        roleCount: roleConfigs.length || roleCards.length,
        userCount: systemSettings.userCount || users.length,
        appName: systemSettings.appName || '知识平台 CMS',
        userRole: auth.user?.role || 'SALES',
        dashboardStats,
      }"
      @open-system-view="openSystemView"
      @open-project-view="handleHomeOpenProjectView"
      @open-project-progress-from-home="handleHomeOpenProjectProgress"
      @start-create-content="startCreateContent"
      @open-all-library="setCurrentView('library')"
      @reset-library-filters="resetLibraryFilters"
      @load-items="loadItems"
      @page-change="changePage"
      @size-change="(size) => { libraryPageSize = size; changePage(1) }"
      @trigger-import="triggerImport"
      @update-library-quick-filter="updateLibraryQuickFilter"
      @toggle-select-all-displayed="toggleSelectAllDisplayed"
      @open-batch-edit-dialog="openBatchEditDialog"
      @bulk-delete-items="bulkDeleteItems"
      @clear-selected-items="clearSelectedItems"
      @toggle-select-item="toggleSelectItem"
      @open-detail="openDetail"
      @fill-form="fillForm"
      @delete-item="removeItem"
      @compose-back="openLibraryHome"
      @save-library-item="saveLibraryItem"
      @files-change="handleFileChange"
      @remove-queued-file="removeQueuedFile"
      @open-preview="openPreview"
      @remove-attachment="handleRemoveAttachment"
      @switch-subview="switchTrackerSubView"
      @refresh-projects="() => Promise.all([loadProjectDashboard(), loadProjects(selectedProject?.id)])"
      @open-project-progress="openProjectProgressDialog"
      @open-project-edit="openProjectEditDialog"
      @open-related-item="openLibraryDetail"
      @create-related-item="createProjectRelatedItem"
      @select-project="selectProject"
      @reset-project-filters="resetProjectFilters"
      @filter-projects="() => loadProjects(selectedProject?.id)"
      @delete-project="deleteProject"
      @delete-project-progress="deleteProjectProgress"
      @close-project-form="closeProjectFormDialog"
      @save-project="saveProject"
      @close-project-progress="closeProjectProgressDialog"
      @save-project-progress="saveProjectProgress"
      @edit-category="editCategory"
      @create-child-category="createChildCategory"
      @reset-category="resetCategoryDraft"
      @save-category="saveSystemCategory"
      @delete-category="deleteSystemCategory"
      @reset-user="resetUserDraft"
      @save-user="saveSystemUser"
      @edit-user="editUser"
      @delete-user="deleteSystemUser"
      @save-role="saveSystemRole"
      @filter-customers="() => loadCustomers(selectedCustomer?.id)"
      @reset-customer-filters="resetCustomerFilters"
      @select-customer="selectCustomer"
      @open-customer="openCustomerDialog"
      @delete-customer="deleteCustomer"
      @open-contact="openContactDialog"
      @delete-contact="deleteContact"
      @save-customer="saveCustomer"
      @close-customer="closeCustomerDialog"
      @save-contact="saveContact"
      @close-contact="closeContactDialog"
      @open-followup="openFollowupDialog"
      @delete-followup="deleteFollowup"
      @save-followup="saveFollowup"
      @close-followup="closeFollowupDialog"
      @edit-dictionary="editDictionaryItem"
      @reset-dictionary="resetDictionaryDraft"
      @save-dictionary="saveSystemDictionary"
      @delete-dictionary="deleteSystemDictionary"
    />

    <LibraryDetailDialog v-model="detailDialogOpen" :item="selectedItem" :format-date-time="formatDateTime" :format-file-size="formatFileSize" :render-markdown="renderMarkdown" :api-base-url="apiBaseUrl" :can-delete-content="currentRolePermissions.canDeleteContent" @edit="fillForm" @open-preview="openPreview" @delete-attachment="handleRemoveAttachment" />
    <LibraryBatchEditDialog v-model="batchEditDialog.open" :batch-edit-dialog="batchEditDialog" :selected-count="selectedItemIds.length" :flat-category-options="flatCategoryOptions" @apply="applyLibraryBatchEdit" @update:model-value="(value) => { if (!value) resetBatchEditDialog(); else batchEditDialog.open = value }" />
    <el-dialog
      v-model="confirmDialog.open"
      width="420px"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      align-center
      class="cms-delete-dialog"
    >
      <div class="cms-delete-dialog-body">
        <div class="cms-delete-dialog-icon" :class="{ danger: confirmDialog.danger }">!</div>
        <div class="cms-delete-dialog-copy">
          <h3>{{ confirmDialog.title }}</h3>
          <p>{{ confirmDialog.message }}</p>
        </div>
      </div>
      <template #footer>
        <div class="cms-delete-dialog-footer">
          <el-button @click="closeDeleteConfirm(false)">取消</el-button>
          <el-button :type="confirmDialog.danger ? 'danger' : 'primary'" @click="closeDeleteConfirm(true)">确认删除</el-button>
        </div>
      </template>
    </el-dialog>
    <el-dialog v-model="preview.open" fullscreen destroy-on-close append-to-body :z-index="300000" class="preview-dialog-shell">
      <template #header>
        <div class="preview-header">
          <div>
            <div class="preview-title-row">
              <h3>{{ preview.fileName }}</h3>
              <el-tag :type="preview.kind === 'onlyoffice' ? 'success' : preview.kind === 'pdf' ? 'warning' : 'info'" effect="light">{{ previewEngineLabel }}</el-tag>
            </div>
            <p>{{ preview.message || '已根据附件类型选择预览方式。' }}</p>
            <p class="preview-hint">{{ previewHint }}</p>
          </div>
          <el-space>
            <el-button @click="preview.open = false">返回详情</el-button>
            <el-button @click="reloadPreview" :loading="previewLoading">刷新预览</el-button>
            <el-button tag="a" :href="preview.downloadUrl || preview.url" target="_blank">打开原文件</el-button>
          </el-space>
        </div>
      </template>
      <div class="preview-stage">
        <el-skeleton v-if="previewLoading" :rows="8" animated />
        <div v-else-if="canUseOnlyOfficeComponent" ref="onlyOfficeMountRef" class="preview-frame onlyoffice-host"></div>
        <div v-else-if="preview.kind === 'image'" class="preview-image-stage">
          <img :src="preview.url" class="preview-image" :alt="preview.fileName" draggable="false" />
        </div>
        <video v-else-if="preview.kind === 'video'" controls class="preview-media" :src="preview.url"></video>
        <audio v-else-if="preview.kind === 'audio'" controls class="preview-audio" :src="preview.url"></audio>
        <embed v-else-if="preview.kind === 'pdf'" class="preview-frame" :src="previewFrameUrl" type="application/pdf" />
        <iframe v-else-if="isPreviewFrameKind(preview.kind)" class="preview-frame" :src="previewFrameUrl" :title="preview.fileName" loading="lazy" sandbox="allow-scripts allow-same-origin"></iframe>
        <el-empty v-else description="当前文件暂不支持直接预览，请打开原文件查看。" />
      </div>
      <div class="preview-meta">
        <el-tag effect="plain">类型：{{ preview.contentType }}</el-tag>
        <el-tag effect="plain">大小：{{ preview.fileSize }}</el-tag>
        <el-tag effect="plain">上传时间：{{ preview.uploadedAt }}</el-tag>
      </div>
    </el-dialog>
    <input ref="importInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="handleImport" />
    <el-dialog v-model="globalSearchDialogOpen" title="全局搜索结果" width="700px" destroy-on-close>
      <el-skeleton v-if="globalSearchLoading" :rows="6" animated />
      <div v-else-if="globalSearchResults">
        <div v-if="globalSearchResults.items?.length" style="margin-bottom: 16px;">
          <h4>知识库 ({{ globalSearchResults.items.length }})</h4>
          <el-table :data="globalSearchResults.items.slice(0, 5)" size="small" stripe>
            <el-table-column prop="title" label="标题" min-width="300" />
            <el-table-column prop="type" label="类型" width="100" />
          </el-table>
        </div>
        <div v-if="globalSearchResults.projects?.length" style="margin-bottom: 16px;">
          <h4>项目 ({{ globalSearchResults.projects.length }})</h4>
          <el-table :data="globalSearchResults.projects.slice(0, 5)" size="small" stripe>
            <el-table-column prop="name" label="项目名称" min-width="300" />
            <el-table-column prop="stage" label="阶段" width="120" />
          </el-table>
        </div>
        <div v-if="globalSearchResults.customers?.length" style="margin-bottom: 16px;">
          <h4>客户 ({{ globalSearchResults.customers.length }})</h4>
          <el-table :data="globalSearchResults.customers.slice(0, 5)" size="small" stripe>
            <el-table-column prop="name" label="客户名称" min-width="300" />
            <el-table-column prop="industry" label="行业" width="120" />
          </el-table>
        </div>
        <el-empty v-if="!globalSearchResults.items?.length && !globalSearchResults.projects?.length && !globalSearchResults.customers?.length" description="未找到匹配结果" />
      </div>
      <el-empty v-else description="请输入关键词搜索" />
    </el-dialog>
  </el-container>
</template>
