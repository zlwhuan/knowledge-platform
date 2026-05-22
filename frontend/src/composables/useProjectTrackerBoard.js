import { computed } from 'vue'

const riskWeight = { 高: 3, 中: 2, 低: 1 }

function formatDateTime(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

export function useProjectTrackerBoard(props) {
  const pendingActions = computed(() => props.viewProjects
    .filter((item) => item.latestProgressSummary)
    .sort((left, right) => {
      const riskGap = (riskWeight[right.riskLevel] || 0) - (riskWeight[left.riskLevel] || 0)
      if (riskGap !== 0) return riskGap
      return (left.progress || 0) - (right.progress || 0)
    })
    .slice(0, 6))

  const selectedActivities = computed(() => props.selectedProject?.activities || [])
  const selectedProgressRecords = computed(() => props.selectedProject?.progressRecords || [])
  const roleTodoEntries = computed(() => Object.entries(props.projectDashboard?.roleTodoCounts || {}).filter(([, value]) => Number(value || 0) > 0))
  const stageEntries = computed(() => Object.entries(props.projectDashboard?.stageCounts || {}).filter(([, value]) => Number(value || 0) > 0))
  const statusEntries = computed(() => Object.entries(props.projectDashboard?.statusCounts || {}).filter(([, value]) => Number(value || 0) > 0))
  const focusWarnings = computed(() => {
    const warnings = []
    const overdueItems = props.upcomingActionItems.filter((item) => item.overdue)
    if (overdueItems.length) warnings.push(`当前有 ${overdueItems.length} 项已逾期`) 
    if ((props.projectDashboard?.highRiskProjects || 0) > 0) warnings.push(`高风险项目 ${props.projectDashboard.highRiskProjects} 个`) 
    if ((props.projectDashboard?.paymentPendingProjects || 0) > 0 && props.trackerSubView === 'finance') warnings.push(`待回款项目 ${props.projectDashboard.paymentPendingProjects} 个`)
    if ((props.projectDashboard?.acceptancePendingProjects || 0) > 0 && props.trackerSubView === 'delivery') warnings.push(`待验收项目 ${props.projectDashboard.acceptancePendingProjects} 个`)
    return warnings.slice(0, 3)
  })

  return {
    pendingActions,
    selectedActivities,
    selectedProgressRecords,
    roleTodoEntries,
    stageEntries,
    statusEntries,
    focusWarnings,
    formatDateTime,
  }
}
