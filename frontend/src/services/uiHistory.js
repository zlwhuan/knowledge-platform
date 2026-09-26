// 站内 UI 状态 ↔ 浏览器历史（视图 + 详情/预览/编辑/技能面板）
import {
  syncStateToBrowser,
  parseLocationState,
  isApplyingFromBrowser,
  pathForView,
} from './browserHistorySync'

function emptyState() {
  return {
    view: 'home',
    detailId: null,
    previewId: null,
    composeId: null,
    skillDetailId: null,
  }
}

let current = emptyState()
const listeners = new Set()

/** 订阅 UI 状态变化（浏览器前进/后退、深链恢复） */
export function onUiState(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}

function emitUiState() {
  const snapshot = getUiState()
  listeners.forEach((fn) => {
    try {
      fn(snapshot)
    } catch (_) {
      /* ignore listener errors */
    }
  })
}

export function getUiState() {
  return { ...current }
}

export function setUiState(next, { replace = false } = {}) {
  current = { ...emptyState(), ...next }
  syncStateToBrowser(current, { replace })
  return getUiState()
}

/** 切换主视图：清掉所有弹窗状态 */
export function pushView(view) {
  return setUiState({ view })
}

/** 打开知识条目详情 */
export function pushDetail(view, detailId) {
  return setUiState({ ...current, view, detailId, previewId: null, skillDetailId: null })
}

/** 在详情上叠预览 */
export function pushPreview(previewId) {
  return setUiState({ ...current, previewId })
}

/** 编辑/新建（compose） */
export function pushCompose(composeId = null) {
  return setUiState({ view: 'compose', composeId })
}

/** 技能助手侧栏详情 */
export function pushSkillDetail(skillDetailId) {
  return setUiState({ ...current, view: 'skill-assistant', skillDetailId })
}

/** 关闭顶层弹窗：优先 history.back，保证后退/关闭一致 */
export function popOverlay() {
  if (typeof window !== 'undefined' && window.history.length > 1) {
    try {
      window.history.back()
      return true
    } catch (_) {
      /* fall through */
    }
  }
  // 退不出时就地去掉弹窗
  return setUiState({ ...current, detailId: null, previewId: null, skillDetailId: null, composeId: current.view === 'compose' ? null : current.composeId })
}

/** 从 hash 恢复（刷新/深链） */
export function restoreUiStateFromLocation() {
  current = parseLocationState()
  return getUiState()
}

/** 浏览器前进/后退应用状态（在 initBrowserHistory 回调里用） */
export function adoptUiState(state) {
  current = { ...emptyState(), ...state }
  emitUiState()
  return getUiState()
}

export { isApplyingFromBrowser, pathForView }
