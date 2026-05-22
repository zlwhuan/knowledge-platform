<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  item: { type: Object, default: null },
  formatDateTime: { type: Function, required: true },
  formatFileSize: { type: Function, required: true },
  renderMarkdown: { type: Function, required: true },
  apiBaseUrl: { type: String, required: true },
  canDeleteContent: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'edit', 'open-preview', 'delete-attachment'])
</script>

<template>
  <el-dialog :model-value="modelValue" fullscreen destroy-on-close :close-on-press-escape="true" class="detail-dialog-shell" @update:model-value="emit('update:modelValue', $event)">
    <template #header>
      <div class="preview-header" v-if="item">
        <div class="preview-header-main">
          <div class="preview-title-row">
            <h3>{{ item.title }}</h3>
            <el-tag effect="plain">{{ item.categoryName || '未分类' }}</el-tag>
          </div>
          <textarea
            class="detail-summary-preview"
            :value="item.summary || '暂无摘要'"
            readonly
            aria-label="摘要预览"
          ></textarea>
        </div>
        <el-space>
          <el-button @click="emit('update:modelValue', false)">返回列表</el-button>
          <el-button type="primary" @click="emit('edit', item)">进入编辑</el-button>
        </el-space>
      </div>
    </template>
    <div v-if="item" class="detail-dialog-body">
      <el-descriptions :column="3" border class="detail-descriptions detail-descriptions-wide">
        <el-descriptions-item label="标签">{{ item.tags || '未设置标签' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ item.source || '未记录来源' }}</el-descriptions-item>
        <el-descriptions-item label="附件数">{{ item.attachments?.length || 0 }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(item.updatedAt || item.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ item.categoryName || '未分类' }}</el-descriptions-item>
      </el-descriptions>
      <el-card class="attachment-card" shadow="never">
        <div class="card-subtitle">附件</div>
        <div v-if="item.attachments?.length" class="attachment-list">
          <div v-for="attachment in item.attachments" :key="attachment.id" class="attachment-row">
            <div>
              <strong>{{ attachment.originalFileName }}</strong>
              <span>{{ formatFileSize(attachment.fileSize) }}</span>
            </div>
            <el-space>
              <el-button link type="primary" @click="emit('open-preview', attachment)">预览</el-button>
              <el-button link :href="`${apiBaseUrl}/attachments/${attachment.id}/download`" target="_blank">下载</el-button>
              <el-button v-if="canDeleteContent" link type="danger" @click="emit('delete-attachment', attachment)">删除</el-button>
            </el-space>
          </div>
        </div>
        <el-empty v-else description="暂无附件" />
      </el-card>
      <el-card shadow="never" class="markdown-card dialog-markdown-card">
        <div class="card-subtitle">正文内容</div>
        <div class="markdown-body" v-html="renderMarkdown(item.contentMarkdown || '')"></div>
      </el-card>
    </div>
    <el-empty v-else description="未找到对应资料" />
  </el-dialog>
</template>
