<script setup>
import { ref } from 'vue'
import companyLogo from '../img/logo.png'
defineProps({
  userDisplayName: { type: String, required: true },
  currentView: { type: String, required: true },
  libraryMenuOpen: { type: Boolean, default: false },
  projectMenuOpen: { type: Boolean, default: false },
  systemMenuOpen: { type: Boolean, default: false },
  trainingMenuOpen: { type: Boolean, default: false },
  projectNavItems: { type: Array, required: true },
  visibleSystemNavItems: { type: Array, required: true },
  flatCategoryOptions: { type: Array, required: true },
  libraryFilters: { type: Object, required: true },
  selectedCategoryId: { type: [String, Number, null], default: null },
  expandedNodes: { type: Object, required: true },
  isNodeVisible: { type: Function, required: true },
  hasChildren: { type: Function, required: true },
})

const emit = defineEmits([
  'go-home',
  'toggle-library-menu',
  'open-all-library',
  'toggle-node',
  'choose-category',
  'toggle-project-menu',
  'open-project-view',
  'toggle-system-menu',
  'open-system-view',
  'toggle-training-menu',
  'global-search',
])

const globalSearchQuery = ref('')
</script>

<template>
  <el-aside class="app-sidebar">
    <div class="brand-block brand-block-premium">
      <img :src="companyLogo" alt="公司Logo" class="home-company-logo" />
      <div class="brand-copy">
        <h2>项目经营协同平台</h2>
        <small>{{ userDisplayName }}</small>
      </div>
    </div>

    <div class="sidebar-section sidebar-mainnav">
      <div class="section-title">主导航</div>

      <button type="button" class="nav-button" :class="{ active: currentView === 'home' }" @click="emit('go-home')">
        <span class="nav-main-label">首页</span>
        <small>总览与动态</small>
      </button>

      <div class="nav-group library-nav-group" :class="{ open: libraryMenuOpen }">
        <button
          type="button"
          class="nav-button nav-button-group"
          :class="{ active: libraryMenuOpen || ['library', 'compose', 'attachment-management'].includes(currentView) }"
          @click="emit('toggle-library-menu')"
        >
          <span class="nav-main-label">知识库</span>
          <small>资料检索与分类导航</small>
        </button>
        <div v-if="libraryMenuOpen" class="nav-sub-list">
          <button
            v-for="node in flatCategoryOptions.filter(isNodeVisible)"
            :key="node.id"
            type="button"
            class="nav-sub-link"
            :class="{
              active: String(libraryFilters.categoryId || selectedCategoryId || '') === String(node.id) && currentView === 'library',
              'is-level-1': node.level <= 0,
              'is-level-2': node.level === 1,
              'is-level-n': node.level >= 2,
            }"
            @click="if (hasChildren(node.id)) emit('toggle-node', node.id); emit('choose-category', node.id)"
          >
            {{ node.name }}
          </button>
          <button
            type="button"
            class="nav-sub-link is-level-1"
            :class="{ active: currentView === 'attachment-management' }"
            @click="emit('open-all-library')"
          >
            附件管理
          </button>
        </div>
      </div>

      <div class="nav-group project-nav-group" :class="{ open: projectMenuOpen }">
        <button
          type="button"
          class="nav-button nav-button-group"
          :class="{ active: projectMenuOpen || String(currentView).startsWith('project') }"
          @click="emit('toggle-project-menu')"
        >
          <span class="nav-main-label">项目追踪</span>
          <small>项目全周期管理与角色协同</small>
        </button>
        <div v-if="projectMenuOpen" class="nav-sub-list">
          <button
            v-for="item in projectNavItems"
            :key="item.key"
            type="button"
            class="nav-sub-link is-level-1"
            :class="{ active: currentView === item.key }"
            @click="emit('open-project-view', item.key)"
          >{{ item.label }}</button>
        </div>
      </div>

      <button type="button" class="nav-button" :class="{ active: currentView === 'customers' }" @click="emit('open-system-view', 'customers')">
        <span class="nav-main-label">客户管理</span>
        <small>CRM 客户公司与联系人</small>
      </button>

      <div class="nav-group training-nav-group" :class="{ open: trainingMenuOpen }">
        <button
          type="button"
          class="nav-button nav-button-group"
          :class="{ active: trainingMenuOpen || ['training', 'assessment'].includes(currentView) }"
          @click="emit('toggle-training-menu')"
        >
          <span class="nav-main-label">培训考核</span>
          <small>培训记录与考核管理</small>
        </button>
        <div v-if="trainingMenuOpen" class="nav-sub-list">
          <button type="button" class="nav-sub-link is-level-1" :class="{ active: currentView === 'training' }" @click="emit('open-system-view', 'training')">培训记录</button>
          <button type="button" class="nav-sub-link is-level-1" :class="{ active: currentView === 'assessment' }" @click="emit('open-system-view', 'assessment')">考核记录</button>
        </div>
      </div>

      <div class="nav-group system-nav-group" :class="{ open: systemMenuOpen }">
        <button
          type="button"
          class="nav-button nav-button-group"
          :class="{ active: systemMenuOpen || ['settings', 'users', 'roles', 'preview-settings', 'system-overview', 'dictionary-settings'].includes(currentView) }"
          @click="emit('toggle-system-menu')"
        >
          <span class="nav-main-label">系统配置</span>
          <small>分类、角色与参数入口</small>
        </button>
        <div v-if="systemMenuOpen" class="nav-sub-list">
          <button
            v-for="item in visibleSystemNavItems"
            :key="item.key"
            type="button"
            class="nav-sub-link is-level-1"
            :class="[{ active: currentView === item.key }, item.key === 'logout' ? 'logout-subnav-link' : '']"
            @click="() => { emit('open-system-view', item.key) }"
          >{{ item.label }}</button>
        </div>
      </div>
    </div>
  </el-aside>
</template>

<style scoped>
.home-company-logo {
  height: 30px;
  width: auto;
  object-fit: contain;
  filter: drop-shadow(0 4px 10px rgba(20, 63, 125, 0.16));
  flex: 0 0 auto;
}

.brand-block-premium {
  margin-bottom: 8px;
  padding: 6px 4px 8px;
  gap: 8px;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-block-premium .brand-copy {
  min-width: 0;
  line-height: 1.25;
}

.brand-block-premium h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 650;
  color: #fff !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-block-premium small {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: rgba(229, 240, 255, 0.72);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
