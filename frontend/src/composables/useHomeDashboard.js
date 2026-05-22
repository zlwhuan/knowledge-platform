import { computed } from 'vue'

export function useHomeDashboard({
  items,
  libraryFilters,
  flatCategoryOptions,
  projectDashboard,
  recentProgressRecords,
  viewProjects,
  selectedProject,
  customers,
  formatMoney,
  formatDateTime,
}) {
  const currentCategoryName = computed(() => {
    const activeId = libraryFilters.categoryId || null
    const hit = flatCategoryOptions.value.find((item) => String(item.id) === String(activeId))
    return hit?.name || '全部资料'
  })

  const projectTrackerSummary = computed(() => [
    { label: '项目总数', value: `${projectDashboard.value.totalProjects || 0}`, hint: `进行中 ${projectDashboard.value.activeProjects || 0}` },
    { label: '高风险项目', value: `${projectDashboard.value.highRiskProjects || 0}`, hint: '优先盯办' },
    { label: '待验收', value: `${projectDashboard.value.acceptancePendingProjects || 0}`, hint: '尽快推动闭环' },
    { label: '待回款', value: `${projectDashboard.value.paymentPendingProjects || 0}`, hint: '财务协同催收' },
  ])

  const projectMilestones = computed(() => recentProgressRecords.value.slice(0, 6).map((item) => ({
    time: formatDateTime(item.recordTime || item.createdAt),
    title: `${item.projectName} · ${item.stage || '未标注阶段'}`,
    type: item.riskLevel === '高' ? 'danger' : item.riskLevel === '中' ? 'warning' : 'primary',
    desc: item.summary || '暂无进度摘要',
  })))

  const projectRecordFeed = computed(() => recentProgressRecords.value.slice(0, 8).map((item) => ({
    stage: item.stage || '未标注阶段',
    project: item.projectName,
    owner: item.ownerName || '--',
    content: item.summary || '暂无进度摘要',
    time: formatDateTime(item.recordTime || item.createdAt),
  })))

  function resolveProjectTodo(project) {
    const records = [...(project.progressRecords || [])]
      .sort((a, b) => new Date(b.recordTime || b.createdAt || 0) - new Date(a.recordTime || a.createdAt || 0))

    const latestByStage = new Map()
    records.forEach((item) => {
      const stage = String(item.stage || '').trim()
      if (!stage) return
      if (!latestByStage.has(stage)) latestByStage.set(stage, item)
    })

    const todos = Array.from(latestByStage.values())
      .filter((item) => String(item.status || '').trim() !== '已完成' && Number(item.progress || 0) < 100)
      .filter((item) => String(item.nextAction || '').trim() && item.nextActionDueDate)
      .sort((a, b) => new Date(a.nextActionDueDate).getTime() - new Date(b.nextActionDueDate).getTime())

    return todos[0] || null
  }

  const homeProjectFocus = computed(() => viewProjects.value.slice(0, 12).map((item) => {
    const todo = resolveProjectTodo(item)
    return {
      id: item.id,
      name: item.name,
      stage: item.stage,
      progress: item.progress || 0,
      next: todo ? `${todo.stage}：${todo.nextAction}` : '暂无下一步安排',
      owner: `项目经理 / ${item.projectManager || '--'}`,
    }
  }))

  const homeActionQueue = computed(() => {
    const projectTasks = viewProjects.value.slice(0, 12).map((item) => {
      const todo = resolveProjectTodo(item)
      return {
        title: item.name,
        desc: todo ? `${todo.stage}：${todo.nextAction}` : `${item.stage || '项目'}待推进`,
        target: { view: 'project-tracker', projectId: item.id },
      }
    })
    return [
      ...projectTasks,
      { title: '资料关联整理', desc: '把项目交付资料沉淀进知识库', target: 'library' },
    ]
  })

  const customerFollowupAlerts = computed(() => {
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const sevenDaysLater = new Date(today)
    sevenDaysLater.setDate(sevenDaysLater.getDate() + 7)

    const records = []
    for (const customer of (customers.value || [])) {
      const followups = customer.followups || []
      const fallbackDate = customer.nextFollowupDate || null
      const candidateDates = [
        ...followups.map((item) => item?.nextFollowupDate).filter(Boolean),
        fallbackDate,
      ].filter(Boolean)
      if (!candidateDates.length) continue
      const nearest = candidateDates
        .map((date) => new Date(date))
        .filter((date) => !Number.isNaN(date.getTime()))
        .sort((a, b) => a.getTime() - b.getTime())[0]
      if (!nearest) continue
      if (nearest > sevenDaysLater) continue
      const diffDays = Math.floor((nearest.getTime() - today.getTime()) / (24 * 60 * 60 * 1000))
      records.push({
        customerId: customer.id,
        customerName: customer.name,
        ownerName: customer.ownerName || '--',
        date: `${nearest.getFullYear()}-${String(nearest.getMonth() + 1).padStart(2, '0')}-${String(nearest.getDate()).padStart(2, '0')}`,
        diffDays,
        status: diffDays < 0 ? '已逾期' : diffDays === 0 ? '今天到期' : `${diffDays}天后到期`,
      })
    }

    return records.sort((a, b) => a.diffDays - b.diffDays).slice(0, 8)
  })

  const homeQuickStats = computed(() => [
    { label: '进行中项目', value: `${projectDashboard.value.activeProjects || 0} 个`, hint: '当前持续推进中的项目' },
    { label: '累计合同额', value: `¥${formatMoney(projectDashboard.value.totalContractAmount || 0)}`, hint: '已录入项目合同额' },
    { label: '累计回款', value: `¥${formatMoney(projectDashboard.value.totalReceivedAmount || 0)}`, hint: '项目累计回款金额' },
    { label: '资料总数', value: `${items.value.length} 条`, hint: `${currentCategoryName.value} / 知识库沉淀内容规模` },
  ])

  const relatedLibraryItems = computed(() => items.value.filter((item) => item.projectId === selectedProject.value?.id).slice(0, 8))

  return {
    currentCategoryName,
    projectTrackerSummary,
    projectMilestones,
    projectRecordFeed,
    homeProjectFocus,
    homeActionQueue,
    homeQuickStats,
    relatedLibraryItems,
    customerFollowupAlerts,
  }
}
