import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../services/api'
import {
  createDefaultProjectActivityForm,
  createDefaultProjectFilters,
  createDefaultProjectForm,
  createDefaultProjectProgressForm,
  mapActivityToForm,
  mapProgressToForm,
  mapProjectToForm,
} from './projectFormState'

const projectFieldLabels = {
  name: '项目名称',
  customerName: '客户名称',
  stage: '项目阶段',
  status: '项目状态',
  progress: '项目进度',
  recordType: '记录类型',
  content: '动态内容',
  summary: '进展摘要',
}

const riskWeight = { 高: 3, 中: 2, 低: 1 }
const stageFlow = ['商机立项', '合同执行', '实施交付', '验收结算', '售后维保']

export function useProjectTracker(options = {}) {
  const { confirmDelete, getCurrentUserName } = options
  const trackerSubView = ref('dashboard')
  const projectsLoading = ref(false)
  const dashboardLoading = ref(false)
  const projectFormSaving = ref(false)
  const projectActivitySaving = ref(false)
  const projectProgressSaving = ref(false)
  const projectFormDialogOpen = ref(false)
  const projectActivityDialogOpen = ref(false)
  const projectProgressDialogOpen = ref(false)

  const projects = ref([])
  const projectDashboard = ref({
    totalProjects: 0,
    activeProjects: 0,
    highRiskProjects: 0,
    acceptancePendingProjects: 0,
    paymentPendingProjects: 0,
    totalContractAmount: 0,
    totalReceivedAmount: 0,
    stageCounts: {},
    statusCounts: {},
    roleTodoCounts: {},
  })
  const selectedProjectId = ref(null)

  const projectFilters = reactive(createDefaultProjectFilters())
  const projectForm = reactive(createDefaultProjectForm())
  const projectActivityForm = reactive(createDefaultProjectActivityForm())
  const projectProgressForm = reactive(createDefaultProjectProgressForm())

  const trackerSubMenus = [
    { key: 'dashboard', label: '管理总览', desc: '全局项目盘点与关键指标' },
    { key: 'sales', label: '销售', desc: '商机立项与合同执行' },
    { key: 'presales', label: '售前', desc: '前期方案支撑与投标协同' },
    { key: 'deliveryOps', label: '实施运维', desc: '实施交付、验收结算、售后维保' },
    { key: 'finance', label: '财务', desc: '合同金额、回款、结算跟踪' },
    { key: 'weeklyProgress', label: '周进度', desc: '按周查看所有项目进度记录' },
  ]

  const selectedProject = computed(() => projects.value.find((item) => item.id === selectedProjectId.value) || null)

  const recentProgressRecords = computed(() => projects.value
    .flatMap((project) => (project.progressRecords || []).map((record) => ({
      ...record,
      projectId: project.id,
      projectName: project.name,
      customerName: project.customerName,
    })))
    .sort((a, b) => new Date(b.recordTime || b.createdAt || 0) - new Date(a.recordTime || a.createdAt || 0))
    .slice(0, 8))

  const dashboardCards = computed(() => [
    { label: '项目总数', value: projectDashboard.value.totalProjects, hint: `进行中 ${projectDashboard.value.activeProjects}` },
    { label: '高风险项目', value: projectDashboard.value.highRiskProjects, hint: '优先盯办' },
    { label: '待验收', value: projectDashboard.value.acceptancePendingProjects, hint: '尽快推动闭环' },
    { label: '待回款', value: projectDashboard.value.paymentPendingProjects, hint: '财务协同催收' },
  ])

  function parseDate(value) {
    if (!value) return null
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? null : date
  }

  function calcDueMeta(rawDate) {
    const date = parseDate(rawDate)
    if (!date) return null
    const today = new Date()
    const midnightToday = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    const midnightTarget = new Date(date.getFullYear(), date.getMonth(), date.getDate())
    const diffDays = Math.round((midnightTarget - midnightToday) / 86400000)
    return {
      date: rawDate,
      diffDays,
      overdue: diffDays < 0,
      dueSoon: diffDays >= 0 && diffDays <= 7,
    }
  }

  function resolveOwnerByStage(project, stage, fallbackOwner = '') {
    return String(fallbackOwner || '').trim()
      || (stage === '合同执行' ? (project.salesOwner || project.projectManager || '--') : '')
      || (stage === '实施交付' || stage === '验收结算' ? (project.implementationOwner || project.projectManager || '--') : '')
      || (stage === '售后维保' ? (project.serviceOwner || project.projectManager || '--') : '')
      || project.projectManager
      || '--'
  }

  function getProgressDrivenTodos(project) {
    const records = [...(project.progressRecords || [])]
      .sort((a, b) => new Date(b.recordTime || b.createdAt || 0) - new Date(a.recordTime || a.createdAt || 0))

    if (!records.length) return []

    const latestByStage = new Map()
    records.forEach((item) => {
      const stageKey = String(item.stage || project.stage || '--').trim() || '--'
      if (!latestByStage.has(stageKey)) latestByStage.set(stageKey, item)
    })

    const todos = []

    Array.from(latestByStage.values()).forEach((latest) => {
      const stage = latest.stage || project.stage || '--'
      const status = String(latest.status || '').trim()
      const progress = Number(latest.progress || 0)
      const nextAction = String(latest.nextAction || '').trim()
      const dueMeta = calcDueMeta(latest.nextActionDueDate)

      if (status === '已完成' || progress >= 100) {
        const nextStage = stageFlow[stageFlow.indexOf(stage) + 1]
        if (!nextStage) return

        const nextStageHasAnyRecord = records.some((item) => String(item.stage || '').trim() === nextStage)
        if (nextStageHasAnyRecord) return

        const transitionDate = String(latest.recordTime || latest.createdAt || '').slice(0, 10)
        const transitionDue = calcDueMeta(transitionDate) || { date: transitionDate || new Date().toISOString().slice(0, 10), diffDays: 0, overdue: false, dueSoon: true }

        todos.push({
          projectId: project.id,
          projectName: project.name,
          customerName: project.customerName,
          type: `${nextStage}启动待办`,
          owner: resolveOwnerByStage(project, nextStage),
          ...transitionDue,
          nextAction: `上一阶段已完成，请启动「${nextStage}」并补充首条进度记录`,
          nextActionDueDate: transitionDue.date,
          riskLevel: latest.riskLevel || project.riskLevel || '低',
          projectStage: nextStage,
          sourceType: 'progress',
          progressRecordId: latest.id,
          transitionTodo: true,
        })
        return
      }

      if (!nextAction || !dueMeta) return

      todos.push({
        projectId: project.id,
        projectName: project.name,
        customerName: project.customerName,
        type: `${stage}待办`,
        owner: resolveOwnerByStage(project, stage, latest.ownerName),
        ...dueMeta,
        nextAction,
        nextActionDueDate: latest.nextActionDueDate || null,
        riskLevel: latest.riskLevel || project.riskLevel || '低',
        projectStage: stage,
        sourceType: 'progress',
        progressRecordId: latest.id,
      })
    })

    return todos
  }

  function getNearestProgressDue(project) {
    return getProgressDrivenTodos(project)
      .sort((a, b) => a.diffDays - b.diffDays)[0] || null
  }

  const sortedProjects = computed(() => [...projects.value].sort((left, right) => {
    const leftDue = getNearestProgressDue(left)
    const rightDue = getNearestProgressDue(right)
    const leftRisk = riskWeight[left.riskLevel] || 0
    const rightRisk = riskWeight[right.riskLevel] || 0
    if (rightRisk !== leftRisk) return rightRisk - leftRisk
    if (!!leftDue !== !!rightDue) return leftDue ? -1 : 1
    if (leftDue && rightDue && leftDue.diffDays !== rightDue.diffDays) return leftDue.diffDays - rightDue.diffDays
    return new Date(right.updatedAt || 0) - new Date(left.updatedAt || 0)
  }))

  const viewProjects = computed(() => {
    const all = sortedProjects.value
    switch (trackerSubView.value) {
      case 'sales': return all.filter((item) => ['商机立项', '合同执行'].includes(item.stage))
      case 'presales': return all.filter((item) => ['商机立项', '合同执行'].includes(item.stage))
      case 'deliveryOps': return all.filter((item) => ['实施交付', '验收结算', '售后维保'].includes(item.stage))
      case 'finance': return all.filter((item) => Number(item.contractAmount || 0) > 0 || Number(item.receivedAmount || 0) > 0 || ['合同执行', '验收结算'].includes(item.stage))
      default: return all
    }
  })

  const upcomingActionItems = computed(() => viewProjects.value
    .flatMap((project) => getProgressDrivenTodos(project))
    .filter((item) => item && (item.overdue || item.dueSoon))
    .sort((a, b) => {
      if (a.overdue !== b.overdue) return a.overdue ? -1 : 1
      if (a.diffDays !== b.diffDays) return a.diffDays - b.diffDays
      return (riskWeight[b.riskLevel] || 0) - (riskWeight[a.riskLevel] || 0)
    }))

  const subViewTitle = computed(() => trackerSubMenus.find((item) => item.key === trackerSubView.value)?.label || '项目工作台')
  const subViewDescription = computed(() => trackerSubMenus.find((item) => item.key === trackerSubView.value)?.desc || '')

  function showToast(message, type = 'success') {
    ElMessage({ message, type, showClose: true, duration: 2200 })
  }

  function formatMoney(value) {
    return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
  }

  function formatErrorMessage(error, fallback) {
    const message = error?.response?.data?.message || fallback
    const [field, detail] = String(message).split(':')
    if (!detail) return message
    return `${projectFieldLabels[field.trim()] || field.trim()}：${detail.trim()}`
  }

  function subViewHighlights(project) {
    if (!project) return []
    if (trackerSubView.value === 'sales') return [`合同状态：${project.contractStatus || '待签约'}`, `销售负责人：${project.salesOwner || '--'}`, `阶段：${project.stage || '--'}`]
    if (trackerSubView.value === 'presales') return [`售前协同：${project.documentOwner || '--'}`, `项目经理：${project.projectManager || '--'}`, `阶段：${project.stage || '--'}`]
    if (trackerSubView.value === 'deliveryOps') return [`实施负责人：${project.implementationOwner || '--'}`, `服务负责人：${project.serviceOwner || '--'}`, `验收状态：${project.acceptanceStatus || '待验收'}`]
    if (trackerSubView.value === 'finance') return [`合同额：¥${formatMoney(project.contractAmount)}`, `已回款：¥${formatMoney(project.receivedAmount)}`, `回款状态：${project.paymentStatus || '待回款'}`]
    return [`阶段：${project.stage || '--'} / ${project.status || '--'}`, `风险：${project.riskLevel || '--'}`, `项目经理：${project.projectManager || '--'}`]
  }

  async function confirmDeleteAction(message, title = '删除确认') {
    if (confirmDelete) return confirmDelete(message, title)
    return true
  }

  async function loadProjectDashboard() {
    dashboardLoading.value = true
    try {
      const { data } = await api.get('/projects/dashboard')
      projectDashboard.value = data.data || projectDashboard.value
    } finally {
      dashboardLoading.value = false
    }
  }

  async function loadProjects(preferredId = null) {
    projectsLoading.value = true
    try {
      const params = {}
      if (projectFilters.keyword) params.keyword = projectFilters.keyword
      if (projectFilters.customerName) params.customerName = projectFilters.customerName
      if (projectFilters.stage) params.stage = projectFilters.stage
      if (projectFilters.status) params.status = projectFilters.status
      if (projectFilters.owner) params.owner = projectFilters.owner
      if (projectFilters.riskLevel) params.riskLevel = projectFilters.riskLevel
      const { data } = await api.get('/projects', { params })
      projects.value = data.data || []
      const targetId = preferredId ?? selectedProjectId.value ?? projects.value[0]?.id ?? null
      selectedProjectId.value = projects.value.find((item) => item.id === targetId)?.id || projects.value[0]?.id || null
    } catch {
      projects.value = []
      selectedProjectId.value = null
    } finally {
      projectsLoading.value = false
    }
  }

  function resetProjectForm() {
    Object.assign(projectForm, createDefaultProjectForm())
  }

  function resetProjectActivityForm(row = selectedProject.value) {
    Object.assign(projectActivityForm, createDefaultProjectActivityForm(row))
  }

  function resetProjectProgressForm(row = selectedProject.value) {
    Object.assign(projectProgressForm, createDefaultProjectProgressForm(row))
    const currentUserName = String(getCurrentUserName?.() || '').trim()
    if (currentUserName) projectProgressForm.ownerName = currentUserName
  }

  function closeProjectFormDialog() {
    projectFormDialogOpen.value = false
    resetProjectForm()
  }

  function closeProjectActivityDialog() {
    projectActivityDialogOpen.value = false
    resetProjectActivityForm(selectedProject.value)
  }

  function closeProjectProgressDialog() {
    projectProgressDialogOpen.value = false
    resetProjectProgressForm(selectedProject.value)
  }

  function validateProjectForm() {
    if (!String(projectForm.name || '').trim()) return showToast('请填写项目名称', 'warning')
    if (!String(projectForm.customerName || '').trim()) return showToast('请填写客户名称', 'warning')
    if (Number(projectForm.progress) < 0 || Number(projectForm.progress) > 100) return showToast('项目进度需在 0-100 之间', 'warning')
    if (Number(projectForm.receivedAmount || 0) > Number(projectForm.contractAmount || 0) && Number(projectForm.contractAmount || 0) > 0) {
      return showToast('已回款不能大于合同额', 'warning')
    }
    return true
  }

  function validateProjectActivityForm() {
    if (!String(projectActivityForm.recordType || '').trim()) return showToast('请选择记录类型', 'warning')
    if (!String(projectActivityForm.content || '').trim()) return showToast('请填写动态内容', 'warning')
    return true
  }

  function validateProjectProgressForm() {
    if (!String(projectProgressForm.stage || '').trim()) return showToast('请选择推进阶段', 'warning')
    if (!String(projectProgressForm.status || '').trim()) return showToast('请选择当前状态', 'warning')
    if (!String(projectProgressForm.summary || '').trim()) return showToast('请填写本次进展摘要', 'warning')
    if (!projectProgressForm.recordTime) return showToast('请填写记录时间', 'warning')
    if (Number(projectProgressForm.progress) < 0 || Number(projectProgressForm.progress) > 100) return showToast('完成进度需在 0-100 之间', 'warning')
    const isFinished = String(projectProgressForm.status || '').trim() === '已完成'
    if (!isFinished && String(projectProgressForm.nextAction || '').trim() && !projectProgressForm.nextActionDueDate) {
      return showToast('填写了下一步动作时，请同时填写截止日期', 'warning')
    }
    return true
  }

  function selectProject(row) {
    if (row) selectedProjectId.value = row.id
  }

  function switchTrackerSubView(key) {
    trackerSubView.value = key
  }


  function openProjectEditDialog(row = null) {
    if (row) {
      Object.assign(projectForm, mapProjectToForm(row))
      selectedProjectId.value = row.id
    } else {
      resetProjectForm()
    }
    projectFormDialogOpen.value = true
  }

  function openProjectActivityDialog(rowOrPayload = selectedProject.value, activityArg = null) {
    const row = rowOrPayload?.row ?? rowOrPayload ?? selectedProject.value
    const activity = rowOrPayload?.activity ?? activityArg
    if (row) selectedProjectId.value = row.id
    if (activity) {
      Object.assign(projectActivityForm, mapActivityToForm(activity))
    } else {
      resetProjectActivityForm(row)
    }
    projectActivityDialogOpen.value = true
  }

  function openProjectProgressDialog(rowOrPayload = selectedProject.value, recordArg = null) {
    const row = rowOrPayload?.row ?? rowOrPayload ?? selectedProject.value
    const record = rowOrPayload?.record ?? recordArg
    const todo = rowOrPayload?.todo ?? null
    if (row) selectedProjectId.value = row.id
    if (record) {
      Object.assign(projectProgressForm, mapProgressToForm(record, row))
    } else {
      resetProjectProgressForm(row)
      if (todo) {
        projectProgressForm.stage = todo.projectStage || row?.stage || projectProgressForm.stage
        projectProgressForm.ownerName = todo.owner || ''
        projectProgressForm.nextAction = todo.nextAction || ''
        projectProgressForm.nextActionDueDate = todo.nextActionDueDate || todo.date || ''
      }
    }
    projectProgressDialogOpen.value = true
  }

  async function saveProject() {
    if (!validateProjectForm()) return
    projectFormSaving.value = true
    const editing = !!projectForm.id
    try {
      const mergedLinks = [
        ...(Array.isArray(projectForm.customerContactLinks) ? projectForm.customerContactLinks : []),
        ...(Array.isArray(projectForm.relatedContactLinks) ? projectForm.relatedContactLinks : []),
      ]

      const normalizedLinks = mergedLinks
        .filter((item) => String(item?.contactKey || '').trim())
        .map((item) => ({
          sourceType: String(item.sourceType || 'customer').trim(),
          relatedCustomerType: String(item.relatedCustomerType || '').trim(),
          relatedCustomerName: String(item.relatedCustomerName || '').trim(),
          contactKey: String(item.contactKey || '').trim(),
          customerName: String(item.customerName || '').trim(),
          contactId: String(item.contactId || '').trim(),
          contactName: String(item.contactName || '').trim(),
          department: String(item.department || '').trim(),
          position: String(item.position || '').trim(),
          mobile: String(item.mobile || '').trim(),
        }))
        .filter((item, index, arr) => arr.findIndex((target) => target.contactKey === item.contactKey) === index)

      const { customerType, ...projectFormPayload } = projectForm
      const payload = {
        ...projectFormPayload,
        projectContactIds: normalizedLinks.map((item) => item.contactId).filter(Boolean).join(','),
        projectContactLinks: JSON.stringify(normalizedLinks),
        relatedContactNotes: '',
      }
      const request = editing ? api.put(`/projects/${projectForm.id}`, payload) : api.post('/projects', payload)
      const { data } = await request
      closeProjectFormDialog()
      await Promise.all([loadProjectDashboard(), loadProjects(data.data?.id || projectForm.id)])
      showToast(editing ? '项目已更新' : '项目已创建')
    } catch (error) {
      showToast(formatErrorMessage(error, '项目保存失败'), 'error')
    } finally {
      projectFormSaving.value = false
    }
  }

  async function deleteProject(row) {
    const confirmed = await confirmDeleteAction(`确认删除项目“${row?.name || ''}”吗？相关动态和进度也会一起删除。`, '删除项目')
    if (!confirmed) return
    try {
      await api.delete(`/projects/${row.id}`)
      await Promise.all([loadProjectDashboard(), loadProjects()])
      showToast('项目已删除')
    } catch (error) {
      showToast(formatErrorMessage(error, '项目删除失败'), 'error')
    }
  }

  async function saveProjectActivity() {
    const targetProjectId = projectActivityForm.projectId || selectedProject.value?.id
    if (!targetProjectId) return showToast('请先选择项目', 'warning')
    if (!validateProjectActivityForm()) return
    projectActivitySaving.value = true
    const editing = !!projectActivityForm.id
    try {
      const payload = {
        recordType: String(projectActivityForm.recordType || '').trim(),
        ownerName: String(projectActivityForm.ownerName || '').trim(),
        content: String(projectActivityForm.content || '').trim(),
        recordTime: projectActivityForm.recordTime || null,
      }
      const request = editing
        ? api.put(`/projects/activities/${projectActivityForm.id}`, payload)
        : api.post(`/projects/${targetProjectId}/activities`, payload)
      await request
      closeProjectActivityDialog()
      await loadProjects(targetProjectId)
      showToast(editing ? '项目动态已更新' : '项目动态已记录')
    } catch (error) {
      showToast(formatErrorMessage(error, '记录保存失败'), 'error')
    } finally {
      projectActivitySaving.value = false
    }
  }

  async function deleteProjectActivity(activity) {
    const confirmed = await confirmDeleteAction('确认删除这条项目动态吗？', '删除动态')
    if (!confirmed) return
    try {
      await api.delete(`/projects/activities/${activity.id}`)
      await loadProjects(selectedProject.value?.id)
      showToast('项目动态已删除')
    } catch (error) {
      showToast(formatErrorMessage(error, '动态删除失败'), 'error')
    }
  }

  async function saveProjectProgress() {
    const targetProjectId = projectProgressForm.projectId || selectedProject.value?.id
    if (!targetProjectId) return showToast('请先选择项目', 'warning')
    if (!validateProjectProgressForm()) return
    projectProgressSaving.value = true
    const editing = !!projectProgressForm.id
    try {
      const normalizedStatus = String(projectProgressForm.status || '').trim()
      const payload = {
        stage: String(projectProgressForm.stage || '').trim(),
        status: normalizedStatus,
        progress: Number(projectProgressForm.progress || 0),
        riskLevel: String(projectProgressForm.riskLevel || '').trim(),
        summary: String(projectProgressForm.summary || '').trim(),
        nextAction: normalizedStatus === '已完成' ? '' : String(projectProgressForm.nextAction || '').trim(),
        nextActionDueDate: normalizedStatus === '已完成' ? null : (projectProgressForm.nextActionDueDate || null),
        ownerName: String(projectProgressForm.ownerName || '').trim(),
        recordTime: projectProgressForm.recordTime || null,
      }
      const request = editing
        ? api.put(`/projects/progress-records/${projectProgressForm.id}`, payload)
        : api.post(`/projects/${targetProjectId}/progress-records`, payload)
      await request
      closeProjectProgressDialog()
      await Promise.all([loadProjectDashboard(), loadProjects(targetProjectId)])
      showToast(editing ? '项目进度已更新' : '项目进度已留痕')
    } catch (error) {
      showToast(formatErrorMessage(error, '进度记录失败'), 'error')
    } finally {
      projectProgressSaving.value = false
    }
  }

  async function deleteProjectProgress(record) {
    const confirmed = await confirmDeleteAction('确认删除这条进度记录吗？', '删除进度')
    if (!confirmed) return
    try {
      await api.delete(`/projects/progress-records/${record.id}`)
      await Promise.all([loadProjectDashboard(), loadProjects(selectedProject.value?.id)])
      showToast('项目进度已删除')
    } catch (error) {
      showToast(formatErrorMessage(error, '进度删除失败'), 'error')
    }
  }

  function resetProjectFilters() {
    Object.assign(projectFilters, createDefaultProjectFilters())
    loadProjects(selectedProjectId.value)
  }

  return {
    trackerSubView,
    projectsLoading,
    dashboardLoading,
    projectFormSaving,
    projectActivitySaving,
    projectProgressSaving,
    projectFormDialogOpen,
    projectActivityDialogOpen,
    projectProgressDialogOpen,
    projects,
    projectDashboard,
    selectedProjectId,
    selectedProject,
    recentProgressRecords,
    projectFilters,
    projectForm,
    projectActivityForm,
    projectProgressForm,
    trackerSubMenus,
    dashboardCards,
    viewProjects,
    upcomingActionItems,
    subViewTitle,
    subViewDescription,
    formatMoney,
    subViewHighlights,
    loadProjectDashboard,
    loadProjects,
    selectProject,
    switchTrackerSubView,
    openProjectEditDialog,
    openProjectActivityDialog,
    openProjectProgressDialog,
    closeProjectFormDialog,
    closeProjectActivityDialog,
    closeProjectProgressDialog,
    saveProject,
    deleteProject,
    saveProjectActivity,
    deleteProjectActivity,
    saveProjectProgress,
    deleteProjectProgress,
    resetProjectFilters,
  }
}
