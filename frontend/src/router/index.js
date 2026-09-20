import { createRouter, createMemoryHistory } from 'vue-router'
import { pathForView } from '../services/browserHistorySync'

// 空 stub：真实界面由 App.vue 的 currentView 渲染
const stub = { template: '<div />' }

const routes = [
  { path: '/', name: 'Home', component: stub, meta: { view: 'home' } },
  { path: '/library', name: 'Library', component: stub, meta: { view: 'library' } },
  { path: '/compose', name: 'Compose', component: stub, meta: { view: 'compose' } },
  { path: '/project-tracker', name: 'ProjectTracker', component: stub, meta: { view: 'project-tracker' } },
  { path: '/project-sales', name: 'ProjectSales', component: stub, meta: { view: 'project-sales' } },
  { path: '/project-presales', name: 'ProjectPresales', component: stub, meta: { view: 'project-presales' } },
  { path: '/project-delivery-ops', name: 'ProjectDeliveryOps', component: stub, meta: { view: 'project-delivery-ops' } },
  { path: '/project-finance', name: 'ProjectFinance', component: stub, meta: { view: 'project-finance' } },
  { path: '/project-gantt', name: 'ProjectGantt', component: stub, meta: { view: 'project-gantt' } },
  { path: '/project-weekly-progress', name: 'ProjectWeeklyProgress', component: stub, meta: { view: 'project-weekly-progress' } },
  { path: '/customers', name: 'Customers', component: stub, meta: { view: 'customers' } },
  { path: '/training', name: 'Training', component: stub, meta: { view: 'training' } },
  { path: '/assessment', name: 'Assessment', component: stub, meta: { view: 'assessment' } },
  { path: '/training-management', name: 'TrainingManagementAlias', component: stub, meta: { view: 'training' } },
  { path: '/attachment-management', name: 'AttachmentManagement', component: stub, meta: { view: 'attachment-management' } },
  { path: '/settings', name: 'Settings', component: stub, meta: { view: 'settings' } },
  { path: '/dictionary-settings', name: 'DictionarySettings', component: stub, meta: { view: 'dictionary-settings' } },
  { path: '/users', name: 'Users', component: stub, meta: { view: 'users' } },
  { path: '/roles', name: 'Roles', component: stub, meta: { view: 'roles' } },
  { path: '/preview-settings', name: 'PreviewSettings', component: stub, meta: { view: 'preview-settings' } },
  { path: '/system-overview', name: 'SystemOverview', component: stub, meta: { view: 'system-overview' } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: stub, meta: { view: 'home' } },
]

// 关键：Vue Router 使用内存历史，不调用 pushState/hash API，
// 避免 Edge 在 localhost 下最小化后被 History 操作顶回前台。
// 浏览器前进/后退由 services/browserHistorySync.js 在窗口可见时维护。
const router = createRouter({
  history: createMemoryHistory(),
  routes,
})

/** 供外部对齐内存路由（可选，界面不依赖 router-view） */
export function syncMemoryRoute(view) {
  const path = pathForView(view)
  if (path && router.currentRoute.value.path !== path) {
    router.push(path).catch(() => {})
  }
}

export default router
