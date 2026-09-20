<script setup>
import { computed, onBeforeUnmount, watch } from 'vue'
import {
  CONTACT_DECISION_OPTIONS,
  CONTACT_GENDER_OPTIONS,
  CUSTOMER_INDUSTRY_OPTIONS,
  CUSTOMER_LEVEL_OPTIONS,
  CUSTOMER_REGION_OPTIONS,
  CUSTOMER_SOURCE_OPTIONS,
  CUSTOMER_STAGE_OPTIONS,
  CUSTOMER_STATUS_OPTIONS,
  CUSTOMER_TYPE_OPTIONS,
  FOLLOWUP_RESULT_OPTIONS,
  FOLLOWUP_TYPE_OPTIONS,
} from '../constants/customerOptions'

const props = defineProps({
  loading: { type: Boolean, default: false },
  customers: { type: Array, required: true },
  selectedCustomer: { type: Object, default: null },
  contacts: { type: Array, required: true },
  followups: { type: Array, required: true },
  customerFilters: { type: Object, required: true },
  customerForm: { type: Object, required: true },
  contactForm: { type: Object, required: true },
  followupForm: { type: Object, required: true },
  customerDialogOpen: { type: Boolean, default: false },
  contactDialogOpen: { type: Boolean, default: false },
  followupDialogOpen: { type: Boolean, default: false },
  customerSaving: { type: Boolean, default: false },
  contactSaving: { type: Boolean, default: false },
  followupSaving: { type: Boolean, default: false },
  dictionaryOptions: { type: Object, default: () => ({}) },
  projects: { type: Array, default: () => [] },
  customerPage: { type: Number, default: 1 },
  customerPageSize: { type: Number, default: 20 },
  customerTotal: { type: Number, default: 0 },
})

const relatedProjects = computed(() => {
  if (!props.selectedCustomer) return []
  const name = props.selectedCustomer.name
  if (!name) return []
  return (props.projects || []).filter(p => p.customerName === name)
})

function optionValues(key, fallback) {
  const values = props.dictionaryOptions?.[key]
  return Array.isArray(values) && values.length ? values : fallback
}

const emit = defineEmits([
  'filter', 'reset-filters', 'select-customer', 'open-customer', 'delete-customer',
  'open-contact', 'delete-contact', 'save-customer', 'close-customer', 'save-contact', 'close-contact',
  'open-followup', 'delete-followup', 'save-followup', 'close-followup',
  'page-change', 'size-change',
])

function healthTagType(level) {
  if (level === '健康') return 'success'
  if (level === '一般') return 'warning'
  return 'danger'
}

let filterTimer = null
function triggerFilterWithDebounce() {
  if (filterTimer) clearTimeout(filterTimer)
  filterTimer = setTimeout(() => emit('filter'), 260)
}

watch(
  () => [
    props.customerFilters.keyword,
    props.customerFilters.ownerName,
    props.customerFilters.status,
    props.customerFilters.level,
    props.customerFilters.region,
    props.customerFilters.decisionLevel,
  ],
  triggerFilterWithDebounce
)

onBeforeUnmount(() => {
  if (filterTimer) clearTimeout(filterTimer)
})

function followupStatus(dateText) {
  if (!dateText) return { text: '未设置', type: 'info' }
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  const target = new Date(dateText)
  target.setHours(0, 0, 0, 0)
  const diff = Math.round((target.getTime() - now.getTime()) / 86400000)
  if (Number.isNaN(diff)) return { text: '待核实', type: 'info' }
  if (diff < 0) return { text: `已逾期 ${Math.abs(diff)} 天`, type: 'danger' }
  if (diff <= 3) return { text: `${diff === 0 ? '今天' : diff + ' 天内'}跟进`, type: 'warning' }
  return { text: `剩余 ${diff} 天`, type: 'success' }
}
</script>

<template>
  <section class="page-section crm-page">
    <el-card shadow="never" class="panel-card crm-page-card">
      <template #header>
        <div class="panel-head-row crm-page-head">
          <div class="crm-page-head-text">
            <h3>客户管理（CRM）</h3>
            <p>客户档案 · 联系人 · 跟进 · 健康度</p>
          </div>
          <el-button type="primary" size="small" @click="emit('open-customer')">新建客户</el-button>
        </div>
      </template>

      <el-form inline class="cms-filter-grid crm-filter-grid">
        <el-form-item label="关键词" class="crm-filter-item"><el-input v-model="props.customerFilters.keyword" size="small" placeholder="客户名/行业/标签" clearable @keyup.enter="emit('filter')" /></el-form-item>
        <el-form-item label="负责人" class="crm-filter-item"><el-input v-model="props.customerFilters.ownerName" size="small" placeholder="负责人" clearable @keyup.enter="emit('filter')" /></el-form-item>
        <el-form-item label="状态" class="crm-filter-item">
          <el-select v-model="props.customerFilters.status" placeholder="全部" clearable size="small" style="width: 100px">
            <el-option v-for="item in optionValues('customerStatus', CUSTOMER_STATUS_OPTIONS)" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级" class="crm-filter-item">
          <el-select v-model="props.customerFilters.level" placeholder="全部" clearable size="small" style="width: 80px">
            <el-option v-for="item in optionValues('customerLevel', CUSTOMER_LEVEL_OPTIONS)" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域" class="crm-filter-item">
          <el-select v-model="props.customerFilters.region" placeholder="全部" clearable size="small" style="width: 96px">
            <el-option v-for="item in optionValues('customerRegion', CUSTOMER_REGION_OPTIONS)" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item class="crm-filter-item crm-filter-actions">
          <el-button type="primary" size="small" @click="emit('filter')">筛选</el-button>
          <el-button size="small" @click="emit('reset-filters')">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="crm-main-grid">
        <div class="crm-list-col">
          <el-table
            :data="props.customers"
            v-loading="props.loading"
            highlight-current-row
            row-key="id"
            class="crm-customer-table"
            fit
            table-layout="fixed"
            style="width: 100%"
            @current-change="emit('select-customer', $event)"
            empty-text="暂无客户数据"
          >
            <el-table-column prop="name" label="客户公司" min-width="160" show-overflow-tooltip />
            <el-table-column prop="industry" label="行业" min-width="72" show-overflow-tooltip>
              <template #default="{ row }">{{ row.industry || '—' }}</template>
            </el-table-column>
            <el-table-column prop="level" label="等级" width="64" align="center">
              <template #default="{ row }">{{ row.level || '—' }}</template>
            </el-table-column>
            <el-table-column prop="ownerName" label="负责人" min-width="80" show-overflow-tooltip>
              <template #default="{ row }">{{ row.ownerName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="nextFollowupDate" label="下次跟进" min-width="88">
              <template #default="{ row }">{{ row.nextFollowupDate || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right" align="center" class-name="crm-actions-col">
              <template #default="{ row }">
                <div class="crm-row-actions">
                  <el-button link type="primary" size="small" @click="emit('open-followup', { customerId: row.id })">跟进</el-button>
                  <el-button link type="primary" size="small" @click="emit('open-customer', row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="emit('delete-customer', row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="props.customerTotal > 0" class="crm-list-pagination">
            <span class="crm-page-total">共 {{ props.customerTotal }} 条</span>
            <el-pagination
              background
              size="small"
              layout="sizes, prev, pager, next"
              :total="props.customerTotal"
              :current-page="props.customerPage"
              :page-size="props.customerPageSize"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="emit('page-change', $event)"
              @size-change="emit('size-change', $event)"
            />
          </div>
        </div>

        <div class="crm-side-col">
          <el-card shadow="never" class="panel-card crm-side-card crm-contacts-card">
            <template #header>
              <div class="panel-head-row crm-subhead-row">
                <span class="crm-side-title">联系人</span>
                <el-space class="crm-head-actions" :size="6">
                  <el-select v-model="props.customerFilters.decisionLevel" placeholder="决策级别" clearable size="small" style="width: 108px">
                    <el-option v-for="item in optionValues('contactDecisionLevel', CONTACT_DECISION_OPTIONS)" :key="item" :label="item" :value="item" />
                  </el-select>
                  <el-button type="primary" size="small" @click="emit('open-contact')" :disabled="!props.selectedCustomer">新增联系人</el-button>
                </el-space>
              </div>
            </template>
            <div v-if="!props.selectedCustomer" class="crm-side-empty">
              <span>在左侧点击客户公司后查看联系人</span>
            </div>
            <el-empty v-else-if="!props.contacts.length" description="暂无联系人" :image-size="48" />
            <el-table v-else :data="props.contacts" size="small" class="crm-side-table">
              <el-table-column prop="name" label="姓名" width="72" />
              <el-table-column prop="department" label="部门" min-width="80" show-overflow-tooltip>
                <template #default="{ row }">{{ row.department || '—' }}</template>
              </el-table-column>
              <el-table-column prop="position" label="职位" min-width="80" show-overflow-tooltip>
                <template #default="{ row }">{{ row.position || '—' }}</template>
              </el-table-column>
              <el-table-column prop="mobile" label="手机" min-width="100" show-overflow-tooltip>
                <template #default="{ row }">{{ row.mobile || '—' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="88" fixed="right" align="center">
                <template #default="{ row }">
                  <div class="crm-row-actions">
                    <el-button link type="primary" size="small" @click="emit('open-contact', row)">编辑</el-button>
                    <el-button link type="danger" size="small" @click="emit('delete-contact', row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card shadow="never" class="panel-card crm-side-card crm-followup-card">
            <template #header>
              <div class="panel-head-row crm-subhead-row">
                <span class="crm-side-title">跟进时间线</span>
                <el-button class="crm-head-actions" type="primary" size="small" @click="emit('open-followup')" :disabled="!props.selectedCustomer">新增跟进</el-button>
              </div>
            </template>
            <div v-if="!props.selectedCustomer" class="crm-side-empty">
              <span>选择客户后显示跟进记录</span>
            </div>
            <el-empty v-else-if="!props.followups.length" description="暂无跟进记录" :image-size="48" />
            <div v-else class="crm-timeline-list">
              <div v-for="item in props.followups" :key="item.id" class="crm-timeline-item">
                <div class="crm-timeline-head">
                  <strong>{{ item.followupType }}</strong>
                  <el-tag size="small" :type="followupStatus(item.nextFollowupDate).type">{{ followupStatus(item.nextFollowupDate).text }}</el-tag>
                </div>
                <div class="crm-timeline-meta">
                  {{ item.resultLevel || '中' }} · {{ item.ownerName || '—' }} · 下次 {{ item.nextFollowupDate || '—' }}
                </div>
                <p class="crm-timeline-content">{{ item.content || '—' }}</p>
                <div class="crm-row-actions crm-timeline-actions">
                  <el-button link type="primary" size="small" @click="emit('open-followup', item)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="emit('delete-followup', item)">删除</el-button>
                </div>
              </div>
            </div>
          </el-card>

          <el-card v-if="props.selectedCustomer" shadow="never" class="panel-card crm-side-card crm-projects-card">
            <template #header>
              <div class="panel-head-row crm-subhead-row">
                <span class="crm-side-title">关联项目</span>
                <span class="crm-side-count">{{ relatedProjects.length }}</span>
              </div>
            </template>
            <el-empty v-if="!relatedProjects.length" description="暂无关联项目" :image-size="40" />
            <el-table v-else :data="relatedProjects" stripe size="small" class="crm-side-table">
              <el-table-column prop="name" label="项目" min-width="140" show-overflow-tooltip />
              <el-table-column prop="stage" label="阶段" width="90" show-overflow-tooltip>
                <template #default="{ row }">{{ row.stage || '—' }}</template>
              </el-table-column>
              <el-table-column prop="progress" label="进度" width="90">
                <template #default="scope">
                  <el-progress :percentage="scope.row.progress || 0" :stroke-width="5" :show-text="false" />
                  <small class="crm-progress-text">{{ scope.row.progress || 0 }}%</small>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </div>
    </el-card>

    <el-dialog :model-value="props.customerDialogOpen" title="客户公司" width="760px" @close="emit('close-customer')">
      <el-form label-width="88px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="公司名称" required><el-input v-model="props.customerForm.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="简称"><el-input v-model="props.customerForm.shortName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="行业"><el-select v-model="props.customerForm.industry" placeholder="请选择"><el-option v-for="item in optionValues('customerIndustry', CUSTOMER_INDUSTRY_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="客户类型"><el-select v-model="props.customerForm.customerType" placeholder="请选择"><el-option v-for="item in optionValues('customerType', CUSTOMER_TYPE_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="等级"><el-select v-model="props.customerForm.level" placeholder="请选择"><el-option v-for="item in optionValues('customerLevel', CUSTOMER_LEVEL_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="区域"><el-select v-model="props.customerForm.region" placeholder="请选择"><el-option v-for="item in optionValues('customerRegion', CUSTOMER_REGION_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="负责人"><el-input v-model="props.customerForm.ownerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="props.customerForm.mainPhone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="props.customerForm.email" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="网站"><el-input v-model="props.customerForm.website" placeholder="https://..." /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="来源"><el-select v-model="props.customerForm.source" placeholder="请选择"><el-option v-for="item in optionValues('customerSource', CUSTOMER_SOURCE_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-select v-model="props.customerForm.status" placeholder="请选择"><el-option v-for="item in optionValues('customerStatus', CUSTOMER_STATUS_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="合作阶段"><el-select v-model="props.customerForm.cooperationStage" placeholder="请选择"><el-option v-for="item in optionValues('customerStage', CUSTOMER_STAGE_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="地址"><el-input v-model="props.customerForm.address" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="标签"><el-input v-model="props.customerForm.tags" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="props.customerForm.notes" type="textarea" :rows="3" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="emit('close-customer')">取消</el-button>
        <el-button type="primary" :loading="props.customerSaving" @click="emit('save-customer')">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="props.contactDialogOpen" title="联系人" width="680px" @close="emit('close-contact')">
      <el-form label-width="88px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="props.contactForm.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="性别"><el-select v-model="props.contactForm.gender" placeholder="请选择" clearable><el-option v-for="item in optionValues('contactGender', CONTACT_GENDER_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="部门"><el-input v-model="props.contactForm.department" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="职位"><el-input v-model="props.contactForm.position" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="决策级别"><el-select v-model="props.contactForm.decisionLevel" placeholder="请选择"><el-option v-for="item in optionValues('contactDecisionLevel', CONTACT_DECISION_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机"><el-input v-model="props.contactForm.mobile" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="办公电话"><el-input v-model="props.contactForm.officePhone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="props.contactForm.email" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="微信"><el-input v-model="props.contactForm.wechat" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="QQ"><el-input v-model="props.contactForm.qq" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="props.contactForm.notes" type="textarea" :rows="3" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="主要联系人"><el-switch v-model="props.contactForm.primaryContact" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="emit('close-contact')">取消</el-button>
        <el-button type="primary" :loading="props.contactSaving" @click="emit('save-contact')">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="props.followupDialogOpen" title="跟进记录" width="680px" @close="emit('close-followup')">
      <el-form label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="跟进类型" required><el-select v-model="props.followupForm.followupType"><el-option v-for="item in optionValues('followupType', FOLLOWUP_TYPE_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结果等级"><el-select v-model="props.followupForm.resultLevel"><el-option v-for="item in optionValues('followupResult', FOLLOWUP_RESULT_OPTIONS)" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="负责人"><el-input v-model="props.followupForm.ownerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="本次跟进时间"><el-date-picker v-model="props.followupForm.followupTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="下次跟进"><el-date-picker v-model="props.followupForm.nextFollowupDate" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="跟进内容" required><el-input v-model="props.followupForm.content" type="textarea" :rows="4" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="emit('close-followup')">取消</el-button>
        <el-button type="primary" :loading="props.followupSaving" @click="emit('save-followup')">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.crm-page-head {
  margin: 0;
  align-items: center;
}

.crm-page-head-text h3 {
  margin: 0;
  font-size: 16px;
}

.crm-page-head-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #6b7c93;
}

.crm-filter-grid {
  margin-bottom: 6px !important;
}

.crm-filter-grid .crm-filter-item {
  margin-right: 8px;
  margin-bottom: 6px;
}

.crm-filter-grid .el-form-item__label {
  font-size: 12px;
  color: #5f6f84;
}

.crm-row-actions {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  gap: 0 !important;
  white-space: nowrap !important;
  overflow: visible !important;
}

.crm-row-actions .el-button {
  margin: 0 !important;
  padding: 0 3px !important;
  font-size: 12px !important;
  min-width: auto !important;
}

.crm-list-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 0 0;
  flex: 0 0 auto;
}

.crm-page-total {
  font-size: 12px;
  color: #6b7c93;
  white-space: nowrap;
}

.crm-side-title {
  font-weight: 600;
  font-size: 13px;
  color: #1a2b40;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 58%;
}

.crm-side-count {
  margin-left: 4px;
  font-size: 12px;
  color: #6b7c93;
}

.crm-side-empty {
  min-height: 40px;
  display: grid;
  place-items: center;
  color: #8a98ab;
  font-size: 12px;
  background: #f7fafd;
  border: 1px dashed #d7e3f1;
  border-radius: 6px;
}

.crm-subhead-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
}

.crm-head-actions {
  margin-left: auto;
}

.crm-timeline-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.crm-timeline-item {
  padding: 6px 8px;
  border: 1px solid #e7eef8;
  border-radius: 6px;
  background: #fbfcfe;
}

.crm-timeline-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.crm-timeline-head strong {
  font-size: 13px;
  color: #0f2f5c;
}

.crm-timeline-meta {
  margin-top: 2px;
  font-size: 11px;
  color: #6b7c93;
}

.crm-timeline-content {
  margin: 4px 0 2px;
  font-size: 12px;
  color: #334861;
  line-height: 1.4;
}

.crm-progress-text {
  display: inline-block;
  margin-left: 4px;
  color: #6b7c93;
  font-size: 11px;
}
</style>
