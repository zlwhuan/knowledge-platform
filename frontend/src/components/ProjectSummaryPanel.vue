<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  selectedProject: { type: Object, default: null },
  upcomingActionItems: { type: Array, required: true },
})

const emit = defineEmits(['open-progress', 'open-edit', 'delete-progress'])

function formatDateTime(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

function handleResize() {
  recalcTodoScrollMode()
}

const selectedProgress = computed(() => props.selectedProject?.progressRecords || [])
const ALL_STAGE = '__ALL__'
const progressStageFilter = ref(ALL_STAGE)
const progressStageOptions = computed(() => [
  { label: '全部', value: ALL_STAGE },
  ...Array.from(new Set(selectedProgress.value.map((item) => String(item.stage || '').trim()).filter(Boolean))).map((stage) => ({
    label: stage,
    value: stage,
  })),
])
const filteredSelectedProgress = computed(() => {
  if (progressStageFilter.value === ALL_STAGE) return selectedProgress.value
  return selectedProgress.value.filter((item) => String(item.stage || '').trim() === progressStageFilter.value)
})
const projectDueActions = computed(() => {
  if (!props.selectedProject?.id) return []
  return props.upcomingActionItems.filter((item) => item.projectId === props.selectedProject.id)
})

const projectProgressTodos = computed(() => projectDueActions.value)

const todoScrollRef = ref(null)
const todoNeedsScroll = ref(false)

function recalcTodoScrollMode() {
  const element = todoScrollRef.value
  if (!element) {
    todoNeedsScroll.value = false
    return
  }
  const panelElement = element.closest('.project-todo-panel')
  const stackElement = element.closest('.project-right-stack')
  if (!panelElement || !stackElement) {
    todoNeedsScroll.value = false
    return
  }

  const headerElement = panelElement.querySelector('.el-card__header')
  const bodyElement = panelElement.querySelector('.el-card__body')
  const headerHeight = headerElement ? headerElement.getBoundingClientRect().height : 0
  const bodyStyle = bodyElement ? window.getComputedStyle(bodyElement) : null
  const bodyPaddingTop = bodyStyle ? Number.parseFloat(bodyStyle.paddingTop || '0') : 0
  const bodyPaddingBottom = bodyStyle ? Number.parseFloat(bodyStyle.paddingBottom || '0') : 0

  const isNoProgress = stackElement.classList.contains('no-progress')
  const stackHeight = stackElement.getBoundingClientRect().height
  const panelMaxHeight = isNoProgress ? stackHeight : stackHeight * 0.5
  const availableContentHeight = Math.max(0, panelMaxHeight - headerHeight - bodyPaddingTop - bodyPaddingBottom)

  todoNeedsScroll.value = element.scrollHeight - availableContentHeight > 1
}

const todoScrollClass = computed(() => (todoNeedsScroll.value ? 'todo-scroll-auto' : 'todo-scroll-natural'))
const todoPanelClass = computed(() => (todoNeedsScroll.value ? 'todo-panel-scroll' : 'todo-panel-natural'))

watch(projectDueActions, async () => {
  await nextTick()
  recalcTodoScrollMode()
}, { deep: true })

watch(() => props.selectedProject?.id, async () => {
  progressStageFilter.value = ALL_STAGE
  await nextTick()
  recalcTodoScrollMode()
})

onMounted(async () => {
  await nextTick()
  recalcTodoScrollMode()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="project-right-stack" :class="{ 'no-progress': !selectedProgress.length }">
    <el-card shadow="never" :class="['panel-card', 'compact-dashboard-card', 'project-workbench-panel', 'project-todo-panel', todoPanelClass]">
      <template #header>
        <div class="card-header-block" style="display:flex;justify-content:space-between;align-items:center;">
          <h3>待办工作</h3>
          <span style="font-size: 12px; color: #5f6f84;">共 {{ projectProgressTodos.length }} 项</span>
        </div>
      </template>

      <div ref="todoScrollRef" :class="['project-workbench-scroll', 'project-record-feed', 'compact-record-feed', 'project-right-compact-scroll', todoScrollClass]">
        <template v-for="item in projectDueActions" :key="`${item.projectId}-${item.sourceType}-${item.type}-${item.date}`">
          <div class="project-record-item compact-timeline-item project-right-compact-item project-right-item-row">
            <div class="project-right-item-main">
              <div class="project-record-head compact-head">
                <strong>{{ item.type }}</strong>
                <el-tag size="small" :type="item.overdue ? 'danger' : 'warning'">{{ item.overdue ? `逾期 ${Math.abs(item.diffDays)} 天` : `${item.diffDays} 天内` }}</el-tag>
              </div>
              <p class="compact-text-one">{{ item.date }} · {{ item.nextAction || '--' }}</p>
            </div>
            <div class="project-right-item-actions single-action">
              <el-button
                link
                type="primary"
                size="small"
                @click="emit('open-progress', { row: selectedProject, todo: item })"
              >更新进度</el-button>
            </div>
          </div>
        </template>
      </div>
    </el-card>

    <el-card v-if="selectedProgress.length" shadow="never" class="panel-card compact-dashboard-card project-workbench-panel project-progress-panel">
      <template #header>
        <div class="card-header-block" style="display:flex;justify-content:space-between;align-items:center;gap:8px;">
          <h3>项目进度</h3>
          <el-space>
            <el-radio-group v-model="progressStageFilter" size="small" class="progress-stage-switch">
              <el-radio-button v-for="item in progressStageOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
            </el-radio-group>
            <el-button type="primary" size="small" :disabled="!selectedProject" @click="emit('open-progress', { row: selectedProject })">新增进度</el-button>
          </el-space>
        </div>
      </template>

      <div class="project-workbench-scroll project-record-feed compact-record-feed project-right-compact-scroll">
        <div v-for="item in filteredSelectedProgress" :key="`progress-${item.id}`" class="project-record-item compact-timeline-item project-right-compact-item project-right-item-row">
          <div class="project-right-item-main">
            <div class="project-record-head compact-head progress-head-fixed">
              <div class="progress-head-main">
                <strong class="progress-stage">{{ item.stage || '--' }}</strong>
                <span class="progress-status">{{ item.status || '--' }} · {{ item.progress ?? 0 }}%</span>
              </div>
              <small>{{ formatDateTime(item.recordTime || item.createdAt) }}</small>
            </div>
            <p class="compact-text-one">{{ item.summary || '--' }}</p>
          </div>
          <div class="project-right-item-actions split-actions">
            <el-button link type="primary" @click="emit('open-progress', { row: selectedProject, record: item })">编辑</el-button>
            <el-button link type="danger" @click="emit('delete-progress', item)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!filteredSelectedProgress.length" description="当前阶段暂无进度记录" :image-size="56" />
      </div>
    </el-card>
  </div>
</template>

