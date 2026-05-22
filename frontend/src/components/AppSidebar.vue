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
  <el-aside width="280px" class="app-sidebar">
    <div class="brand-block brand-block-premium">
      <img :src="companyLogo" alt="公司Logo" class="home-company-logo" />
      <div class="brand-copy">
        <h2>项目经营协同平台</h2>
        <p>Knowledge · CRM · Project Tracking · Finance</p>
        <small>当前账号：{{ userDisplayName }}</small>
      </div>
    </div>
    <div class="sidebar-global-search" style="padding: 0 16px 12px;">
      <!-- <el-input 
        v-model="globalSearchQuery"
        placeholder="全局搜索..."
        prefix-icon="Search"
        clearable
        @keyup.enter="emit('global-search', globalSearchQuery)"
        @clear="emit('global-search', '')"
      />
       -->
    </div>
    <div class="sidebar-section sidebar-mainnav">
      <div class="section-title">主导航</div>
      <button type="button" class="nav-button" :class="{ active: currentView === 'home' }" @click="emit('go-home')">
        首页<small>总览与动态</small>
      </button>
      <div class="nav-group library-nav-group" :class="{ active: libraryMenuOpen || ['library', 'compose', 'attachment-management'].includes(currentView) }">
        <button type="button" class="nav-button nav-button-group" :class="{ active: libraryMenuOpen || ['library', 'compose', 'attachment-management'].includes(currentView) }" @click="emit('toggle-library-menu')">
          知识库<small>资料检索与分类导航</small>
        </button>
        <div v-show="libraryMenuOpen" class="subnav-card library-subnav-card">
          <div class="subnav-title">分类导航</div>
          <div class="category-scroll">
            <div class="category-nav">
              <div
                v-for="node in flatCategoryOptions.filter(isNodeVisible)"
                :key="node.id"
                class="category-link-row"
                :style="{ paddingLeft: `${8 + node.level * 18}px` }"
              >
                <button
                  v-if="hasChildren(node.id)"
                  class="category-toggle"
                  @click.stop="emit('toggle-node', node.id)"
                >
                  <span class="category-toggle-icon" :class="{ expanded: expandedNodes.has(String(node.id)) }" aria-hidden="true"></span>
                </button>
                <span v-else class="category-toggle category-toggle-placeholder"></span>
                <button
                  class="category-link"
                  :class="{ active: String(libraryFilters.categoryId || selectedCategoryId || '') === String(node.id) && currentView === 'library' }"
                  @click="if (hasChildren(node.id)) emit('toggle-node', node.id); emit('choose-category', node.id)"
                >
                  {{ node.name }}
                </button>
              </div>
            </div>
          </div>
          <button class="subnav-link" :class="{ active: currentView === 'attachment-management' }" @click="emit('open-all-library')">附件管理</button>
        </div>
      </div>
      <div class="nav-group project-nav-group" :class="{ active: String(currentView).startsWith('project') }">
        <button type="button" class="nav-button nav-button-group" :class="{ active: String(currentView).startsWith('project') || projectMenuOpen }" @click="emit('toggle-project-menu')">
          项目追踪<small>项目全周期管理与角色协同</small>
        </button>
        <div v-show="projectMenuOpen" class="subnav-card subnav-card-compact system-subnav-card">
          <button v-for="item in projectNavItems" :key="item.key" class="subnav-link" :class="{ active: currentView === item.key }" @click="emit('open-project-view', item.key)">{{ item.label }}</button>
        </div>
      </div>
      <button type="button" class="nav-button" :class="{ active: currentView === 'customers' }" @click="emit('open-system-view', 'customers')">
        客户管理<small>CRM 客户公司与联系人</small>
      </button>
      <button type="button" class="nav-button" :class="{ active: ['training', 'assessment'].includes(currentView) }" @click="emit('toggle-training-menu')">
        培训考核<small>培训记录与考核管理</small>
      </button>
      <div v-show="trainingMenuOpen" class="subnav-card subnav-card-compact system-subnav-card">
        <button class="subnav-link" :class="{ active: currentView === 'training' }" @click="emit('open-system-view', 'training')">培训记录</button>
        <button class="subnav-link" :class="{ active: currentView === 'assessment' }" @click="emit('open-system-view', 'assessment')">考核记录</button>
      </div>
      <div class="nav-group system-nav-group" :class="{ active: ['settings', 'users', 'roles', 'preview-settings', 'system-overview', 'dictionary-settings'].includes(currentView) }">
        <button type="button" class="nav-button nav-button-group" :class="{ active: ['settings', 'users', 'roles', 'preview-settings', 'system-overview', 'dictionary-settings'].includes(currentView) || systemMenuOpen }" @click="emit('toggle-system-menu')">
          系统配置<small>分类、角色与参数入口</small>
        </button>
        <div v-show="systemMenuOpen" class="subnav-card subnav-card-compact system-subnav-card">
          <button v-for="item in visibleSystemNavItems" :key="item.key" class="subnav-link" :class="[{ active: currentView === item.key }, item.key === 'logout' ? 'logout-subnav-link' : '']" @click="()=>{emit('open-system-view', item.key) }">{{ item.label }}</button>
        </div>
      </div>
    </div>
  </el-aside>
</template>
<style scoped>
.home-company-logo { height: 44px; width: auto; object-fit: contain; filter: drop-shadow(0 6px 14px rgba(20, 63, 125, 0.16)); }
</style>
