import { useRouter } from 'vue-router'
import { pushView, pathForView } from '../services/uiHistory'

export function useShellNavigation({
  currentView,
  libraryMenuOpen,
  projectMenuOpen,
  systemMenuOpen,
  trainingMenuOpen,
  openAllLibrary,
  switchTrackerSubView,
}) {
  const router = useRouter()

  function syncRoute(view) {
    // 内存路由：仅同步路径镜像，不碰浏览器 History
    const path = pathForView(view)
    if (path && router.currentRoute.value.path !== path) {
      router.push(path).catch(() => {})
    }
    // 浏览器前进/后退：写入完整 UI 状态（切视图时清掉弹窗）
    pushView(view)
  }

  function closeAllSubmenus() {
    libraryMenuOpen.value = false
    projectMenuOpen.value = false
    systemMenuOpen.value = false
    trainingMenuOpen.value = false
  }

  /** 手风琴：同一时间只展开一个主菜单 */
  function expandOnly(key) {
    libraryMenuOpen.value = key === 'library'
    projectMenuOpen.value = key === 'project'
    systemMenuOpen.value = key === 'system'
    trainingMenuOpen.value = key === 'training'
  }

  function setCurrentView(view) {
    currentView.value = view
    syncRoute(view)
    if (view === 'home') {
      closeAllSubmenus()
      return
    }
    if (view === 'library' || view === 'compose' || view === 'attachment-management') {
      expandOnly('library')
      if (view === 'library') openAllLibrary()
      return
    }
    if (view === 'vector-search' || view === 'vector-maintenance' || view === 'vector-health') {
      expandOnly('library')
      return
    }
    if (view === 'skill-assistant') {
      closeAllSubmenus()
      return
    }
    if (String(view).startsWith('project')) {
      expandOnly('project')
      return
    }
    if (view === 'customers') {
      closeAllSubmenus()
      return
    }
    if (view === 'training' || view === 'assessment') {
      expandOnly('training')
      return
    }
    expandOnly('system')
  }

  function openProjectView(view) {
    currentView.value = view
    syncRoute(view)
    expandOnly('project')
    const mapping = {
      'project-tracker': 'dashboard',
      'project-sales': 'sales',
      'project-presales': 'presales',
      'project-delivery-ops': 'deliveryOps',
      'project-finance': 'finance',
      'project-gantt': 'gantt',
      'project-weekly-progress': 'weeklyProgress',
    }
    switchTrackerSubView(mapping[view] || 'dashboard')
  }

  function toggleLibraryMenu() {
    if (libraryMenuOpen.value) {
      libraryMenuOpen.value = false
      return
    }
    // 展开时进入知识库，避免首页等其它项仍保持选中
    setCurrentView('library')
    openAllLibrary()
  }

  function openLibraryHome() {
    setCurrentView('library')
    openAllLibrary()
  }

  function toggleProjectMenu() {
    if (projectMenuOpen.value) {
      projectMenuOpen.value = false
      return
    }
    if (!String(currentView.value).startsWith('project')) {
      openProjectView('project-tracker')
    } else {
      expandOnly('project')
    }
  }

  function toggleSystemMenu() {
    if (systemMenuOpen.value) {
      systemMenuOpen.value = false
      return
    }
    const systemViews = ['settings', 'users', 'roles', 'preview-settings', 'system-overview', 'dictionary-settings']
    if (!systemViews.includes(currentView.value)) {
      setCurrentView('settings')
    } else {
      expandOnly('system')
    }
  }

  function toggleTrainingMenu() {
    if (trainingMenuOpen.value) {
      trainingMenuOpen.value = false
      return
    }
    if (!['training', 'assessment'].includes(currentView.value)) {
      setCurrentView('training')
    } else {
      expandOnly('training')
    }
  }

  function openSystemView(target, onLogout) {
    if (target === 'logout') {
      onLogout?.()
      return
    }
    currentView.value = target
    syncRoute(target)
    if (target === 'customers') {
      closeAllSubmenus()
      return
    }
    if (target === 'training' || target === 'assessment') {
      expandOnly('training')
      return
    }
    expandOnly('system')
  }

  return {
    closeAllSubmenus,
    expandOnly,
    setCurrentView,
    openProjectView,
    toggleLibraryMenu,
    openLibraryHome,
    toggleProjectMenu,
    toggleSystemMenu,
    toggleTrainingMenu,
    openSystemView,
  }
}
