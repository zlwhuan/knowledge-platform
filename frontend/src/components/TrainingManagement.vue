<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../services/api'

const props = defineProps({
  auth: { type: Object, default: null },
})

const loading = ref(false)
const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filters = reactive({
  keyword: '',
  trainingType: '',
})

// 表单对话框
const formDialog = reactive({
  open: false,
  loading: false,
  isEdit: false,
  id: null,
  title: '',
  content: '',
  trainingDate: '',
  trainingType: '',
  trainer: '',
  participantIds: '',
  remarks: '',
})

const trainingTypeOptions = [
  { label: '线下培训', value: '线下' },
  { label: '会议培训', value: '会议' },
  { label: '线上培训', value: '线上' },
]

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadRecords() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.trainingType) params.trainingType = filters.trainingType
    const { data } = await api.get('/training-records', { params })
    records.value = data.data?.content || []
    total.value = data.data?.totalElements || 0
  } catch (error) {
    ElMessage.error('加载培训记录失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadRecords()
}

function handlePageChange(page) {
  currentPage.value = page
  loadRecords()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadRecords()
}

function openCreateDialog() {
  formDialog.isEdit = false
  formDialog.id = null
  formDialog.title = ''
  formDialog.content = ''
  formDialog.trainingDate = new Date().toISOString().slice(0, 16)
  formDialog.trainingType = ''
  formDialog.trainer = ''
  formDialog.participantIds = ''
  formDialog.remarks = ''
  formDialog.open = true
}

function openEditDialog(record) {
  formDialog.isEdit = true
  formDialog.id = record.id
  formDialog.title = record.title
  formDialog.content = record.content || ''
  formDialog.trainingDate = record.trainingDate ? new Date(record.trainingDate).toISOString().slice(0, 16) : ''
  formDialog.trainingType = record.trainingType || ''
  formDialog.trainer = record.trainer || ''
  formDialog.participantIds = record.participantIds || ''
  formDialog.remarks = record.remarks || ''
  formDialog.open = true
}

async function handleSubmit() {
  if (!formDialog.title.trim()) {
    ElMessage.warning('请输入培训主题')
    return
  }
  if (!formDialog.trainingDate) {
    ElMessage.warning('请选择培训时间')
    return
  }
  if (!formDialog.trainingType) {
    ElMessage.warning('请选择培训类型')
    return
  }

  formDialog.loading = true
  try {
    const payload = {
      title: formDialog.title,
      content: formDialog.content,
      trainingDate: new Date(formDialog.trainingDate).toISOString(),
      trainingType: formDialog.trainingType,
      trainer: formDialog.trainer,
      participantIds: formDialog.participantIds,
      remarks: formDialog.remarks,
    }

    if (formDialog.isEdit) {
      await api.put(`/training-records/${formDialog.id}`, payload)
      ElMessage.success('更新成功')
    } else {
      await api.post('/training-records', payload)
      ElMessage.success('创建成功')
    }

    formDialog.open = false
    loadRecords()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  } finally {
    formDialog.loading = false
  }
}

async function handleDelete(record) {
  try {
    await ElMessageBox.confirm(`确定要删除培训记录 "${record.title}" 吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await api.delete(`/training-records/${record.id}`)
    ElMessage.success('删除成功')
    loadRecords()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  }
}

function handleReset() {
  filters.keyword = ''
  filters.trainingType = ''
  currentPage.value = 1
  loadRecords()
}

onMounted(() => {
  loadRecords()
})
</script>

<template>
  <div class="training-management">
    <div class="page-header">
      <div class="header-title">
        <h2>培训记录管理</h2>
        <p>管理线下培训、会议培训等记录</p>
      </div>
      <div class="header-stats">
        <el-statistic title="培训记录总数" :value="total" />
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索培训主题..."
        prefix-icon="Search"
        clearable
        style="width: 280px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select
        v-model="filters.trainingType"
        placeholder="培训类型"
        clearable
        style="width: 130px; margin-left: 12px"
        @change="handleSearch"
      >
        <el-option
          v-for="option in trainingTypeOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-button type="primary" style="margin-left: 12px" @click="handleSearch">
        搜索
      </el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" style="margin-left: auto" @click="openCreateDialog">
        新建培训记录
      </el-button>
    </div>

    <el-table
      :data="records"
      v-loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="title" label="培训主题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="trainingType" label="培训类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.trainingType || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="trainer" label="培训讲师" width="120" show-overflow-tooltip />
      <el-table-column prop="trainingDate" label="培训时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.trainingDate) }}
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" label="创建人" width="100" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button-group>
            <el-button type="primary" link size="small" @click.stop="openEditDialog(row)">
              编辑
            </el-button>
            <el-popconfirm
              title="确定删除此记录？"
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
    <el-empty v-if="!loading && !records.length" description="暂无培训记录" />
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

    <!-- 表单对话框 -->
    <el-dialog
      v-model="formDialog.open"
      :title="formDialog.isEdit ? '编辑培训记录' : '新建培训记录'"
      width="700px"
      destroy-on-close
      append-to-body
    >
      <el-form label-position="top" :model="formDialog">
        <el-form-item label="培训主题" required>
          <el-input v-model="formDialog.title" placeholder="请输入培训主题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="培训类型">
              <el-select v-model="formDialog.trainingType" placeholder="请选择培训类型" style="width: 100%">
                <el-option
                  v-for="option in trainingTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="培训讲师">
              <el-input v-model="formDialog.trainer" placeholder="请输入培训讲师" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="培训时间" required>
          <el-date-picker
            v-model="formDialog.trainingDate"
            type="datetime"
            placeholder="选择培训时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm"
          />
        </el-form-item>
        <el-form-item label="培训内容">
          <el-input
            v-model="formDialog.content"
            type="textarea"
            :rows="6"
            placeholder="请输入培训内容/知识点"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="formDialog.remarks"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.open = false">取消</el-button>
        <el-button type="primary" :loading="formDialog.loading" @click="handleSubmit">
          {{ formDialog.isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.training-management {
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

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}
</style>
