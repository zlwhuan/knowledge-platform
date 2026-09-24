<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../services/api'

const emit = defineEmits(['open-item'])

const loading = ref(false)
const mode = ref('chunks') // chunks | sources
const chunks = ref([])
const sources = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filters = reactive({
  q: '',
  path: '',
  docType: '',
})
const actionLoading = ref('')
const detailDialog = reactive({ open: false, chunk: null })

const docTypeOptions = computed(() => {
  const roots = []
  const seen = new Set()
  for (const s of sources.value) {
    const root = String(s.doc_type || s.product || '').split('/')[0]
    if (root && !seen.has(root)) {
      seen.add(root)
      roots.push({ label: root, value: root })
    }
  }
  for (const c of chunks.value) {
    const root = String(c.doc_type || c.product || '').split('/')[0]
    if (root && !seen.has(root)) {
      seen.add(root)
      roots.push({ label: root, value: root })
    }
  }
  return [{ label: '全部类型', value: '' }, ...roots]
})

const isSourceMode = computed(() => mode.value === 'sources')

async function load() {
  loading.value = true
  try {
    if (isSourceMode.value) {
      const params = {}
      if (filters.docType) params.docType = filters.docType
      const { data } = await api.get('/vector/sources', { params })
      const list = data?.data?.sources || []
      const q = filters.q.trim().toLowerCase()
      const pathQ = filters.path.trim().toLowerCase()
      sources.value = list.filter((s) => {
        if (q && !`${s.title} ${s.product} ${s.path}`.toLowerCase().includes(q)) return false
        if (pathQ && !String(s.path || '').toLowerCase().includes(pathQ)) return false
        return true
      })
      total.value = sources.value.length
    } else {
      const params = {
        limit: pageSize.value,
        offset: (currentPage.value - 1) * pageSize.value,
      }
      if (filters.q) params.q = filters.q
      if (filters.path) params.path = filters.path
      if (filters.docType) params.docType = filters.docType
      const { data } = await api.get('/vector/chunks', { params })
      chunks.value = data?.data?.items || []
      total.value = data?.data?.total || 0
    }
  } catch (error) {
    chunks.value = []
    sources.value = []
    total.value = 0
    ElMessage.error(error?.response?.data?.message || '加载索引数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  load()
}

function switchMode(next) {
  mode.value = next
  currentPage.value = 1
  load()
}

function handlePageChange(page) {
  currentPage.value = page
  load()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  load()
}

function openChunk(row) {
  detailDialog.chunk = row
  detailDialog.open = true
}

async function loadFullChunk(row) {
  try {
    const { data } = await api.get(`/vector/chunks/${row.chunk_id}`)
    detailDialog.chunk = data?.data || row
    detailDialog.open = true
  } catch {
    detailDialog.chunk = row
    detailDialog.open = true
  }
}

function itemIdFromPath(path) {
  const match = String(path || '').match(/knowledge_items\/(?:attachment_)?(\d+)/)
  if (match) return Number(match[1])
  const att = String(path || '').match(/attachments\/(\d+)\//)
  return att ? Number(att[1]) : null
}

function openAttachment(row) {
  const attId = Number(row.attachment_id)
    || Number(String(row.path || '').match(/attachments\/\d+\/(\d+)\//)?.[1])
  if (!attId) {
    ElMessage.info('无附件 ID')
    return
  }
  const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api$/, '')
  window.open(`${base}/api/attachments/${attId}/download`, '_blank')
}

function openItem(row) {
  const id = itemIdFromPath(row.path)
  if (id) {
    emit('open-item', id)
    detailDialog.open = false
  } else {
    ElMessage.info(`来源路径：${row.path}`)
  }
}

async function removeChunk(row) {
  try {
    await ElMessageBox.confirm(
      `确定把切块「${row.title || row.chunk_id}」移出向量索引吗？原文知识条目不会删除。`,
      '移出切块',
      { type: 'warning', confirmButtonText: '移出', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  actionLoading.value = row.chunk_id
  try {
    await api.delete(`/vector/chunks/${row.chunk_id}`)
    ElMessage.success('已移出索引')
    load()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '移出失败')
  } finally {
    actionLoading.value = ''
  }
}

async function removeSource(row) {
  try {
    await ElMessageBox.confirm(
      `确定把来源「${row.title || row.path}」的全部切块移出向量索引吗？`,
      '移出来源',
      { type: 'warning', confirmButtonText: '移出', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  actionLoading.value = row.path
  try {
    await api.delete('/vector/sources', { params: { path: row.path } })
    ElMessage.success('已移出索引')
    load()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '移出失败')
  } finally {
    actionLoading.value = ''
  }
}

async function reindexSource(row) {
  actionLoading.value = `re:${row.path}`
  try {
    await api.post('/vector/reindex', { path: row.path, mode: 'source' })
    ElMessage.success('重建任务已排队，稍后刷新查看')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '重建失败')
  } finally {
    actionLoading.value = ''
  }
}

async function resyncItem(row) {
  const id = itemIdFromPath(row.path)
  if (!id) {
    ElMessage.info('该来源不是平台知识条目，无法从数据库重同步')
    return
  }
  actionLoading.value = `sync:${id}`
  try {
    await api.post(`/vector/items/${id}/resync`)
    ElMessage.success('已触发从数据库重新同步')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '重同步失败')
  } finally {
    actionLoading.value = ''
  }
}

onMounted(load)
</script>

<template>
  <section class="page-section vector-page">
    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="vector-page-head">
          <div>
            <h2>向量库 · 结果维护</h2>
            <p>浏览已入库切块/来源，支持重建、移出索引、从数据库重同步</p>
          </div>
          <el-radio-group :model-value="mode" size="small" @change="switchMode">
            <el-radio-button value="chunks">切块</el-radio-button>
            <el-radio-button value="sources">来源</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div class="vm-toolbar">
        <el-input
          v-model="filters.q"
          placeholder="关键词过滤标题/正文"
          clearable
          class="vm-filter"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="filters.path"
          placeholder="路径包含…"
          clearable
          class="vm-filter"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filters.docType" placeholder="文档类型" clearable class="vm-filter-sm">
          <el-option v-for="opt in docTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="load">刷新</el-button>
      </div>

      <el-table v-if="!isSourceMode" :data="chunks" v-loading="loading" stripe size="default">
        <el-table-column label="出处" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" :type="row.source_kind === 'attachment' ? 'warning' : 'info'" effect="plain">
              {{ row.source_kind === 'attachment' ? '附件' : '条目' }}
            </el-tag>
            <span class="vm-locator">{{ row.locator || row.section }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="filename" label="附件名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="file_path" label="附件路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="product" label="分类树" width="140" show-overflow-tooltip />
        <el-table-column prop="text_preview" label="摘要" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="loadFullChunk(row)">查看</el-button>
            <el-button
              size="small"
              text
              type="warning"
              :loading="actionLoading === `re:${row.path}`"
              @click="reindexSource(row)"
            >重建</el-button>
            <el-button
              size="small"
              text
              type="danger"
              :loading="actionLoading === row.chunk_id"
              @click="removeChunk(row)"
            >移出</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="sources" v-loading="loading" stripe>
        <el-table-column label="出处" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" :type="row.source_kind === 'attachment' ? 'warning' : 'info'" effect="plain">
              {{ row.source_kind === 'attachment' ? '附件' : '条目' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="filename" label="附件名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="file_path" label="附件路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="product" label="分类树" width="140" show-overflow-tooltip />
        <el-table-column prop="chunks" label="切块数" width="80" align="center" />
        <el-table-column prop="path" label="索引路径" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.attachment_id"
              size="small"
              text
              type="warning"
              @click="openAttachment(row)"
            >打开附件</el-button>
            <el-button
              size="small"
              text
              type="primary"
              :disabled="!itemIdFromPath(row.path) && !row.item_id"
              @click="openItem(row)"
            >打开条目</el-button>
            <el-button
              size="small"
              text
              type="success"
              :loading="actionLoading === `sync:${itemIdFromPath(row.path) || row.item_id}`"
              :disabled="!itemIdFromPath(row.path) && !row.item_id"
              @click="resyncItem(row)"
            >重同步</el-button>
            <el-button
              size="small"
              text
              type="warning"
              :loading="actionLoading === `re:${row.path}`"
              @click="reindexSource(row)"
            >重建</el-button>
            <el-button
              size="small"
              text
              type="danger"
              :loading="actionLoading === row.path"
              @click="removeSource(row)"
            >移出</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!isSourceMode && total > pageSize" class="vm-pager">
        <el-pagination
          layout="total, sizes, prev, pager, next"
          :total="total"
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailDialog.open" title="切块详情" width="640px">
      <template v-if="detailDialog.chunk">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="标题">{{ detailDialog.chunk.title }}</el-descriptions-item>
          <el-descriptions-item label="出处">{{ detailDialog.chunk.locator || detailDialog.chunk.section || '-' }}</el-descriptions-item>
          <el-descriptions-item label="附件名">{{ detailDialog.chunk.filename || '-' }}</el-descriptions-item>
          <el-descriptions-item label="附件路径"><code>{{ detailDialog.chunk.file_path || '-' }}</code></el-descriptions-item>
          <el-descriptions-item label="索引路径"><code>{{ detailDialog.chunk.path }}</code></el-descriptions-item>
          <el-descriptions-item label="分类树">{{ detailDialog.chunk.product }} / {{ detailDialog.chunk.doc_type }}</el-descriptions-item>
        </el-descriptions>
        <pre class="vm-chunk-text">{{ detailDialog.chunk.text || detailDialog.chunk.text_preview }}</pre>
      </template>
      <template #footer>
        <el-button
          v-if="detailDialog.chunk?.attachment_id || String(detailDialog.chunk?.path || '').startsWith('attachments/')"
          type="warning"
          @click="openAttachment(detailDialog.chunk)"
        >打开附件</el-button>
        <el-button
          v-if="detailDialog.chunk && (itemIdFromPath(detailDialog.chunk.path) || detailDialog.chunk.item_id)"
          type="primary"
          @click="openItem(detailDialog.chunk)"
        >打开条目</el-button>
        <el-button @click="detailDialog.open = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.vector-page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.vector-page-head h2 {
  margin: 0;
  font-size: 18px;
}
.vector-page-head p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.vm-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
}
.vm-filter {
  width: 220px;
}
.vm-filter-sm {
  width: 130px;
}
.vm-pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
.vm-chunk-text {
  margin-top: 12px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 320px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.6;
}
.vm-locator {
  margin-left: 6px;
  color: var(--el-color-primary);
}
</style>
