<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useProjectTrackerTable } from '../composables/useProjectTrackerTable'
import {
  projectDueStateOptions,
  projectRiskOptions as projectRiskOptionsFallback,
  projectStageOptions as projectStageOptionsFallback,
  projectStatusOptions as projectStatusOptionsFallback,
} from '../composables/projectDialogOptions'

const props = defineProps({
  projectFilters: { type: Object, required: true },
  viewProjects: { type: Array, required: true },
  projectsLoading: { type: Boolean, default: false },
  selectedProjectId: { type: [Number, String], default: null },
  customers: { type: Array, default: () => [] },
  users: { type: Array, default: () => [] },
  dictionaryOptions: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['reset-filters', 'filter', 'select-project', 'open-progress', 'open-edit', 'delete-project'])
const { getDueStatus, matchDueState } = useProjectTrackerTable()

const TABLE_DENSITY_KEY = 'kp.projectTracker.compactTable'
const compactTable = ref(localStorage.getItem(TABLE_DENSITY_KEY) === 'true')
const listContentRef = ref(null)
const tableBodyHeight = ref(320)
let filterDebounceTimer = null

function recalcTableBodyHeight() {
  const container = listContentRef.value
  if (!container) return
  const rect = container.getBoundingClientRect()
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 900
  const bottomGap = 20
  tableBodyHeight.value = Math.max(220, Math.floor(viewportHeight - rect.top - bottomGap))
}

function handleWindowResize() {
  recalcTableBodyHeight()
}

const customerFilter = computed(() => String(props.projectFilters.customerName || '').trim())

const customerOptions = computed(() => {
  const source = props.customers.length
    ? props.customers.map((item) => String(item?.name || '').trim()).filter(Boolean)
    : props.viewProjects.map((item) => String(item?.customerName || '').trim()).filter(Boolean)
  return [...new Set(source)].sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
})

const ownerOptions = computed(() => {
  const userNames = props.users
    .filter((user) => user && user.enabled !== false)
    .map((user) => String(user.displayName || user.username || '').trim())
    .filter(Boolean)

  const fallbackNames = props.viewProjects
    .flatMap((item) => [item.projectManager, item.salesOwner, item.implementationOwner, item.documentOwner, item.serviceOwner])
    .map((name) => String(name || '').trim())
    .filter(Boolean)

  const source = userNames.length ? userNames : fallbackNames
  return [...new Set(source)].sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
})

const filteredProjects = computed(() => props.viewProjects.filter((item) => {
  if (customerFilter.value && String(item.customerName || '').trim() !== customerFilter.value) return false
  return matchDueState(item, props.projectFilters.dueState)
}))
const dueStatusMap = computed(() => new Map(filteredProjects.value.map((item) => [item.id, getDueStatus(item)])))

watch(compactTable, async (value) => {
  localStorage.setItem(TABLE_DENSITY_KEY, String(value))
  await nextTick()
  recalcTableBodyHeight()
})

watch(
  () => ({ ...props.projectFilters }),
  () => {
    if (filterDebounceTimer) clearTimeout(filterDebounceTimer)
    filterDebounceTimer = setTimeout(() => emit('filter'), 180)
  },
  { deep: true },
)

onMounted(async () => {
  await nextTick()
  recalcTableBodyHeight()
  window.addEventListener('resize', handleWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleWindowResize)
})

const stageOptions = computed(() => props.dictionaryOptions?.projectStage?.length ? props.dictionaryOptions.projectStage : projectStageOptionsFallback)
const statusOptions = computed(() => props.dictionaryOptions?.projectStatus?.length ? props.dictionaryOptions.projectStatus : projectStatusOptionsFallback)
const riskOptions = computed(() => props.dictionaryOptions?.projectRisk?.length ? props.dictionaryOptions.projectRisk : projectRiskOptionsFallback)

const dueStateLabelMap = Object.fromEntries(projectDueStateOptions.map((item) => [item.value, item.label]))
const stageLabelMap = computed(() => Object.fromEntries(stageOptions.value.map((item) => [item.value, item.label])))
const statusLabelMap = computed(() => Object.fromEntries(statusOptions.value.map((item) => [item.value, item.label])))
const riskLabelMap = computed(() => Object.fromEntries(riskOptions.value.map((item) => [item.value, item.label])))

function resolveDueStatus(row) {
  return dueStatusMap.value.get(row.id) || getDueStatus(row)
}

const filterSummary = computed(() => {
  const chips = []
  if (props.projectFilters.keyword) chips.push(`关键词：${props.projectFilters.keyword}`)
  if (props.projectFilters.customerName) chips.push(`客户：${props.projectFilters.customerName}`)
  if (props.projectFilters.stage) chips.push(`阶段：${stageLabelMap.value[props.projectFilters.stage] || props.projectFilters.stage}`)
  if (props.projectFilters.status) chips.push(`状态：${statusLabelMap.value[props.projectFilters.status] || props.projectFilters.status}`)
  if (props.projectFilters.owner) chips.push(`负责人：${props.projectFilters.owner}`)
  if (props.projectFilters.riskLevel) chips.push(`风险：${riskLabelMap.value[props.projectFilters.riskLevel] || props.projectFilters.riskLevel}`)
  if (props.projectFilters.dueState) chips.push(`到期：${dueStateLabelMap[props.projectFilters.dueState] || props.projectFilters.dueState}`)
  return chips.length ? chips.join(' · ') : '当前无筛选条件'
})

const filterSummaryWithCount = computed(() => `${filterSummary.value}（${filteredProjects.value.length}/${props.viewProjects.length}）`)

const overdueCount = computed(() => filteredProjects.value.filter((item) => resolveDueStatus(item).type === 'danger').length)
const dueSoonCount = computed(() => filteredProjects.value.filter((item) => resolveDueStatus(item).type === 'warning').length)
const highRiskCount = computed(() => filteredProjects.value.filter((item) => String(item.riskLevel || '') === '高').length)

function resetFilters() {
  emit('reset-filters')
}
</script>

<template>
  <el-card shadow="never" class="panel-card project-tracker-list-card">
    <div class="project-filter-summary-bar" style="align-items: flex-start; gap: 10px; flex: 0 0 auto;">
      <div style="display:flex; flex-direction:column; gap:6px;">
        <span class="project-filter-summary-text">{{ filterSummaryWithCount }}</span>
        <el-space>
          <el-tag size="small" type="danger">逾期 {{ overdueCount }}</el-tag>
          <el-tag size="small" type="warning">7天内 {{ dueSoonCount }}</el-tag>
          <el-tag size="small" type="info">高风险 {{ highRiskCount }}</el-tag>
        </el-space>
      </div>
      <el-space>
        <el-switch v-model="compactTable" inline-prompt active-text="卡片" inactive-text="标准" />
        <el-button @click="resetFilters">重置筛选</el-button>
      </el-space>
    </div>

    <div class="project-filter-inline" style="margin-top: 10px; display: grid; grid-template-columns: 1.2fr repeat(6, minmax(120px, 1fr)); gap: 8px; flex: 0 0 auto;">
      <el-input v-model="projectFilters.keyword" clearable placeholder="关键词（项目名/客户名）" @keyup.enter="emit('filter')" />
      <el-select v-model="projectFilters.customerName" clearable filterable placeholder="客户名称">
        <el-option v-for="name in customerOptions" :key="`customer-${name}`" :label="name" :value="name" />
      </el-select>
      <el-select v-model="projectFilters.stage" clearable placeholder="阶段">
        <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="projectFilters.status" clearable placeholder="状态">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="projectFilters.owner" clearable filterable placeholder="负责人">
        <el-option v-for="name in ownerOptions" :key="`owner-${name}`" :label="name" :value="name" />
      </el-select>
      <el-select v-model="projectFilters.riskLevel" clearable placeholder="风险">
        <el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="projectFilters.dueState" clearable placeholder="到期">
        <el-option v-for="item in projectDueStateOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>

    <div ref="listContentRef" class="project-tracker-list-content">
      <el-table
        v-if="!compactTable"
        class="project-tracker-list-table"
        :data="filteredProjects"
        :height="tableBodyHeight"
        table-layout="fixed"
        stripe
        border
        highlight-current-row
        row-key="id"
        :current-row-key="selectedProjectId"
        empty-text="暂无匹配项目"
        @row-click="emit('select-project', $event)"
        @row-dblclick="emit('open-progress', $event)"
        @current-change="emit('select-project', $event)"
        v-loading="projectsLoading"
      >
          <el-table-column label="项目" min-width="170" sortable>
            <template #default="scope">
              <div class="project-table-name-cell">
                <strong class="project-table-name-text">{{ scope.row.name || '--' }}</strong>
                <small class="project-table-sub-text">{{ scope.row.customerName || '--' }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="阶段/状态" width="150" sortable>
            <template #default="scope">
              <div class="project-table-stage-cell">
                <span class="project-table-stage-text">{{ scope.row.stage || '--' }}</span>
                <small class="project-table-sub-text">{{ scope.row.status || '--' }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="跟进" width="100">
            <template #default="scope">
              <el-tag size="small" :type="resolveDueStatus(scope.row).type">{{ resolveDueStatus(scope.row).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="130" sortable>
            <template #default="scope">
              <el-progress :percentage="scope.row.progress || 0" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230">
            <template #default="scope">
              <el-space>
                <el-button link type="primary" @click.stop="emit('open-progress', scope.row)">推进</el-button>
                <el-button link @click.stop="emit('open-edit', scope.row)">编辑</el-button>
                <el-button link type="danger" @click.stop="emit('delete-project', scope.row)">删除</el-button>
              </el-space>
            </template>
          </el-table-column>
      </el-table>

      <div
        v-else
        class="project-card-grid project-card-grid-scroll"
        :style="{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '10px', maxHeight: `${tableBodyHeight}px` }"
      >
        <el-card
          v-for="item in filteredProjects"
          :key="item.id"
          shadow="never"
          :class="['project-mini-card', { 'project-mini-card-active': String(item.id) === String(selectedProjectId ?? '') }]"
          @click="emit('select-project', item)"
        >
          <div style="display: flex; justify-content: space-between; gap: 8px; align-items: center;">
            <strong style="font-size: 14px;">{{ item.name }}</strong>
            <el-tag size="small" :type="resolveDueStatus(item).type">{{ resolveDueStatus(item).label }}</el-tag>
          </div>
          <div style="margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px;">
            {{ item.customerName || '--' }} · {{ item.stage || '--' }} · {{ item.status || '--' }}
          </div>
          <div style="margin-top: 8px;">
            <el-progress :percentage="item.progress || 0" :stroke-width="6" />
          </div>
          <div style="margin-top: 8px; display: flex; justify-content: space-between; align-items: center;">
            <span style="font-size: 12px; color: var(--el-text-color-secondary);">负责人：{{ item.projectManager || item.salesOwner || item.implementationOwner || '--' }}</span>
            <el-space>
              <el-button link type="primary" size="small" @click.stop="emit('open-progress', item)">推进</el-button>
              <el-button link size="small" @click.stop="emit('open-edit', item)">编辑</el-button>
              <el-button link type="danger" size="small" @click.stop="emit('delete-project', item)">删除</el-button>
            </el-space>
          </div>
        </el-card>
        <el-empty v-if="!filteredProjects.length" description="暂无匹配项目" />
      </div>
    </div>
  </el-card>
</template>
