<script setup>
import { computed, ref } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  projects: { type: Array, default: () => [] },
})

const currentWeekStart = ref(dayjs().startOf('week').add(1, 'day'))

const weekLabel = computed(() => {
  const start = currentWeekStart.value
  const end = start.add(6, 'day')
  return `${start.format('YYYY-MM-DD')} ~ ${end.format('YYYY-MM-DD')}`
})

function prevWeek() {
  currentWeekStart.value = currentWeekStart.value.subtract(7, 'day')
}

function nextWeek() {
  currentWeekStart.value = currentWeekStart.value.add(7, 'day')
}

function goToThisWeek() {
  currentWeekStart.value = dayjs().startOf('week').add(1, 'day')
}

const weekDays = computed(() => {
  const days = []
  for (let i = 0; i < 7; i++) {
    const d = currentWeekStart.value.add(i, 'day')
    days.push({
      label: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][i],
      date: d.format('MM-DD'),
      full: d.format('YYYY-MM-DD'),
    })
  }
  return days
})

const weeklyRecords = computed(() => {
  const start = currentWeekStart.value.startOf('day')
  const end = currentWeekStart.value.add(6, 'day').endOf('day')
  const result = []
  for (const project of props.projects) {
    for (const record of project.progressRecords || []) {
      const t = dayjs(record.recordTime || record.createdAt)
      if (t.isBefore(start) || t.isAfter(end)) continue
      result.push({
        ...record,
        projectId: project.id,
        projectName: project.name,
        customerName: project.customerName,
        stage: project.stage,
        projectProgress: project.progress,
        riskLevel: project.riskLevel,
        projectManager: project.projectManager,
        weekDay: t.day() === 0 ? 6 : t.day() - 1,
        recordTimeFormatted: t.format('MM-DD HH:mm'),
      })
    }
  }
  result.sort((a, b) => new Date(b.recordTime || 0) - new Date(a.recordTime || 0))
  return result
})

const recordsByDay = computed(() => {
  const map = {}
  for (let i = 0; i < 7; i++) map[i] = []
  for (const r of weeklyRecords.value) {
    map[r.weekDay].push(r)
  }
  return map
})

const totalRecords = computed(() => weeklyRecords.value.length)
const projectsInvolved = computed(() => new Set(weeklyRecords.value.map(r => r.projectId)).size)
const highRiskCount = computed(() => weeklyRecords.value.filter(r => r.riskLevel === '高').length)

const statusColor = (status) => {
  const map = { '进行中': '', '已暂停': 'warning', '已完成': 'success', '已取消': 'info' }
  return map[status] || ''
}
const riskColor = (level) => {
  const map = { '高': 'danger', '中': 'warning', '低': 'success' }
  return map[level] || 'info'
}
const stageColor = (stage) => {
  const map = { '商机立项': 'info', '合同执行': '', '实施交付': 'warning', '验收结算': 'success', '售后维保': '' }
  return map[stage] || ''
}
</script>

<template>
  <div class="weekly-progress-view">
    <div class="weekly-toolbar">
      <el-button-group>
        <el-button @click="prevWeek">&lt;</el-button>
        <el-button @click="goToThisWeek">本周</el-button>
        <el-button @click="nextWeek">&gt;</el-button>
      </el-button-group>
      <span class="week-label">{{ weekLabel }}</span>
      <div class="week-stats">
        <el-tag type="info">进度记录 {{ totalRecords }}</el-tag>
        <el-tag>涉及项目 {{ projectsInvolved }}</el-tag>
        <el-tag v-if="highRiskCount > 0" type="danger">高风险 {{ highRiskCount }}</el-tag>
      </div>
    </div>

    <div v-if="weeklyRecords.length === 0" class="empty-state">
      <el-empty description="本周暂无进度记录" />
    </div>

    <div v-else class="week-grid">
      <div v-for="(day, idx) in weekDays" :key="day.full" class="day-column">
        <div class="day-header" :class="{ 'day-today': day.full === dayjs().format('YYYY-MM-DD') }">
          <span class="day-label">{{ day.label }}</span>
          <span class="day-date">{{ day.date }}</span>
          <el-badge v-if="recordsByDay[idx].length" :value="recordsByDay[idx].length" class="day-badge" />
        </div>
        <div class="day-records">
          <div v-if="recordsByDay[idx].length === 0" class="day-empty">--</div>
          <div v-for="record in recordsByDay[idx]" :key="record.id" class="record-card">
            <div class="record-header">
              <span class="record-project" :title="record.projectName">{{ record.projectName }}</span>
              <span class="record-time">{{ record.recordTimeFormatted }}</span>
            </div>
            <div class="record-meta">
              <el-tag size="small" :type="stageColor(record.stage)">{{ record.stage }}</el-tag>
              <el-tag v-if="record.riskLevel" size="small" :type="riskColor(record.riskLevel)">风险: {{ record.riskLevel }}</el-tag>
            </div>
            <div class="record-summary">{{ record.summary || '--' }}</div>
            <div v-if="record.nextAction" class="record-action">
              <span class="action-label">下一步:</span> {{ record.nextAction }}
              <span v-if="record.nextActionDueDate" class="action-date">{{ record.nextActionDueDate }}</span>
            </div>
            <div class="record-footer">
              <span class="record-owner" v-if="record.ownerName">{{ record.ownerName }}</span>
              <span class="record-progress">进度 {{ record.progress || 0 }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.weekly-progress-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.weekly-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.week-label {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}
.week-stats {
  display: flex;
  gap: 6px;
  margin-left: auto;
}
.empty-state {
  padding: 40px 0;
}
.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  min-height: 300px;
}
.day-column {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.day-header {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 4px;
  border-bottom: 2px solid #e4e7ed;
  font-size: 12px;
  color: #606266;
}
.day-header.day-today {
  border-bottom-color: #409eff;
  color: #409eff;
  font-weight: 600;
}
.day-label {
  font-weight: 600;
}
.day-date {
  color: #909399;
  font-size: 11px;
}
.day-records {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 4px 0;
  flex: 1;
}
.day-empty {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  padding: 20px 0;
}
.record-card {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 8px;
  font-size: 12px;
  border: 1px solid #ebeef5;
}
.record-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 4px;
  margin-bottom: 4px;
}
.record-project {
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.record-time {
  color: #909399;
  font-size: 11px;
  white-space: nowrap;
}
.record-meta {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.record-summary {
  color: #606266;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.record-action {
  margin-top: 4px;
  color: #909399;
  font-size: 11px;
}
.action-label {
  font-weight: 600;
  color: #e6a23c;
}
.action-date {
  margin-left: 4px;
  color: #409eff;
}
.record-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  color: #909399;
  font-size: 11px;
}
.record-owner {
  font-weight: 500;
}
.record-progress {
  font-weight: 600;
  color: #409eff;
}
</style>
