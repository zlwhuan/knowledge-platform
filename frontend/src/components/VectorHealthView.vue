<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../services/api'

const loading = ref(false)
const rebuilding = ref(false)
const healthy = ref(null)
const status = ref(null)
let timer = null

function statusColor(key) {
  if (key === 'ready' || key === true) return 'success'
  if (key === 'error' || key === false || key === 'unavailable') return 'danger'
  return 'warning'
}

function formatBool(v) {
  if (v === true) return '是'
  if (v === false) return '否'
  return v == null ? '-' : String(v)
}

async function load() {
  loading.value = true
  try {
    const [healthRes, statusRes] = await Promise.all([
      api.get('/vector/health'),
      api.get('/vector/status'),
    ])
    healthy.value = healthRes?.data?.data?.healthy ?? false
    status.value = statusRes?.data?.data || null
  } catch (error) {
    healthy.value = false
    status.value = { status: 'error', message: error?.response?.data?.message || error?.message }
  } finally {
    loading.value = false
  }
}

async function rebuild() {
  try {
    await ElMessageBox.confirm(
      '全库重建会：\n1. 从数据库导出全部知识条目（列注释+列值拼文本，含分类树）\n2. 全部重新切块并嵌入（仅平台数据）\n\n可能需要几分钟，确定继续？',
      '全库重建向量库',
      { type: 'warning', confirmButtonText: '开始重建', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  rebuilding.value = true
  try {
    await api.post('/vector/rebuild')
    ElMessage.success('全库重建已排队，请稍后刷新')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '触发重建失败')
  } finally {
    rebuilding.value = false
  }
}

onMounted(() => {
  load()
  timer = setInterval(load, 15000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <section class="page-section vector-page">
    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="vector-page-head">
          <div>
            <h2>向量库 · 库健康</h2>
            <p>服务连通性、索引规模、嵌入模型与重建入口（每 15 秒自动刷新）</p>
          </div>
          <div class="vh-actions">
            <el-button :loading="loading" @click="load">刷新</el-button>
            <el-button type="warning" :loading="rebuilding" @click="rebuild">全库重建</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="vh-body">
        <div class="vh-health-row">
          <div class="vh-stat-card">
            <span>服务状态</span>
            <strong>
              <el-tag :type="healthy ? 'success' : 'danger'">
                {{ healthy ? '在线' : '离线/不可用' }}
              </el-tag>
            </strong>
            <small>{{ status?.serviceUrl || 'http://localhost:8081' }}</small>
          </div>
          <div class="vh-stat-card">
            <span>索引状态</span>
            <strong>
              <el-tag :type="statusColor(status?.status)">{{ status?.status || '-' }}</el-tag>
            </strong>
            <small>{{ status?.message || (status?.vector_ready ? '向量就绪' : '检查向量后端') }}</small>
          </div>
          <div class="vh-stat-card">
            <span>切块数</span>
            <strong>{{ status?.total_chunks ?? '-' }}</strong>
            <small>已入库文本块</small>
          </div>
          <div class="vh-stat-card">
            <span>文档数</span>
            <strong>{{ status?.total_documents ?? '-' }}</strong>
            <small>独立来源文档</small>
          </div>
          <div class="vh-stat-card">
            <span>vault 文件</span>
            <strong>{{ status?.vault_files ?? '-' }}</strong>
            <small>docs-vault 可入库文件</small>
          </div>
          <div class="vh-stat-card">
            <span>向量就绪</span>
            <strong>
              <el-tag :type="status?.vector_ready ? 'success' : 'warning'">
                {{ formatBool(status?.vector_ready) }}
              </el-tag>
            </strong>
            <small>LanceDB / 混合检索</small>
          </div>
        </div>

        <el-descriptions title="技术细节" :column="2" border class="vh-desc">
          <el-descriptions-item label="向量后端">{{ status?.vector_backend || '-' }}</el-descriptions-item>
          <el-descriptions-item label="嵌入模型">{{ status?.embed_model || '-' }}</el-descriptions-item>
          <el-descriptions-item label="混合检索">{{ formatBool(status?.hybrid ?? true) }}</el-descriptions-item>
          <el-descriptions-item label="服务启用">{{ formatBool(status?.enabled) }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" :span="2">
            {{ status?.vector_error || status?.message || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="!healthy"
          type="error"
          title="向量库服务不可达"
          description="请确认 product-assistant 的 FastAPI 服务已启动（默认 8081 端口），或检查 RAG_SERVICE_URL 配置。"
          :closable="false"
          show-icon
        />
        <el-alert
          v-else-if="status?.vector_ready === false"
          type="warning"
          title="向量索引未就绪"
          description="可能尚未入库或向量构建失败，可尝试「全库重建」。"
          :closable="false"
          show-icon
        />
      </div>
    </el-card>
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
.vh-actions {
  display: flex;
  gap: 8px;
}
.vh-health-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}
.vh-stat-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--el-fill-color-blank);
}
.vh-stat-card span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.vh-stat-card strong {
  font-size: 18px;
  min-height: 28px;
  display: flex;
  align-items: center;
}
.vh-stat-card small {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.vh-desc {
  margin-bottom: 16px;
}
</style>
