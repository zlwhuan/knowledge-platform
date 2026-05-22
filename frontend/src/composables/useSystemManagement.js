import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../services/api'

const ACTIVE_ROLES = ['ADMIN', 'SALES', 'PRESALES', 'DELIVERY_OPS', 'FINANCE']

const systemFieldLabels = {
  username: '用户名',
  displayName: '显示名',
  password: '密码',
  role: '角色',
  enabled: '启用状态',
  name: '分类名称',
  code: '分类编码',
  description: '分类描述',
  sortOrder: '排序',
}

export function useSystemManagement(options = {}) {
  const { confirmDelete } = options
  const usersLoading = ref(false)
  const categoriesSaving = ref(false)
  const usersSaving = ref(false)
  const rolesSaving = ref(false)
  const rolePermissionsLoading = ref(false)

  const users = ref([])
  const roleConfigs = ref([])
  const systemSettings = reactive({
    appName: '知识平台 CMS',
    officeCommand: '',
    onlyOfficeEnabled: false,
    serverPort: '8080',
    itemCount: 0,
    categoryCount: 0,
    userCount: 0,
  })
  const dashboardStats = reactive({
    itemCount: 0,
    categoryCount: 0,
    attachmentCount: 0,
    markdownItemCount: 0,
    missingSummaryCount: 0,
    missingAttachmentCount: 0,
    recentUpdatedCount: 0,
  })

  const categoryDraft = reactive({
    id: null,
    name: '',
    code: '',
    parentId: '',
    sortOrder: 10,
    description: '',
  })

  const userDraft = reactive({
    id: null,
    username: '',
    displayName: '',
    password: '',
    role: 'SALES',
    enabled: true,
  })

  function toast(message, type = 'success') {
    ElMessage({ message, type, showClose: true, duration: 2200 })
  }

  function formatErrorMessage(error, fallback) {
    const message = error?.response?.data?.message || fallback
    const [field, detail] = String(message).split(':')
    if (!detail) return message
    return `${systemFieldLabels[field.trim()] || field.trim()}：${detail.trim()}`
  }

  const roleCards = computed(() => [
    { value: 'ADMIN', label: '管理员', description: '全部权限与系统治理', count: users.value.filter((user) => user.role === 'ADMIN').length },
    { value: 'SALES', label: '销售', description: '商机与合同推进', count: users.value.filter((user) => user.role === 'SALES').length },
    { value: 'PRESALES', label: '售前', description: '方案支撑与投标协同', count: users.value.filter((user) => user.role === 'PRESALES').length },
    { value: 'DELIVERY_OPS', label: '实施运维', description: '交付、验收、售后一体', count: users.value.filter((user) => user.role === 'DELIVERY_OPS').length },
    { value: 'FINANCE', label: '财务', description: '回款与结算跟踪', count: users.value.filter((user) => user.role === 'FINANCE').length },
  ])

  async function loadUsers() {
    usersLoading.value = true
    try {
      const { data } = await api.get('/users')
      users.value = data.data || []
    } catch (error) {
      toast(error?.response?.data?.message || '加载用户失败', 'error')
    } finally {
      usersLoading.value = false
    }
  }

  async function loadRolePermissions() {
    rolePermissionsLoading.value = true
    try {
      const { data } = await api.get('/roles')
      roleConfigs.value = (data.data || []).filter((item) => ACTIVE_ROLES.includes(item.role))
    } catch (error) {
      toast(error?.response?.data?.message || '加载角色权限失败', 'error')
    } finally {
      rolePermissionsLoading.value = false
    }
  }

  async function loadSystemOverview(fallback = {}) {
    try {
      const { data } = await api.get('/system/overview')
      Object.assign(systemSettings, data.data || {})
    } catch {
      Object.assign(systemSettings, {
        appName: fallback.appName || systemSettings.appName,
        officeCommand: fallback.officeCommand || '',
        onlyOfficeEnabled: Boolean(fallback.onlyOfficeEnabled),
        serverPort: String(fallback.serverPort || systemSettings.serverPort || '8080'),
        itemCount: fallback.itemCount ?? systemSettings.itemCount,
        categoryCount: fallback.categoryCount ?? systemSettings.categoryCount,
        userCount: fallback.userCount ?? systemSettings.userCount,
      })
    }
  }

  async function loadDashboardStats(fallback = {}) {
    try {
      const { data } = await api.get('/items/dashboard/stats')
      Object.assign(dashboardStats, data.data || {})
    } catch {
      Object.assign(dashboardStats, {
        itemCount: fallback.itemCount ?? dashboardStats.itemCount,
        categoryCount: fallback.categoryCount ?? dashboardStats.categoryCount,
        attachmentCount: fallback.attachmentCount ?? dashboardStats.attachmentCount,
        markdownItemCount: fallback.markdownItemCount ?? dashboardStats.markdownItemCount,
        missingSummaryCount: fallback.missingSummaryCount ?? dashboardStats.missingSummaryCount,
        missingAttachmentCount: fallback.missingAttachmentCount ?? dashboardStats.missingAttachmentCount,
        recentUpdatedCount: fallback.recentUpdatedCount ?? dashboardStats.recentUpdatedCount,
      })
    }
  }

  async function saveUser() {
    if (!String(userDraft.username || '').trim() || !String(userDraft.displayName || '').trim()) {
      toast('请完整填写用户名和显示名', 'warning')
      return false
    }
    if (!userDraft.id && !String(userDraft.password || '').trim()) {
      toast('新建用户时必须填写密码', 'warning')
      return false
    }
    usersSaving.value = true
    try {
      const payload = {
        username: String(userDraft.username || '').trim(),
        displayName: String(userDraft.displayName || '').trim(),
        password: userDraft.password || undefined,
        role: userDraft.role,
        enabled: userDraft.enabled,
      }
      if (userDraft.id) await api.put(`/users/${userDraft.id}`, payload)
      else await api.post('/users', payload)
      await loadUsers()
      const isEditing = Boolean(userDraft.id)
      resetUserDraft()
      toast(isEditing ? '用户已更新' : '用户已创建')
      return true
    } catch (error) {
      toast(formatErrorMessage(error, '保存用户失败'), 'error')
      return false
    } finally {
      usersSaving.value = false
    }
  }

  async function removeUser(userId) {
    const ok = confirmDelete ? await confirmDelete('确认删除该用户吗？', '删除用户') : true
    if (!ok) return
    try {
      await api.delete(`/users/${userId}`)
      await loadUsers()
      if (userDraft.id === userId) resetUserDraft()
      toast('用户已删除')
    } catch (error) {
      toast(error?.response?.data?.message || '删除用户失败', 'error')
    }
  }

  function editUser(user) {
    Object.assign(userDraft, {
      id: user.id,
      username: user.username,
      displayName: user.displayName,
      password: '',
      role: user.role,
      enabled: user.enabled,
    })
  }

  function resetUserDraft() {
    Object.assign(userDraft, {
      id: null,
      username: '',
      displayName: '',
      password: '',
      role: 'SALES',
      enabled: true,
    })
  }

  async function saveCategory() {
    if (!String(categoryDraft.name || '').trim()) {
      toast('请先填写分类名称', 'warning')
      return false
    }
    if (String(categoryDraft.name || '').trim().length > 100) {
      toast('分类名称不能超过 100 个字符', 'warning')
      return false
    }
    if (String(categoryDraft.code || '').length > 100) {
      toast('分类编码不能超过 100 个字符', 'warning')
      return false
    }
    if (String(categoryDraft.description || '').length > 500) {
      toast('分类描述不能超过 500 个字符', 'warning')
      return false
    }
    categoriesSaving.value = true
    try {
      const payload = {
        name: String(categoryDraft.name || '').trim(),
        code: String(categoryDraft.code || '').trim(),
        description: categoryDraft.description || '',
        parentId: categoryDraft.parentId || null,
        sortOrder: categoryDraft.sortOrder ?? 10,
      }
      if (categoryDraft.id) await api.put(`/categories/${categoryDraft.id}`, payload)
      else await api.post('/categories', payload)
      toast(categoryDraft.id ? '分类已更新' : '分类已创建')
      return true
    } catch (error) {
      toast(formatErrorMessage(error, '保存分类失败'), 'error')
      return false
    } finally {
      categoriesSaving.value = false
    }
  }

  async function removeCategory(categoryId) {
    const ok = confirmDelete ? await confirmDelete('确认删除该分类吗？若存在子级或资料引用，后端会拦截。', '删除分类') : true
    if (!ok) return false
    try {
      await api.delete(`/categories/${categoryId}`)
      toast('分类已删除')
      if (categoryDraft.id === categoryId) resetCategoryDraft()
      return true
    } catch (error) {
      toast(error?.response?.data?.message || '删除分类失败', 'error')
      return false
    }
  }

  function editCategory(category) {
    Object.assign(categoryDraft, {
      id: category.id,
      name: category.name || '',
      code: category.code || '',
      parentId: category.parentId || '',
      sortOrder: category.sortOrder ?? 10,
      description: category.description || '',
    })
  }

  function createChildCategory(parentId) {
    Object.assign(categoryDraft, {
      id: null,
      name: '',
      code: '',
      parentId,
      sortOrder: 10,
      description: '',
    })
  }

  function resetCategoryDraft() {
    Object.assign(categoryDraft, {
      id: null,
      name: '',
      code: '',
      parentId: '',
      sortOrder: 10,
      description: '',
    })
  }

  async function saveRoleConfig(config) {
    rolesSaving.value = true
    try {
      await api.put(`/roles/${config.role}`, {
        canViewLibrary: config.canViewLibrary,
        canCreateContent: config.canCreateContent,
        canEditContent: config.canEditContent,
        canDeleteContent: config.canDeleteContent,
        canManageCategories: config.canManageCategories,
        canManageUsers: config.canManageUsers,
        canManageRoles: config.canManageRoles,
        canPreviewOffice: config.canPreviewOffice,
      })
      await loadRolePermissions()
      toast('角色权限已保存')
    } catch (error) {
      toast(formatErrorMessage(error, '保存角色权限失败'), 'error')
    } finally {
      rolesSaving.value = false
    }
  }

  return {
    usersLoading, categoriesSaving, usersSaving, rolesSaving, rolePermissionsLoading,
    users, roleConfigs, roleCards, categoryDraft, userDraft, systemSettings, dashboardStats,
    loadUsers, loadRolePermissions, loadSystemOverview, loadDashboardStats, saveUser, removeUser, editUser, resetUserDraft,
    saveCategory, removeCategory, editCategory, createChildCategory, resetCategoryDraft, saveRoleConfig,
  }
}
