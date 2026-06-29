<script setup>
import { computed, getCurrentInstance } from 'vue'
import ProjectTrackerOverviewChart from './ProjectTrackerOverviewChart.vue'
import ProjectTrackerTable from './ProjectTrackerTable.vue'
import ProjectSummaryPanel from './ProjectSummaryPanel.vue'
import ProjectDialogs from './ProjectDialogs.vue'
import ProjectWeeklyProgressView from './ProjectWeeklyProgressView.vue'
import { useProjectTrackerView } from '../composables/useProjectTrackerView'

defineProps({
  trackerSubMenus: { type: Array, required: true },
  trackerSubView: { type: String, required: true },
  subViewTitle: { type: String, required: true },
  subViewDescription: { type: String, required: true },
  projectFilters: { type: Object, required: true },
  viewProjects: { type: Array, required: true },
  projectsLoading: { type: Boolean, default: false },
  selectedProject: { type: Object, default: null },
  upcomingActionItems: { type: Array, required: true },
  dialogs: { type: Object, required: true },
  saving: { type: Object, required: true },
  forms: { type: Object, required: true },
  users: { type: Array, default: () => [] },
  customers: { type: Array, default: () => [] },
  currentUserName: { type: String, default: '' },
  dictionaryOptions: { type: Object, default: () => ({}) },
})

const emit = defineEmits([
  'open-progress',
  'open-edit',
  'select-project',
  'reset-filters',
  'filter',
  'delete-project',
  'delete-progress',
  'close-project-form',
  'save-project',
  'close-project-progress',
  'save-project-progress',
])

const instance = getCurrentInstance()

const relatedContactNames = computed(() => {
  const selectedProject = instance?.props?.selectedProject
  if (!selectedProject) return []
  try {
    const links = JSON.parse(selectedProject.projectContactLinks || '[]')
    if (!Array.isArray(links)) return []
    return links.map((item) => String(item?.contactName || '').trim()).filter(Boolean)
  } catch {
    return []
  }
})

const {
  onOpenProgress,
  onOpenEdit,
  onSelectProject,
  onResetFilters,
  onFilter,
  onDeleteProject,
  onDeleteProgress,
  onCloseProjectForm,
  onSaveProject,
  onCloseProjectProgress,
  onSaveProjectProgress,
} = useProjectTrackerView(emit)
</script>

<template>
  <section class="page-section project-tracker-page project-tracker-redesign">
    <el-card shadow="never" class="panel-card project-summary-card">
      <template #header>
        <div class="project-summary-header">
          <div class="project-summary-header-left">
            <h3>{{ subViewTitle }}</h3>
            <p v-if="subViewDescription">{{ subViewDescription }}</p>
          </div>

          <div class="project-summary-header-middle project-summary-meta">
            <template v-if="selectedProject">
              <div class="project-summary-meta-row project-summary-meta-title">{{ selectedProject.name || '--' }}</div>
              <div class="project-summary-meta-row">
                <span class="project-summary-meta-chip">客户：{{ selectedProject.customerName || '--' }}</span>
                <span class="project-summary-meta-chip">阶段/状态：{{ selectedProject.stage || '--' }} / {{ selectedProject.status || '--' }}</span>
                <span class="project-summary-meta-chip">进度：{{ selectedProject.progress || 0 }}%</span>
                <span class="project-summary-meta-chip">风险：{{ selectedProject.riskLevel || '低' }}</span>
                <span class="project-summary-meta-chip project-summary-meta-next">项目经理：{{ selectedProject.projectManager || '--' }}</span>
                <span class="project-summary-meta-chip">项目联系人：{{ relatedContactNames.join('、') || '--' }}</span>
              </div>
            </template>
            <template v-else>
              <div class="project-summary-meta-row">
                <span class="project-summary-meta-chip">请先在左侧选中项目</span>
              </div>
            </template>
          </div>

          <div class="project-summary-header-right">
            <el-button type="primary" @click="onOpenEdit()">新建项目</el-button>
          </div>
        </div>
      </template>
    </el-card>

    <el-row :gutter="12" class="compact-project-row project-workbench-row">
      <template v-if="trackerSubView === 'weeklyProgress'">
        <el-col :span="24" class="project-workbench-col">
          <ProjectWeeklyProgressView :projects="viewProjects" />
        </el-col>
      </template>
      <template v-else>
        <el-col :xs="24" :xl="14" class="project-workbench-col">
        <ProjectTrackerOverviewChart :selected-project="selectedProject" />
        <ProjectTrackerTable
          class="project-tracker-list-panel"
          :project-filters="projectFilters"
          :view-projects="viewProjects"
          :projects-loading="projectsLoading"
          :selected-project-id="selectedProject?.id || null"
          :customers="customers"
          :users="users"
          :dictionary-options="dictionaryOptions"
          @reset-filters="onResetFilters"
          @filter="onFilter"
          @select-project="onSelectProject"
          @open-progress="onOpenProgress"
          @open-edit="onOpenEdit"
          @delete-project="onDeleteProject"
        />
      </el-col>

      <el-col :xs="24" :xl="10" class="project-workbench-col project-workbench-right-col">
        <ProjectSummaryPanel
          :selected-project="selectedProject"
          :upcoming-action-items="upcomingActionItems"
          @open-progress="onOpenProgress"
          @open-edit="onOpenEdit"
          @delete-progress="onDeleteProgress"
        />
      </el-col>
      </template>
    </el-row>

    <ProjectDialogs
      :dialogs="dialogs"
      :saving="saving"
      :forms="forms"
      :users="users"
      :customers="customers"
      :current-user-name="currentUserName"
      :dictionary-options="dictionaryOptions"
      @close-project-form="onCloseProjectForm"
      @save-project="onSaveProject"
      @close-project-progress="onCloseProjectProgress"
      @save-project-progress="onSaveProjectProgress"
    />
  </section>
</template>
