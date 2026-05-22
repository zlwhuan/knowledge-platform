<script setup>
const props = defineProps({
  trackerSubView: { type: String, required: true },
  projectDashboard: { type: Object, required: true },
  upcomingActionItems: { type: Array, required: true },
  viewProjects: { type: Array, required: true },
  formatMoney: { type: Function, required: true },
})

const emit = defineEmits(['open-today-actions'])

function resolveFocusCards() {
  const progressItems = props.upcomingActionItems.filter((item) => item.sourceType === 'progress')
  const baseItems = progressItems.length ? progressItems : props.upcomingActionItems
  const overdueCount = baseItems.filter((item) => item.overdue).length
  const dueSoonCount = baseItems.filter((item) => !item.overdue).length
  const todayTodoCount = baseItems.filter((item) => Number(item.diffDays) === 0).length
  if (props.trackerSubView === 'finance') {
    const receivable = Math.max(Number(props.projectDashboard.totalContractAmount || 0) - Number(props.projectDashboard.totalReceivedAmount || 0), 0)
    return [
      { label: '合同总额', value: `¥${props.formatMoney(props.projectDashboard.totalContractAmount)}`, hint: `项目 ${props.viewProjects.length} 个` },
      { label: '累计回款', value: `¥${props.formatMoney(props.projectDashboard.totalReceivedAmount)}`, hint: `待回款 ${props.projectDashboard.paymentPendingProjects || 0} 个` },
      { label: '未收金额', value: `¥${props.formatMoney(receivable)}`, hint: overdueCount ? `逾期跟进 ${overdueCount} 项` : '暂无逾期跟进' },
    ]
  }
  if (props.trackerSubView === 'sales') {
    return [
      { label: '商机/合同池', value: `${props.viewProjects.length} 个`, hint: `高风险 ${props.projectDashboard.highRiskProjects || 0} 个` },
      { label: '近7天待办', value: `${dueSoonCount} 项`, hint: overdueCount ? `其中逾期 ${overdueCount} 项` : '暂无逾期待办', badge: todayTodoCount },
      { label: '待签约/执行', value: `${props.projectDashboard.activeProjects || 0} 个`, hint: '聚焦签约与推进节奏' },
    ]
  }
  if (props.trackerSubView === 'presales') {
    return [
      { label: '售前协同项目', value: `${props.viewProjects.length} 个`, hint: '围绕商机与合同前置支撑' },
      { label: '近7天支撑待办', value: `${dueSoonCount} 项`, hint: overdueCount ? `逾期 ${overdueCount} 项` : '支撑节奏正常', badge: todayTodoCount },
      { label: '需优先支撑', value: `${props.viewProjects.filter((item) => item.riskLevel === '高').length} 个`, hint: '高风险项目优先投入方案资源' },
    ]
  }
  if (props.trackerSubView === 'deliveryOps') {
    return [
      { label: '交付项目', value: `${props.viewProjects.length} 个`, hint: `待验收 ${props.projectDashboard.acceptancePendingProjects || 0} 个` },
      { label: '近7天验收/实施', value: `${dueSoonCount} 项`, hint: overdueCount ? `逾期 ${overdueCount} 项` : '交付节奏正常', badge: todayTodoCount },
      { label: '高风险交付', value: `${props.viewProjects.filter((item) => item.riskLevel === '高').length} 个`, hint: '建议优先盯办阻塞项' },
    ]
  }
  return [
    { label: '项目池', value: `${props.viewProjects.length} 个`, hint: `进行中 ${props.projectDashboard.activeProjects || 0} 个` },
    { label: '逾期待办', value: `${overdueCount} 项`, hint: dueSoonCount ? `另有近7天待办 ${dueSoonCount} 项` : '暂无近期待办', badge: todayTodoCount },
    { label: '高风险项目', value: `${props.projectDashboard.highRiskProjects || 0} 个`, hint: '适合重点盯办' },
  ]
}
</script>

<template>
  <div class="project-focus-bar">
    <div v-for="item in resolveFocusCards()" :key="item.label" class="workflow-card project-focus-card">
      <el-badge v-if="item.badge" :value="item.badge" type="danger" :max="99">
        <button type="button" class="focus-card-label-btn" @click="emit('open-today-actions')">{{ item.label }}</button>
      </el-badge>
      <span v-else>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
      <small>{{ item.hint }}</small>
    </div>
  </div>
</template>
