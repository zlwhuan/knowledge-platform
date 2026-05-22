<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../services/api'

const props = defineProps({
  auth: { type: Object, required: true },
})

const emit = defineEmits(['open-item', 'open-preview'])

const loading = ref(false)
const attachments = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filters = reactive({
  keyword: '',
  contentType: '',
  uploadedBy: '',
})

const contentTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '图片', value: 'image/' },
  { label: '视频', value: 'video/' },
  { label: '音频', value: 'audio/' },
  { label: 'PDF', value: 'application/pdf' },
  { label: 'Word', value: 'word' },
  { label: 'Excel', value: 'excel' },
  { label: 'PowerPoint', value: 'powerpoint' },
  { label: '文本/代码', value: 'text/' },
  { label: '压缩包', value: 'zip' },
]

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function formatContentType(contentType) {
  if (!contentType) return '未知'
  if (contentType.startsWith('image/')) return '图片'
  if (contentType.startsWith('video/')) return '视频'
  if (contentType.startsWith('audio/')) return '音频'
  if (contentType.includes('pdf')) return 'PDF'
  if (contentType.includes('word') || contentType.includes('docx')) return 'Word'
  if (contentType.includes('excel') || contentType.includes('xlsx') || contentType.includes('spreadsheet')) return 'Excel'
  if (contentType.includes('powerpoint') || contentType.includes('pptx') || contentType.includes('presentation')) return 'PowerPoint'
  if (contentType.startsWith('text/')) return '文本'
  if (contentType.includes('zip') || contentType.includes('rar') || contentType.includes('7z') || contentType.includes('compressed')) return '压缩包'
  return contentType
}

function getContentTypeColor(contentType) {
  if (!contentType) return 'info'
  if (contentType.startsWith('image/')) return 'success'
  if (contentType.startsWith('video/')) return 'warning'
  if (contentType.startsWith('audio/')) return 'primary'
  if (contentType.includes('pdf')) return 'danger'
  if (contentType.includes('word') || contentType.includes('docx')) return 'primary'
  if (contentType.includes('excel') || contentType.includes('xlsx') || contentType.includes('spreadsheet')) return 'success'
  if (contentType.includes('powerpoint') || contentType.includes('pptx') || contentType.includes('presentation')) return 'warning'
  if (contentType.startsWith('text/')) return 'info'
  return 'info'
}

function getFileExtension(fileName) {
  if (!fileName) return '-'
  const parts = fileName.split('.')
  return parts.length > 1 ? parts.pop().toUpperCase() : '-'
}

async function loadAttachments() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.contentType) params.contentType = filters.contentType
    const { data } = await api.get('/attachments', { params })
    attachments.value = data.data?.content || []
    total.value = data.data?.totalElements || 0
  } catch (error) {
    ElMessage.error('加载附件列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadAttachments()
}

function handlePageChange(page) {
  currentPage.value = page
  loadAttachments()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadAttachments()
}

async function handleDelete(attachment) {
  try {
    await ElMessageBox.confirm(`确定要删除附件 "${attachment.originalFileName}" 吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await api.delete(`/attachments/${attachment.id}`)
    ElMessage.success('删除成功')
    loadAttachments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  }
}

function handleDownload(attachment) {
  window.open(`/api/attachments/${attachment.id}/download`, '_blank')
}

function handlePreview(attachment) {
  emit('open-preview', attachment)
}

function handleOpenItem(attachment) {
  const itemId = attachment.itemId || attachment.item?.id
  if (!itemId) {
    ElMessage.warning('无法获取关联资料ID')
    return
  }
  emit('open-item', itemId)
}

function handleReset() {
  filters.keyword = ''
  filters.contentType = ''
  filters.uploadedBy = ''
  currentPage.value = 1
  loadAttachments()
}

const totalSize = computed(() => {
  return attachments.value.reduce((sum, a) => sum + (a.fileSize || 0), 0)
})

onMounted(() => {
  loadAttachments()
})
</script>

<template>
  <div class="attachment-management">
    <div class="page-header">
      <div class="header-title">
        <h2>附件管理</h2>
        <p>管理所有知识条目中的附件文件</p>
      </div>
      <div class="header-stats">
        <el-statistic title="附件总数" :value="total" />
        <el-statistic title="当前页文件大小" :value="formatFileSize(totalSize)" />
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索文件名或资料标题..."
        prefix-icon="Search"
        clearable
        style="width: 280px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select
        v-model="filters.contentType"
        placeholder="文件类型"
        clearable
        style="width: 130px; margin-left: 12px"
        @change="handleSearch"
      >
        <el-option
          v-for="option in contentTypeOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-input
        v-model="filters.uploadedBy"
        placeholder="上传者..."
        prefix-icon="User"
        clearable
        style="width: 150px; margin-left: 12px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" style="margin-left: 12px" @click="handleSearch">
        搜索
      </el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-table
      :data="attachments"
      v-loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="originalFileName" label="文件名" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="file-name-cell">
            <el-icon class="file-icon"><Document /></el-icon>
            <div class="file-info">
              <span class="file-name">{{ row.originalFileName }}</span>
              <span class="file-ext">{{ getFileExtension(row.originalFileName) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="contentType" label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getContentTypeColor(row.contentType)" size="small">
            {{ formatContentType(row.contentType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="100" align="right">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="itemTitle" label="所属资料" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" @click.stop="handleOpenItem(row)">
            {{ row.itemTitle || '-' }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="itemType" label="资料类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.itemType || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="所属分类" width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.categoryName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="projectName" label="所属项目" width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.projectName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="uploadedBy" label="上传者" width="90" show-overflow-tooltip />
      <el-table-column prop="uploadedAt" label="上传时间" width="160">
        <template #default="{ row }">
          {{ row.uploadedAt ? new Date(row.uploadedAt).toLocaleString('zh-CN') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right" align="center">
        <template #default="{ row }">
          <el-button-group>
            <el-button type="primary" link size="small" @click.stop="handlePreview(row)">
              预览
            </el-button>
            <el-button type="success" link size="small" @click.stop="handleDownload(row)">
              下载
            </el-button>
            <el-popconfirm
              title="确定删除此附件？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" link size="small" @click.stop>删除</el-button>
              </template>
            </el-popconfirm>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.attachment-management {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.header-title h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.header-title p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.header-stats {
  display: flex;
  gap: 32px;
}

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #909399;
  font-size: 18px;
}

.file-info {
  display: flex;
  flex-direction: column;
}

.file-name {
  font-size: 14px;
  color: #303133;
}

.file-ext {
  font-size: 12px;
  color: #909399;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

:deep(.el-table__row) {
  cursor: default;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa !important;
}
</style>
