// 浏览器前进/后退桥：Vue Router 使用内存历史（不碰 History API），
// 由本模块在「窗口可见」时用 pushState/hash 维护浏览器历史。
// 窗口隐藏（最小化）时完全不写 History，避免 Edge 在 localhost 下把窗口顶回来。

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

let applyingFromBrowser = false
let bound = false
let applyView = null
let lastPath = null

function isHidden() {
  return typeof document !== 'undefined' && document.visibilityState === 'hidden'
}

function normalizeHashPath() {
  const raw = (typeof window !== 'undefined' && window.location.hash) || ''
  const path = raw.replace(/^#/, '').split('?')[0] || '/'
  return path.startsWith('/') ? path : `/${path}`
}

export function viewFromLocation() {
  const path = normalizeHashPath()
  return pathToView[path] || (path === '/' ? 'home' : null)
}

export function pathForView(view) {
  return viewToPath[view] || '/'
}

/** 应用内切换视图 → 写入浏览器历史（仅可见时） */
export function syncViewToBrowser(view) {
  if (typeof window === 'undefined' || !view) return
  if (isHidden() || applyingFromBrowser) return
  const path = pathForView(view)
  const hash = `#${path}`
  if (window.location.hash === hash) {
    lastPath = path
    return
  }
  try {
    if (lastPath == null) {
      window.history.replaceState({ view, path }, '', hash)
    } else {
      window.history.pushState({ view, path }, '', hash)
    }
    lastPath = path
  } catch (_) {
    // 某些 Web 环境禁止 History API 时静默降级
  }
}

function handlePopState(event) {
  if (isHidden() || !applyView) return
  const path = normalizeHashPath()
  const view = event?.state?.view || pathToView[path] || (path === '/' ? 'home' : null)
  if (!view) return
  applyingFromBrowser = true
  lastPath = path
  try {
    applyView(view)
  } finally {
    // 等一拍再放开，避免 applyView 内部再次 sync 造成回声
    setTimeout(() => {
      applyingFromBrowser = false
    }, 0)
  }
}

/**
 * 初始化：绑定 popstate，并解析当前 hash 作为初始视图。
 * @param {(view: string) => void} onViewApplied 浏览器后退/前进时应用视图
 */
export function initBrowserHistory(onViewApplied) {
  applyView = onViewApplied
  if (typeof window === 'undefined') return viewFromLocation()

  if (!bound) {
    window.addEventListener('popstate', handlePopState)
    bound = true
  }

  // 刷新/直链：用当前 hash 初始化，但不 push
  const initial = viewFromLocation()
  const path = normalizeHashPath()
  lastPath = path
  try {
    if (!window.history.state?.view && initial) {
      window.history.replaceState({ view: initial, path }, '', `#${path}`)
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
  applyView = null
}

/** 调试：当前是否允许写浏览器历史 */
export function canWriteBrowserHistory() {
  return !isHidden() && !applyingFromBrowser
}
