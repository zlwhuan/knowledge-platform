<script setup>
import { computed } from 'vue'

const props = defineProps({
  itemCount: { type: Number, default: 0 },
  categoryCount: { type: Number, default: 0 },
  projectCount: { type: Number, default: 0 },
  roleCount: { type: Number, default: 0 },
  userCount: { type: Number, default: 0 },
  appName: { type: String, default: '知识平台 CMS' },
  userRole: { type: String, default: 'SALES' },
  dashboardStats: { type: Object, default: () => ({}) },
})

const stats = computed(() => [
  { label: '资料', value: props.itemCount },
  { label: '分类', value: props.categoryCount },
  { label: '用户', value: props.userCount },
  { label: '项目', value: props.projectCount },
  { label: '角色', value: props.roleCount },
])

const suggestions = computed(() => [
  { label: '分类维护', value: props.categoryCount ? '已建立分类树' : '建议先建立分类体系', hint: `当前分类 ${props.categoryCount} 个` },
  { label: '资料沉淀', value: props.itemCount ? '已有内容沉淀' : '建议先补首批核心资料', hint: `当前资料 ${props.itemCount} 条` },
  { label: '项目联动', value: props.projectCount ? '项目追踪已接入' : '建议补录项目台账', hint: `当前项目 ${props.projectCount} 个` },
  { label: '权限治理', value: props.userRole === 'ADMIN' ? '管理员可继续细化权限' : '当前以查看为主', hint: `角色数 ${props.roleCount}` },
  { label: '缺少摘要', value: `${props.dashboardStats.missingSummaryCount || 0} 条`, hint: '建议优先补齐，提升检索效率' },
  { label: '缺少附件', value: `${props.dashboardStats.missingAttachmentCount || 0} 条`, hint: '建议补充原始文件便于追溯' },
])
</script>

<template>
  <section class="page-section sys-page">
    <el-card shadow="never" class="panel-card sys-page-card">
      <template #header>
        <div class="sys-page-head">
          <div>
            <h2>系统信息</h2>
            <p>{{ appName }} · 服务与数据概况</p>
          </div>
        </div>
      </template>

      <div class="sys-page-body">
        <div class="sys-stat-row">
          <div v-for="item in stats" :key="item.label" class="sys-stat-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <div class="sys-suggest-grid">
          <div v-for="item in suggestions" :key="item.label" class="sys-suggest-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
      </div>
    </el-card>
  </section>
</template>
