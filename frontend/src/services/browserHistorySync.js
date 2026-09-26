// 浏览器前进/后退桥（视图 + 弹窗状态）
// - Vue Router 仍用内存历史，不碰 History API（避免 Edge 被顶窗）
// - 本模块用 location.hash 表达完整 UI 状态：视图 + 详情/预览/编辑
// - 示例：
//     #/library
//     #/library?detail=9
//     #/library?detail=9&preview=14
//     #/compose?item=9
//     #/skill-assistant?detail=9

const viewToPath = {
  home: '/',
  library: '/library',
  compose: '/compose',
  'project-tracker': '/project-tracker',
  'project-sales': '/project-sales',
  'project-presales': '/project-presales',
  'project-delivery-ops': '/project-delivery-ops',
  'project-finance': '/project-finance',
  'project-gantt': '/project-gantt',
  'project-weekly-progress': '/project-weekly-progress',
  customers: '/customers',
  training: '/training',
  assessment: '/assessment',
  'attachment-management': '/attachment-management',
  'vector-search': '/vector-search',
  'vector-maintenance': '/vector-maintenance',
  'vector-health': '/vector-health',
  'skill-assistant': '/skill-assistant',
  settings: '/settings',
  'dictionary-settings': '/dictionary-settings',
  users: '/users',
  roles: '/roles',
  'preview-settings': '/preview-settings',
  'system-overview': '/system-overview',
}

const pathToView = Object.fromEntries(
  Object.entries(viewToPath).map(([view, path]) => [path, view]),
)
pathToView['/training-management'] = 'training'

const OVERLAY_KEYS = ['detail', 'preview', 'item', 'skillDetail']

let applyingFromBrowser = false
let bound = false
let applyState = null
let lastHash = null

function isHidden() {
  return typeof document !== 'undefined' && document.visibilityState === 'hidden'
}

function emptyState() {
  return {
    view: 'home',
    detailId: null,
    previewId: null,
    composeId: null,
    skillDetailId: null,
  }
}

function parseHash(hash) {
  const raw = (hash || '').replace(/^#/, '')
  const [pathPart, queryPart] = raw.split('?')
  const path = (pathPart || '/').startsWith('/') ? pathPart || '/' : `/${pathPart}`
  const view = pathToView[path] || (path === '/' ? 'home' : null)
  const query = new URLSearchParams(queryPart || '')
  const num = (key) => {
    const v = query.get(key)
    return v != null && v !== '' && !Number.isNaN(Number(v)) ? Number(v) : null
  }
  return {
    view: view || 'home',
    detailId: num('detail'),
    previewId: num('preview'),
    composeId: num('item'),
    skillDetailId: num('skillDetail'),
  }
}

function hashForState(state) {
  const path = viewToPath[state.view] || '/'
  const query = new URLSearchParams()
  if (state.detailId != null) query.set('detail', String(state.detailId))
  if (state.previewId != null) query.set('preview', String(state.previewId))
  if (state.composeId != null) query.set('item', String(state.composeId))
  if (state.skillDetailId != null) query.set('skillDetail', String(state.skillDetailId))
  const qs = query.toString()
  return `#${path}${qs ? `?${qs}` : ''}`
}

function normalizeState(partial) {
  const base = emptyState()
  const next = { ...base, ...(partial || {}) }
  if (typeof next.view !== 'string' || !viewToPath[next.view]) next.view = 'home'
  const toId = (v) => (v == null || v === '' || Number.isNaN(Number(v)) ? null : Number(v))
  next.detailId = toId(next.detailId)
  next.previewId = toId(next.previewId)
  next.composeId = toId(next.composeId)
  next.skillDetailId = toId(next.skillDetailId)
  // compose 视图用 item 参数
  if (next.view === 'compose' && next.composeId == null) {
    // 允许空白 compose（新建）
    next.composeId = next.composeId ?? null
  }
  return next
}

export function parseLocationState() {
  if (typeof window === 'undefined') return emptyState()
  return normalizeState(parseHash(window.location.hash))
}

/** 应用内状态变化 → 写入浏览器历史 */
export function syncStateToBrowser(state, { replace = false } = {}) {
  if (typeof window === 'undefined') return
  if (isHidden() || applyingFromBrowser) return
  const next = normalizeState(state)
  const hash = hashForState(next)
  if (window.location.hash === hash) {
    lastHash = hash
    return
  }
  try {
    const payload = { ...next, hash }
    if (replace || lastHash == null) {
      window.history.replaceState(payload, '', hash)
    } else {
      window.history.pushState(payload, '', hash)
    }
    lastHash = hash
  } catch (_) {
    /* History API 不可用时静默降级 */
  }
}

function handlePopState(event) {
  if (isHidden() || !applyState) return
  const fromEvent = event?.state && typeof event.state === 'object' && event.state.view
    ? normalizeState(event.state)
    : parseLocationState()
  const hash = hashForState(fromEvent)
  lastHash = hash
  applyingFromBrowser = true
  try {
    applyState(fromEvent)
  } finally {
    setTimeout(() => {
      applyingFromBrowser = false
    }, 0)
  }
}

/**
 * @param {(state: object) => void} onStateApplied 浏览器后退/前进时应用完整状态
 * @returns {object} 初始状态
 */
export function initBrowserHistory(onStateApplied) {
  applyState = onStateApplied
  if (typeof window === 'undefined') return emptyState()

  if (!bound) {
    window.addEventListener('popstate', handlePopState)
    bound = true
  }

  const initial = parseLocationState()
  const hash = hashForState(initial)
  lastHash = hash
  try {
    if (!window.history.state?.view) {
      window.history.replaceState({ ...initial, hash }, '', hash)
    }
  } catch (_) {
    /* ignore */
  }
  return initial
}

export function disposeBrowserHistory() {
  if (typeof window === 'undefined' || !bound) return
  window.removeEventListener('popstate', handlePopState)
  bound = false
  applyState = null
}

export function canWriteBrowserHistory() {
  return !isHidden() && !applyingFromBrowser
}

/** 是否正在应用浏览器前进/后退（用于避免回声） */
export function isApplyingFromBrowser() {
  return applyingFromBrowser
}

/** 兼容旧接口：按 view 同步（无弹窗状态） */
export function syncViewToBrowser(view) {
  syncStateToBrowser(normalizeState({ view }))
}

export function pathForView(view) {
  return viewToPath[view] || '/'
}

export function viewFromLocation() {
  return parseLocationState().view
}

export { viewToPath, OVERLAY_KEYS }
