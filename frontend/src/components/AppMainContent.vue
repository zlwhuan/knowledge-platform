<script setup>
import { computed, defineAsyncComponent } from 'vue'

const HomeDashboard = defineAsyncComponent(() => import('./HomeDashboard.vue'))
const KnowledgeLibraryPage = defineAsyncComponent(() => import('./KnowledgeLibraryPage.vue'))
const ProjectTrackerView = defineAsyncComponent(() => import('./ProjectTrackerView.vue'))
const SystemConfigView = defineAsyncComponent(() => import('./SystemConfigView.vue'))
const RoleManagementView = defineAsyncComponent(() => import('./RoleManagementView.vue'))
const CategoryManagementView = defineAsyncComponent(() => import('./CategoryManagementView.vue'))
const UserManagementView = defineAsyncComponent(() => import('./UserManagementView.vue'))
const PreviewSettingsView = defineAsyncComponent(() => import('./PreviewSettingsView.vue'))
const SystemOverviewView = defineAsyncComponent(() => import('./SystemOverviewView.vue'))
const LibraryComposeView = defineAsyncComponent(() => import('./LibraryComposeView.vue'))
const CustomerManagementView = defineAsyncComponent(() => import('./CustomerManagementView.vue'))
const DictionaryManagementView = defineAsyncComponent(() => import('./DictionaryManagementView.vue'))
const ProjectGanttView = defineAsyncComponent(() => import('./ProjectGanttView.vue'))
const AttachmentManagement = defineAsyncComponent(() => import('./AttachmentManagement.vue'))
const TrainingManagement = defineAsyncComponent(() => import('./TrainingManagement.vue'))
const AssessmentManagement = defineAsyncComponent(() => import('./AssessmentManagement.vue'))

const props = defineProps({
  currentView: { type: String, required: true },
  currentRolePermissions: { type: Object, required: true },
  homeQuickStats: { type: Array, required: true },
  projectTrackerSummary: { type: Array, required: true },
  homeProjectFocus: { type: Array, required: true },
  projectRecordFeed: { type: Array, required: true },
  homeActionQueue: { type: Array, required: true },
  projectMilestones: { type: Array, required: true },
  customerFollowupAlerts: { type: Array, default: () => [] },
  libraryProps: { type: Object, required: true },
  composeProps: { type: Object, required: true },
  projectProps: { type: Object, required: true },
  categoryProps: { type: Object, required: true },
  userProps: { type: Object, required: true },
  roleProps: { type: Object, required: true },
  previewSettingsProps: { type: Object, required: true },
  systemOverviewProps: { type: Object, required: true },
  customerProps: { type: Object, required: true },
  dictionaryProps: { type: Object, required: true },
})

const emit = defineEmits([
  'open-system-view',
  'open-project-view',
  'open-project-progress-from-home',
  'start-create-content',
  'open-all-library',
  'reset-library-filters',
  'load-items',
  'update-library-quick-filter',
  'toggle-select-all-displayed',
  'open-batch-edit-dialog',
  'bulk-delete-items',
  'clear-selected-items',
  'toggle-select-item',
  'open-detail',
  'fill-form',
  'delete-item',
  'compose-back',
  'save-library-item',
  'files-change',
  'remove-queued-file',
  'open-preview',
  'remove-attachment',
  'switch-subview',
  'refresh-projects',
  'open-project-progress',
  'open-project-edit',
  'open-related-item',
  'create-related-item',
  'select-project',
  'reset-project-filters',
  'filter-projects',
  'delete-project',
  'delete-project-progress',
  'close-project-form',
  'save-project',
  'close-project-progress',
  'save-project-progress',
  'edit-category',
  'create-child-category',
  'reset-category',
  'save-category',
  'delete-category',
  'reset-user',
  'save-user',
  'edit-user',
  'delete-user',
  'save-role',
  'filter-customers',
  'reset-customer-filters',
  'select-customer',
  'open-customer',
  'delete-customer',
  'open-contact',
  'delete-contact',
  'save-customer',
  'close-customer',
  'save-contact',
  'close-contact',
  'open-followup',
  'delete-followup',
  'save-followup',
  'close-followup',
  'edit-dictionary',
  'reset-dictionary',
  'save-dictionary',
  'delete-dictionary',
  'page-change',
  'size-change',
  'trigger-import',
])
</script>

<template>
  <el-main class="app-main">
    <HomeDashboard
      v-if="currentView === 'home'"
      :home-quick-stats="homeQuickStats"
      :can-create-content="currentRolePermissions.canCreateContent"
      :project-tracker-summary="projectTrackerSummary"
      :home-project-focus="homeProjectFocus"
      :project-record-feed="projectRecordFeed"
      :home-action-queue="homeActionQueue"
      :project-milestones="projectMilestones"
      :customer-followup-alerts="customerFollowupAlerts"
      @open-project-view="emit('open-project-view', $event)"
      @open-project-progress="emit('open-project-progress-from-home', $event)"
      @start-create-content="emit('start-create-content')"
      @open-all-library="emit('open-all-library')"
      @open-system-view="emit('open-system-view', $event)"
    />

    <KnowledgeLibraryPage
      v-else-if="currentView === 'library'"
      v-bind="libraryProps"
      @reset-library-filters="emit('reset-library-filters')"
      @load-items="emit('load-items')"
      @update-library-quick-filter="emit('update-library-quick-filter', $event)"
      @start-create-content="emit('start-create-content')"
      @toggle-select-all-displayed="emit('toggle-select-all-displayed')"
      @open-batch-edit-dialog="emit('open-batch-edit-dialog')"
      @bulk-delete-items="emit('bulk-delete-items')"
      @clear-selected-items="emit('clear-selected-items')"
      @toggle-select-item="emit('toggle-select-item', $event)"
      @open-detail="emit('open-detail', $event)"
      @fill-form="emit('fill-form', $event)"
      @delete-item="emit('delete-item', $event)"
      @page-change="emit('page-change', $event)"
      @size-change="emit('size-change', $event)"
      @trigger-import="emit('trigger-import')"
    />

    <LibraryComposeView
      v-else-if="currentView === 'compose'"
      v-bind="composeProps"
      @back="emit('compose-back')"
      @save="emit('save-library-item')"
      @files-change="emit('files-change', $event)"
      @remove-queued-file="emit('remove-queued-file', $event)"
      @open-preview="emit('open-preview', $event)"
      @remove-attachment="emit('remove-attachment', $event)"
    />

    <ProjectGanttView
      v-else-if="currentView === 'project-gantt'"
      :projects="projectProps.viewProjects"
      :loading="projectProps.projectsLoading"
      @select-project="emit('select-project', $event)"
    />

    <ProjectTrackerView
      v-else-if="String(currentView).startsWith('project')"
      v-bind="projectProps"
      @switch-subview="emit('switch-subview', $event)"
      @refresh="emit('refresh-projects')"
      @open-progress="emit('open-project-progress', $event)"
      @open-edit="emit('open-project-edit', $event)"
      @open-related-item="emit('open-related-item', $event)"
      @create-related-item="emit('create-related-item', $event)"
      @select-project="emit('select-project', $event)"
      @reset-filters="emit('reset-project-filters')"
      @filter="emit('filter-projects')"
      @delete-project="emit('delete-project', $event)"
      @delete-progress="emit('delete-project-progress', $event)"
      @close-project-form="emit('close-project-form')"
      @save-project="emit('save-project')"
      @close-project-progress="emit('close-project-progress')"
      @save-project-progress="emit('save-project-progress')"
    />

    <CustomerManagementView
      v-else-if="currentView === 'customers'"
      v-bind="customerProps"
      @filter="emit('filter-customers')"
      @reset-filters="emit('reset-customer-filters')"
      @select-customer="emit('select-customer', $event)"
      @open-customer="emit('open-customer', $event)"
      @delete-customer="emit('delete-customer', $event)"
      @open-contact="emit('open-contact', $event)"
      @delete-contact="emit('delete-contact', $event)"
      @save-customer="emit('save-customer')"
      @close-customer="emit('close-customer')"
      @save-contact="emit('save-contact')"
      @close-contact="emit('close-contact')"
      @open-followup="emit('open-followup', $event)"
      @delete-followup="emit('delete-followup', $event)"
      @save-followup="emit('save-followup')"
      @close-followup="emit('close-followup')"
    />

    <CategoryManagementView
      v-else-if="currentView === 'settings'"
      v-bind="categoryProps"
      @edit-category="emit('edit-category', $event)"
      @create-child-category="emit('create-child-category', $event)"
      @reset-category="emit('reset-category')"
      @save-category="emit('save-category')"
      @delete-category="emit('delete-category', $event)"
    />

    <UserManagementView
      v-else-if="currentView === 'users'"
      v-bind="userProps"
      @reset-user="emit('reset-user')"
      @save-user="emit('save-user')"
      @edit-user="emit('edit-user', $event)"
      @delete-user="emit('delete-user', $event)"
    />

    <RoleManagementView
      v-else-if="currentView === 'roles'"
      v-bind="roleProps"
      @save-role="emit('save-role', $event)"
    />

    <PreviewSettingsView
      v-else-if="currentView === 'preview-settings'"
      v-bind="previewSettingsProps"
    />

    <SystemOverviewView
      v-else-if="currentView === 'system-overview'"
      v-bind="systemOverviewProps"
    />

    <AttachmentManagement
      v-else-if="currentView === 'attachment-management'"
      :auth="{}"
      @open-item="(itemId) => emit('open-detail', { id: itemId })"
      @open-preview="(attachment) => emit('open-preview', attachment)"
    />
    <TrainingManagement
      v-else-if="currentView === 'training'"
      :auth="{}"
    />
    <AssessmentManagement
      v-else-if="currentView === 'assessment'"
      :auth="{}"
    />
    <DictionaryManagementView
      v-else-if="currentView === 'dictionary-settings'"
      v-bind="dictionaryProps"
      @edit-dictionary="emit('edit-dictionary', $event)"
      @reset-dictionary="emit('reset-dictionary', $event)"
      @save-dictionary="emit('save-dictionary')"
      @delete-dictionary="emit('delete-dictionary', $event)"
    />
    <SystemConfigView v-else />
  </el-main>
</template>
