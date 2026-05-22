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

const suggestions = computed(() => [
  { label: '分类维护', value: props.categoryCount ? '已建立分类树' : '建议先建立分类体系', hint: `当前分类 ${props.categoryCount} 个` },
  { label: '资料沉淀', value: props.itemCount ? '已有内容沉淀' : '建议先补首批核心资料', hint: `当前资料 ${props.itemCount} 条` },
  { label: '项目联动', value: props.projectCount ? '项目追踪已接入' : '建议补录项目台账', hint: `当前项目 ${props.projectCount} 个` },
  { label: '权限治理', value: props.userRole === 'ADMIN' ? '管理员可继续细化权限' : '当前以查看为主', hint: `角色数 ${props.roleCount}` },
])
</script>

<template>
  <section class="page-section">
    <el-row :gutter="12" class="system-overview-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-header-block"><div><h3>系统信息</h3><p>服务与数据概况</p></div></div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="应用名称">{{ appName }}</el-descriptions-item>
            <el-descriptions-item label="资料数量">{{ itemCount }}</el-descriptions-item>
            <el-descriptions-item label="分类数量">{{ categoryCount }}</el-descriptions-item>
            <el-descriptions-item label="用户数量">{{ userCount }}</el-descriptions-item>
            <el-descriptions-item label="项目数量">{{ projectCount }}</el-descriptions-item>
            <el-descriptions-item label="角色数量">{{ roleCount }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-header-block"><div><h3>维护建议</h3><p>结合当前数据给出下一步动作建议</p></div></div>
          </template>
          <div class="workflow-board">
            <div v-for="item in suggestions" :key="item.label" class="workflow-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.hint }}</small>
            </div>
            <div class="workflow-card"><span>缺少摘要</span><strong>{{ dashboardStats.missingSummaryCount || 0 }} 条</strong><small>建议优先补齐摘要，提升检索效率</small></div>
            <div class="workflow-card"><span>缺少附件</span><strong>{{ dashboardStats.missingAttachmentCount || 0 }} 条</strong><small>建议补充原始文件，方便后续预览与追溯</small></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>
