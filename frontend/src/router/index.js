import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: { template: '<div />' },
    meta: { view: 'home' },
  },
  {
    path: '/library',
    name: 'Library',
    component: { template: '<div />' },
    meta: { view: 'library' },
  },
  {
    path: '/compose',
    name: 'Compose',
    component: { template: '<div />' },
    meta: { view: 'compose' },
  },
  {
    path: '/project-tracker',
    name: 'ProjectTracker',
    component: { template: '<div />' },
    meta: { view: 'project-tracker' },
  },
  {
    path: '/project-sales',
    name: 'ProjectSales',
    component: { template: '<div />' },
    meta: { view: 'project-sales' },
  },
  {
    path: '/project-presales',
    name: 'ProjectPresales',
    component: { template: '<div />' },
    meta: { view: 'project-presales' },
  },
  {
    path: '/project-delivery-ops',
    name: 'ProjectDeliveryOps',
    component: { template: '<div />' },
    meta: { view: 'project-delivery-ops' },
  },
  {
    path: '/project-finance',
    name: 'ProjectFinance',
    component: { template: '<div />' },
    meta: { view: 'project-finance' },
  },
  {
    path: '/project-gantt',
    name: 'ProjectGantt',
    component: { template: '<div />' },
    meta: { view: 'project-gantt' },
  },
  {
    path: '/customers',
    name: 'Customers',
    component: { template: '<div />' },
    meta: { view: 'customers' },
  },
  {
    path: '/training-management',
    name: 'TrainingManagement',
    component: { template: '<div />' },
    meta: { view: 'training-management' },
  },
  {
    path: '/settings',
    name: 'Settings',
    component: { template: '<div />' },
    meta: { view: 'settings' },
  },
  {
    path: '/dictionary-settings',
    name: 'DictionarySettings',
    component: { template: '<div />' },
    meta: { view: 'dictionary-settings' },
  },
  {
    path: '/users',
    name: 'Users',
    component: { template: '<div />' },
    meta: { view: 'users' },
  },
  {
    path: '/roles',
    name: 'Roles',
    component: { template: '<div />' },
    meta: { view: 'roles' },
  },
  {
    path: '/preview-settings',
    name: 'PreviewSettings',
    component: { template: '<div />' },
    meta: { view: 'preview-settings' },
  },
  {
    path: '/system-overview',
    name: 'SystemOverview',
    component: { template: '<div />' },
    meta: { view: 'system-overview' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
