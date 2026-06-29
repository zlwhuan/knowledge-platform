<script setup>
import { ref, reactive, onMounted } from 'vue'
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
  assessmentType: '',
  grade: '',
})

// 表单对话框
const formDialog = reactive({
  open: false,
  loading: false,
  isEdit: false,
  id: null,
  title: '',
  assessmentType: '',
  assessmentDate: '',
  assessorIds: '',
  grade: '',
  evaluation: '',
  trainingRecordId: null,
})

// 培训记录列表
const trainingRecords = ref([])

const assessmentTypeOptions = [
  { label: '模拟售前', value: '模拟售前' },
  { label: '本地测试', value: '本地测试' },
  { label: '现场实施', value: '现场实施' },
]

const gradeOptions = [
  { label: 'A - 优秀', value: 'A' },
  { label: 'B - 良好', value: 'B' },
  { label: 'C - 合格', value: 'C' },
  { label: 'D - 不合格', value: 'D' },
]

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function getGradeType(grade) {
  const map = { 'A': 'success', 'B': 'primary', 'C': 'warning', 'D': 'danger' }
  return map[grade] || 'info'
}

async function loadRecords() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.assessmentType) params.assessmentType = filters.assessmentType
    if (filters.grade) params.grade = filters.grade
    const { data } = await api.get('/assessment-records', { params })
    records.value = data.data?.content || []
    total.value = data.data?.totalElements || 0
  } catch (error) {
    ElMessage.error('加载考核记录失败')
  } finally {
    loading.value = false
  }
}

async function loadTrainingRecords() {
  try {
    const { data } = await api.get('/training-records/all')
    trainingRecords.value = data.data || []
  } catch (error) {
    console.error('加载培训记录失败', error)
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
  formDialog.assessmentType = ''
  formDialog.assessmentDate = new Date().toISOString().slice(0, 16)
  formDialog.assessorIds = ''
  formDialog.grade = ''
  formDialog.evaluation = ''
  formDialog.trainingRecordId = null
  formDialog.open = true
  loadTrainingRecords()
}

function openEditDialog(record) {
  formDialog.isEdit = true
  formDialog.id = record.id
  formDialog.title = record.title
  formDialog.assessmentType = record.assessmentType || ''
  formDialog.assessmentDate = record.assessmentDate ? new Date(record.assessmentDate).toISOString().slice(0, 16) : ''
  formDialog.assessorIds = record.assessorIds || ''
  formDialog.grade = record.grade || ''
  formDialog.evaluation = record.evaluation || ''
  formDialog.trainingRecordId = record.trainingRecordId
  formDialog.open = true
  loadTrainingRecords()
}

async function handleSubmit() {
  if (!formDialog.title.trim()) {
    ElMessage.warning('请输入考核主题')
    return
  }
  if (!formDialog.assessmentDate) {
    ElMessage.warning('请选择考核时间')
    return
  }
  if (!formDialog.assessmentType) {
    ElMessage.warning('请选择考核类型')
    return
  }
  if (!formDialog.grade) {
    ElMessage.warning('请选择考核等级')
    return
  }

  formDialog.loading = true
  try {
    const payload = {
      title: formDialog.title,
      assessmentType: formDialog.assessmentType,
      assessmentDate: new Date(formDialog.assessmentDate).toISOString(),
      assessorIds: formDialog.assessorIds,
      grade: formDialog.grade,
      evaluation: formDialog.evaluation,
      trainingRecordId: formDialog.trainingRecordId,
    }

    if (formDialog.isEdit) {
      await api.put(`/assessment-records/${formDialog.id}`, payload)
      ElMessage.success('更新成功')
    } else {
      await api.post('/assessment-records', payload)
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
    await ElMessageBox.confirm(`确定要删除考核记录 "${record.title}" 吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await api.delete(`/assessment-records/${record.id}`)
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
  filters.assessmentType = ''
  filters.grade = ''
  currentPage.value = 1
  loadRecords()
}

onMounted(() => {
  loadRecords()
})
</script>

<template>
  <div class="assessment-management">
    <div class="page-header">
      <div class="header-title">
        <h2>考核记录管理</h2>
        <p>管理人员考核结果，包括模拟售前、本地测试等</p>
      </div>
      <div class="header-stats">
        <el-statistic title="考核记录总数" :value="total" />
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索考核主题..."
        prefix-icon="Search"
        clearable
        style="width: 250px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select
        v-model="filters.assessmentType"
        placeholder="考核类型"
        clearable
        style="width: 130px; margin-left: 12px"
        @change="handleSearch"
      >
        <el-option
          v-for="option in assessmentTypeOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-select
        v-model="filters.grade"
        placeholder="考核等级"
        clearable
        style="width: 120px; margin-left: 12px"
        @change="handleSearch"
      >
        <el-option
          v-for="option in gradeOptions"
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
        新建考核记录
      </el-button>
    </div>

    <el-table
      :data="records"
      v-loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="title" label="考核主题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="assessmentType" label="考核类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.assessmentType || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="grade" label="考核等级" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.grade" size="small" :type="getGradeType(row.grade)">{{ row.grade }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="assessmentDate" label="考核时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.assessmentDate) }}
        </template>
      </el-table-column>
      <el-table-column prop="trainingRecordTitle" label="关联培训" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.trainingRecordTitle || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" label="创建人" width="100" show-overflow-tooltip />
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
    <el-empty v-if="!loading && !records.length" description="暂无考核记录" />
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
      :title="formDialog.isEdit ? '编辑考核记录' : '新建考核记录'"
      width="700px"
      destroy-on-close
      append-to-body
    >
      <el-form label-position="top" :model="formDialog">
        <el-form-item label="考核主题" required>
          <el-input v-model="formDialog.title" placeholder="请输入考核主题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考核类型">
              <el-select v-model="formDialog.assessmentType" placeholder="请选择考核类型" style="width: 100%">
                <el-option
                  v-for="option in assessmentTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核等级">
              <el-select v-model="formDialog.grade" placeholder="请选择考核等级" style="width: 100%">
                <el-option
                  v-for="option in gradeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="考核时间" required>
          <el-date-picker
            v-model="formDialog.assessmentDate"
            type="datetime"
            placeholder="选择考核时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm"
          />
        </el-form-item>
        <el-form-item label="关联培训记录">
          <el-select v-model="formDialog.trainingRecordId" placeholder="请选择关联的培训记录（可选）" clearable style="width: 100%">
            <el-option
              v-for="record in trainingRecords"
              :key="record.id"
              :label="record.title"
              :value="record.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="考核评价">
          <el-input
            v-model="formDialog.evaluation"
            type="textarea"
            :rows="6"
            placeholder="请输入考核评价/备注"
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
.assessment-management {
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
