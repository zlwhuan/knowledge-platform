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
  // 笔记本/堆叠布局：面板高度由内容决定，统一交给 CSS 滚动，避免 0 高度误判成「不需要滚动」
  if (typeof window !== 'undefined' && window.innerWidth <= 1919) {
    todoNeedsScroll.value = true
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
        <div
          v-for="item in projectDueActions"
          :key="`${item.projectId}-${item.sourceType}-${item.type}-${item.date}`"
          class="side-card"
        >
          <div class="side-card-top">
            <div class="side-card-titles">
              <strong class="side-card-title">{{ item.type }}</strong>
              <el-tag size="small" :type="item.overdue ? 'danger' : 'warning'">
                {{ item.overdue ? `逾期 ${Math.abs(item.diffDays)} 天` : `${item.diffDays} 天内` }}
              </el-tag>
            </div>
            <div class="side-card-actions">
              <el-button
                link
                type="primary"
                size="small"
                @click="emit('open-progress', { row: selectedProject, todo: item })"
              >更新进度</el-button>
            </div>
          </div>
          <div class="side-card-meta">{{ item.date }}</div>
          <div class="side-card-body">{{ item.nextAction || '--' }}</div>
        </div>
        <el-empty v-if="!projectDueActions.length" description="暂无待办" :image-size="48" />
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
        <div
          v-for="item in filteredSelectedProgress"
          :key="`progress-${item.id}`"
          class="side-card"
        >
          <div class="side-card-top">
            <div class="side-card-titles">
              <strong class="side-card-title">{{ item.stage || '--' }}</strong>
              <span class="side-card-sub">{{ item.status || '--' }} · {{ item.progress ?? 0 }}%</span>
            </div>
            <div class="side-card-actions">
              <el-button link type="primary" size="small" @click="emit('open-progress', { row: selectedProject, record: item })">编辑</el-button>
              <el-button link type="danger" size="small" @click="emit('delete-progress', item)">删除</el-button>
            </div>
          </div>
          <div class="side-card-meta">{{ formatDateTime(item.recordTime || item.createdAt) }}</div>
          <div class="side-card-body">{{ item.summary || '--' }}</div>
        </div>
        <el-empty v-if="!filteredSelectedProgress.length" description="当前阶段暂无进度记录" :image-size="48" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.side-card {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  margin: 0 0 8px;
  padding: 8px 12px 8px 10px;
  border: 1px solid #e3ebf5;
  border-radius: 8px;
  background: #fbfcfe;
  overflow: visible;
}

.side-card-top {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 6px 8px;
  max-width: 100%;
  overflow: visible;
}

.side-card-titles {
  min-width: 0;
  flex: 1 1 auto;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  overflow: visible;
}

.side-card-title {
  min-width: 0;
  color: #0f2f5c;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.35;
  word-break: break-word;
}

.side-card-sub {
  min-width: 0;
  color: #5f6f84;
  font-size: 12px;
}

.side-card-actions {
  flex: 0 0 auto;
  min-width: max-content;
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
  overflow: visible;
  margin-left: auto;
}

.side-card-actions .el-button {
  margin: 0;
  padding: 0 4px;
  white-space: nowrap;
}

.side-card-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #8a98ab;
  line-height: 1.35;
  word-break: break-all;
}

.side-card-body {
  margin-top: 4px;
  font-size: 12px;
  color: #334861;
  line-height: 1.45;
  word-break: break-word;
  max-width: 100%;
  overflow: hidden;
}
</style>

