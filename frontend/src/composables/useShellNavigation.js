import { useRouter } from 'vue-router'

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
    const routeMap = {
      'home': '/',
      'library': '/library',
      'compose': '/compose',
      'project-tracker': '/project-tracker',
      'project-sales': '/project-sales',
      'project-presales': '/project-presales',
      'project-delivery-ops': '/project-delivery-ops',
      'project-finance': '/project-finance',
      'project-gantt': '/project-gantt',
      'customers': '/customers',
      'training': '/training',
      'assessment': '/assessment',
      'settings': '/settings',
      'dictionary-settings': '/dictionary-settings',
      'users': '/users',
      'roles': '/roles',
      'preview-settings': '/preview-settings',
      'system-overview': '/system-overview',
    }
    const path = routeMap[view]
    if (path && router.currentRoute.value.path !== path) {
      router.push(path)
    }
  }

  function closeAllSubmenus() {
    libraryMenuOpen.value = false
    projectMenuOpen.value = false
    systemMenuOpen.value = false
    trainingMenuOpen.value = false
  }

  function setCurrentView(view) {
    currentView.value = view
    syncRoute(view)
    if (view === 'home') {
      closeAllSubmenus()
      return
    }
    if (view === 'library' || view === 'compose' || view === 'attachment-management') {
      libraryMenuOpen.value = true
      projectMenuOpen.value = false
      systemMenuOpen.value = false
      trainingMenuOpen.value = false
      if (view === 'library') openAllLibrary()
      return
    }
    if (String(view).startsWith('project')) {
      libraryMenuOpen.value = false
      projectMenuOpen.value = true
      systemMenuOpen.value = false
      trainingMenuOpen.value = false
      return
    }
    if (view === 'customers') {
      libraryMenuOpen.value = false
      projectMenuOpen.value = false
      systemMenuOpen.value = false
      trainingMenuOpen.value = false
      return
    }
    if (view === 'training' || view === 'assessment') {
      libraryMenuOpen.value = false
      projectMenuOpen.value = false
      systemMenuOpen.value = false
      trainingMenuOpen.value = true
      return
    }
    // 系统配置视图
    libraryMenuOpen.value = false
    projectMenuOpen.value = false
    systemMenuOpen.value = true
    trainingMenuOpen.value = false
  }

  function openProjectView(view) {
    currentView.value = view
    syncRoute(view)
    libraryMenuOpen.value = false
    projectMenuOpen.value = true
    systemMenuOpen.value = false
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
    const next = !libraryMenuOpen.value
    libraryMenuOpen.value = next
    if (next) {
      projectMenuOpen.value = false
      systemMenuOpen.value = false
      if (!['library', 'compose'].includes(currentView.value)) setCurrentView('library')
    }
  }

  function openLibraryHome() {
    currentView.value = 'library'
    syncRoute('library')
    libraryMenuOpen.value = true
    projectMenuOpen.value = false
    systemMenuOpen.value = false
    openAllLibrary()
  }

  function toggleProjectMenu() {
    const next = !projectMenuOpen.value
    projectMenuOpen.value = next
    if (next) {
      libraryMenuOpen.value = false
      systemMenuOpen.value = false
      if (!String(currentView.value).startsWith('project')) openProjectView('project-tracker')
    }
  }

  function toggleSystemMenu() {
    const next = !systemMenuOpen.value
    systemMenuOpen.value = next
    if (next) {
      libraryMenuOpen.value = false
      projectMenuOpen.value = false
      trainingMenuOpen.value = false
      if (!['settings', 'users', 'roles', 'preview-settings', 'system-overview', 'customers', 'dictionary-settings'].includes(currentView.value)) setCurrentView('settings')
    }
  }

  function toggleTrainingMenu() {
    const next = !trainingMenuOpen.value
    trainingMenuOpen.value = next
    if (next) {
      libraryMenuOpen.value = false
      projectMenuOpen.value = false
      systemMenuOpen.value = false
      if (!['training', 'assessment'].includes(currentView.value)) setCurrentView('training')
    }
  }

  function openSystemView(target, onLogout) {
    if (target === 'logout') {
      onLogout?.()
      return
    }
    currentView.value = target
    syncRoute(target)
    libraryMenuOpen.value = false
    projectMenuOpen.value = false
    systemMenuOpen.value = ['customers', 'training', 'assessment'].includes(target) ? false : true
    trainingMenuOpen.value = ['training', 'assessment'].includes(target) ? true : false
  }

  return {
    closeAllSubmenus,
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
