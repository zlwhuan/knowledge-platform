<script setup>
import { computed } from 'vue'

const props = defineProps({
  itemCount: { type: Number, default: 0 },
  categoryCount: { type: Number, default: 0 },
  systemSettings: { type: Object, default: () => ({}) },
  dashboardStats: { type: Object, default: () => ({}) },
})

const previewCards = computed(() => [
  { label: 'Office 预览', value: props.systemSettings.officeCommand ? 'LibreOffice 已配置' : '未配置 LibreOffice', hint: props.systemSettings.officeCommand || '将自动回退为文本 / 原文件预览' },
  { label: 'OnlyOffice', value: props.systemSettings.onlyOfficeEnabled ? '已启用' : '未启用', hint: props.systemSettings.onlyOfficeEnabled ? 'Office 文件可走在线文档预览' : '未启用时走其他预览链路' },
  { label: '附件覆盖', value: `${props.dashboardStats.attachmentCount || 0} 个附件`, hint: `资料 ${props.itemCount} 条 / 分类 ${props.categoryCount} 个` },
  { label: '近期活跃', value: `${props.dashboardStats.recentUpdatedCount || 0} 条`, hint: '近 7 天有更新的资料数量' },
])
</script>

<template>
  <section class="page-section">
    <el-row :gutter="12" class="preview-section-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-header-block">
              <div>
                <h3>预览配置</h3>
                <p>查看当前文档预览策略与运行方式</p>
              </div>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="应用名称">{{ systemSettings.appName || '知识平台 CMS' }}</el-descriptions-item>
            <el-descriptions-item label="LibreOffice 命令">{{ systemSettings.officeCommand || '未配置' }}</el-descriptions-item>
            <el-descriptions-item label="OnlyOffice">{{ systemSettings.onlyOfficeEnabled ? '已启用' : '未启用' }}</el-descriptions-item>
            <el-descriptions-item label="服务端口">{{ systemSettings.serverPort || '8080' }}</el-descriptions-item>
            <el-descriptions-item label="预览策略">图片、音视频、PDF、文本优先在线预览，Office 走后端能力自动分流</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-header-block">
              <div>
                <h3>预览说明</h3>
                <p>结合当前环境显示可用能力与降级策略</p>
              </div>
            </div>
          </template>
          <div class="workflow-board">
            <div v-for="item in previewCards" :key="item.label" class="workflow-card"><span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.hint }}</small></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>
