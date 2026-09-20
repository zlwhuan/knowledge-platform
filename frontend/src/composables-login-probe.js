import { createApp, h, reactive } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './components/LoginView.vue'
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
import './style.css'

const routes = [
  { path: '/', name: 'Home', component: { template: '<div />' }, meta: { view: 'home' } },
]
const router = createRouter({ history: createWebHistory(), routes })

const loginForm = reactive({ username: 'admin', password: 'Admin@123' })
const auth = reactive({ token: '', user: null })
const currentView = refShim()
const libraryMenuOpen = refShim(false)
const projectMenuOpen = refShim(false)
const systemMenuOpen = refShim(false)
const trainingMenuOpen = refShim(false)

function refShim(v) {
  // local minimal ref without importing vue ref to keep probe explicit
  const state = { value: v }
  return state
}

const noopConfirm = async () => true

// Same composable init as App.vue (login path still runs setup)
const tracker = useProjectTracker({ confirmDelete: noopConfirm, getCurrentUserName: () => '' })
const library = useKnowledgeLibrary({ confirmDelete: noopConfirm })
const system = useSystemManagement({ confirmDelete: noopConfirm })
const customers = useCustomerManagement({ confirmDelete: noopConfirm })
const dicts = useDictionaries()
const preview = useAttachmentPreview({
  api,
  apiBaseUrl: '/api',
  showToast: () => {},
  formatDateTime: String,
  formatFileSize: String,
})
const nav = useShellNavigation({
  currentView,
  libraryMenuOpen,
  projectMenuOpen,
  systemMenuOpen,
  trainingMenuOpen,
  openAllLibrary: () => {},
  switchTrackerSubView: () => {},
})
const dash = useHomeDashboard({
  items: library.items,
  libraryFilters: library.libraryFilters,
  flatCategoryOptions: library.flatCategories,
  projectDashboard: tracker.projectDashboard,
  recentProgressRecords: tracker.recentProgressRecords,
  viewProjects: tracker.viewProjects,
  selectedProject: tracker.selectedProject,
  customers: customers.customers,
  formatMoney: (x) => String(x ?? ''),
  formatDateTime: (x) => String(x ?? ''),
})
const libActions = useLibraryActions({
  api,
  items: library.items,
  selectedItem: library.selectedItem,
  form: library.form,
  detailDialogOpen: library.detailDialogOpen,
  loadItems: library.loadItems,
  openDetail: library.openDetail,
  fillForm: library.fillForm,
  startLibraryCreateContent: library.startCreateContent,
  showToast: () => {},
  currentView,
  confirmDelete: noopConfirm,
})

attachAuthToken(() => auth.token)
attachAuthGuard(() => {})

// keep referenced so bundler keeps them
void [
  tracker, library, system, customers, dicts, preview, nav, dash, libActions,
  tracker.loadProjects, library.loadItems, dicts.loadDictionaries,
]

const app = createApp({
  render() {
    return h(LoginView, { loginForm, loginError: '', onSubmit: () => {} })
  },
})
app.use(ElementPlus)
app.use(router)
app.mount('#app')
