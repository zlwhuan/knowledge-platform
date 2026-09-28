<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  item: { type: Object, default: null },
  formatDateTime: { type: Function, required: true },
  formatFileSize: { type: Function, required: true },
  renderMarkdown: { type: Function, required: true },
  apiBaseUrl: { type: String, required: true },
})

const emit = defineEmits(['update:modelValue', 'edit', 'open-preview'])
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    fullscreen
    destroy-on-close
    :close-on-press-escape="true"
    class="detail-dialog-shell"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template #header>
      <div v-if="item" class="detail-top">
        <div class="detail-top-title">
          <h3>{{ item.title }}</h3>
          <el-tag effect="plain" size="small">{{ item.categoryName || '未分类' }}</el-tag>
          <el-tag v-if="item.type" effect="plain" size="small" type="info">{{ item.type }}</el-tag>
        </div>
        <p class="detail-top-summary">{{ item.summary || '暂无摘要' }}</p>
      </div>
    </template>

    <div v-if="item" class="detail-dialog-body">
      <div class="detail-meta-grid">
        <div class="detail-meta-item">
          <span class="detail-meta-label">标签</span>
          <span class="detail-meta-value">{{ item.tags || '—' }}</span>
        </div>
        <div class="detail-meta-item">
          <span class="detail-meta-label">来源</span>
          <span class="detail-meta-value">{{ item.source || '—' }}</span>
        </div>
        <div class="detail-meta-item">
          <span class="detail-meta-label">更新时间</span>
          <span class="detail-meta-value">{{ formatDateTime(item.updatedAt || item.createdAt) }}</span>
        </div>
        <div class="detail-meta-item">
          <span class="detail-meta-label">附件</span>
          <span class="detail-meta-value">{{ item.attachments?.length || 0 }} 个</span>
        </div>
      </div>

      <section v-if="item.attachments?.length" class="detail-section">
        <div class="detail-section-title">附件</div>
        <div class="detail-att-list">
          <div v-for="attachment in item.attachments" :key="attachment.id" class="detail-att-row">
            <span class="detail-att-name" :title="attachment.originalFileName">{{ attachment.originalFileName }}</span>
            <span class="detail-att-size">{{ formatFileSize(attachment.fileSize) }}</span>
            <button type="button" class="detail-link" @click="emit('open-preview', attachment)">预览</button>
            <a class="detail-link" :href="`${apiBaseUrl}/attachments/${attachment.id}/download`" target="_blank">下载</a>
          </div>
        </div>
      </section>

      <section class="detail-section">
        <div class="detail-section-title">正文</div>
        <div class="markdown-body detail-content" v-html="renderMarkdown(item.contentMarkdown || '')"></div>
      </section>
    </div>
    <el-empty v-else description="未找到对应资料" />
  </el-dialog>
</template>

<style scoped>
.detail-top {
  width: 100%;
}
.detail-top-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.detail-top-title h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.4;
}
.detail-top-summary {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
}
.detail-dialog-body {
  max-width: 1080px;
  margin: 0 auto;
  padding-bottom: 32px;
}
.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px 16px;
  padding: 14px 16px;
  background: var(--el-fill-color-light);
  border-radius: 10px;
  margin-bottom: 18px;
}
.detail-meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.detail-meta-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.detail-meta-value {
  font-size: 13px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-section {
  margin-bottom: 18px;
}
.detail-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}
.detail-att-list {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;
}
.detail-att-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.detail-att-row:last-child {
  border-bottom: none;
}
.detail-att-name {
  flex: 1;
  font-size: 13px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-att-size {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}
.detail-link {
  font-size: 12px;
  color: var(--el-color-primary);
  text-decoration: underline;
  text-underline-offset: 2px;
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  flex-shrink: 0;
}
.detail-link:hover {
  color: var(--el-color-primary-light-3);
}
.detail-content {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  padding: 16px 20px;
}
@media (max-width: 900px) {
  .detail-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
