<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../services/api'

const emit = defineEmits(['open-item'])

const loading = ref(false)
const hasSearched = ref(false)
const results = ref([])
const query = ref('')
const topK = ref(8)
const filters = reactive({
  category: '',
  docType: '',
})

// 分类树选项（来自知识库分类），替代旧 products/faq/meeting/competitor
const categoryOptions = ref([{ label: '全部分类', value: '' }])
const docTypeOptions = computed(() => {
  const roots = []
  const seen = new Set()
  for (const opt of categoryOptions.value) {
    if (!opt.value) continue
    const root = opt.value.split('/')[0]
    if (!seen.has(root)) {
      seen.add(root)
      roots.push({ label: root, value: root })
    }
  }
  return [{ label: '全部类型', value: '' }, ...roots]
})

async function loadCategories() {
  try {
    const { data } = await api.get('/categories')
    const list = data?.data || []
    const flat = []
    const walk = (nodes, prefix) => {
      for (const n of nodes || []) {
        const path = n.path || (prefix ? `${prefix}/${n.name}` : n.name)
        flat.push({ label: path, value: path })
        walk(n.children, path)
      }
    }
    walk(list, '')
    categoryOptions.value = [{ label: '全部分类', value: '' }, ...flat]
  } catch {
    // 分类接口失败时保留默认
  }
}

function highlight(text) {
  if (!text) return ''
  const q = query.value.trim()
  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  if (!q) return escaped
  const safeQ = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escaped.replace(new RegExp(safeQ, 'gi'), (m) => `<mark class="vs-hit">${m}</mark>`)
}

async function doSearch() {
  const q = query.value.trim()
  if (q.length < 1) {
    ElMessage.warning('请输入检索词')
    return
  }
  loading.value = true
  hasSearched.value = true
  try {
    const payload = { query: q, top_k: topK.value }
    if (filters.category) payload.category = filters.category
    if (filters.docType) payload.doc_type = filters.docType
    const { data } = await api.post('/vector/search', payload)
    results.value = data?.data || []
  } catch (error) {
    results.value = []
    ElMessage.error(error?.response?.data?.message || '检索失败，请确认向量库服务已启动')
  } finally {
    loading.value = false
  }
}

function itemIdOf(row) {
  if (row.item_id) return Number(row.item_id)
  const match = String(row.path || '').match(/knowledge_items\/(?:attachment_)?(\d+)/)
  return match ? Number(match[1]) : null
}

function attachmentIdOf(row) {
  if (row.attachment_id) return Number(row.attachment_id)
  const match = String(row.path || '').match(/attachments\/\d+\/(\d+)\//)
  return match ? Number(match[1]) : null
}

function openSource(row) {
  const itemId = itemIdOf(row)
  if (itemId) {
    emit('open-item', itemId)
    return
  }
  ElMessage.info(`来源路径：${row.path || '未知'}`)
}

function openAttachment(row) {
  const attId = attachmentIdOf(row)
  if (!attId) {
    ElMessage.info('该命中不在附件中')
    return
  }
  // 与附件管理一致的下载/预览入口
  const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api$/, '')
  window.open(`${base}/api/attachments/${attId}/download`, '_blank')
}

function locatorText(row) {
  return row.locator || (row.source_kind === 'attachment'
    ? `附件 ${row.filename || ''} · ${row.section || ''}${row.page ? ' · p.' + row.page : ''}`
    : `条目正文${row.section ? ' · ' + row.section : ''}`)
}

function scoreText(score) {
  const n = Number(score)
  if (!Number.isFinite(n)) return '-'
  return n.toFixed(3)
}

onMounted(() => {
  loadCategories()
})
</script>

<template>
  <section class="page-section vector-page">
    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="vector-page-head">
          <div>
            <h2>向量库 · 快捷搜索</h2>
            <p>混合检索（向量 + BM25），支持分类与文档类型过滤</p>
          </div>
        </div>
      </template>

      <div class="vs-search-bar">
        <el-input
          v-model="query"
          placeholder="输入产品名、参数、问题关键词…"
          clearable
          class="vs-query"
          @keyup.enter="doSearch"
        />
        <el-select v-model="filters.category" placeholder="分类树" clearable class="vs-filter">
          <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-select v-model="filters.docType" placeholder="文档类型" clearable class="vs-filter">
          <el-option v-for="opt in docTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-input-number v-model="topK" :min="1" :max="30" class="vs-topk" />
        <el-button type="primary" :loading="loading" @click="doSearch">搜索</el-button>
      </div>

      <div v-if="loading" class="vs-empty">检索中…</div>
      <div v-else-if="!hasSearched" class="vs-empty">输入关键词开始语义检索</div>
      <div v-else-if="!results.length" class="vs-empty">没有命中结果，试试更短或更具体的词</div>

      <div v-else class="vs-result-list">
        <article v-for="row in results" :key="row.chunk_id" class="vs-card">
          <header class="vs-card-head">
            <div class="vs-title" v-html="highlight(row.title)"></div>
            <el-tag size="small" effect="plain">{{ scoreText(row.score) }}</el-tag>
          </header>
          <div class="vs-meta">
            <el-tag size="small" :type="row.source_kind === 'attachment' ? 'warning' : 'info'" effect="plain">
              {{ row.source_kind === 'attachment' ? '附件' : '条目' }}
            </el-tag>
            <span class="vs-locator" v-html="highlight(locatorText(row))"></span>
            <span v-if="row.product">{{ row.product }}</span>
          </div>
          <p class="vs-text" v-html="highlight(row.text)"></p>
          <footer class="vs-card-foot">
            <code class="vs-path">{{ row.file_path || row.path }}</code>
            <div class="vs-actions">
              <el-button
                v-if="row.source_kind === 'attachment' || row.attachment_id"
                size="small"
                text
                type="warning"
                @click="openAttachment(row)"
              >打开附件</el-button>
              <el-button size="small" text type="primary" @click="openSource(row)">打开条目</el-button>
            </div>
          </footer>
        </article>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.vector-page-head h2 {
  margin: 0;
  font-size: 18px;
}
.vector-page-head p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.vs-search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
  align-items: center;
}
.vs-query {
  flex: 1 1 240px;
  min-width: 200px;
}
.vs-filter {
  width: 140px;
}
.vs-topk {
  width: 110px;
}
.vs-empty {
  padding: 48px 0;
  text-align: center;
  color: var(--el-text-color-secondary);
}
.vs-result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.vs-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px 14px;
  background: var(--el-fill-color-blank);
}
.vs-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.vs-title {
  font-weight: 600;
  font-size: 15px;
}
.vs-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.vs-section {
  color: var(--el-color-primary);
}
.vs-locator {
  color: var(--el-color-primary);
  font-weight: 500;
}
.vs-actions {
  display: flex;
  gap: 4px;
}
.vs-text {
  margin: 8px 0;
  line-height: 1.6;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
}
.vs-card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.vs-path {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  word-break: break-all;
}
:deep(.vs-hit) {
  background: #ffe58f;
  color: #613400;
  border-radius: 2px;
  padding: 0 1px;
}
</style>
