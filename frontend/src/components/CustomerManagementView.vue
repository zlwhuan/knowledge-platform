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
  <section class="page-section">
    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-head-row">
          <div>
            <h3>客户管理（CRM）</h3>
            <p>客户档案、联系人、跟进时间线、健康度</p>
          </div>
          <el-button type="primary" @click="emit('open-customer')">新建客户</el-button>
        </div>
      </template>

      <el-form inline class="cms-filter-grid">
        <el-form-item label="关键词"><el-input v-model="props.customerFilters.keyword" placeholder="客户名/行业/标签" clearable @keyup.enter="emit('filter')" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="props.customerFilters.ownerName" placeholder="销售/负责人" clearable @keyup.enter="emit('filter')" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="props.customerFilters.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in optionValues('customerStatus', CUSTOMER_STATUS_OPTIONS)" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="props.customerFilters.level" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="item in optionValues('customerLevel', CUSTOMER_LEVEL_OPTIONS)" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域">
          <el-select v-model="props.customerFilters.region" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in optionValues('customerRegion', CUSTOMER_REGION_OPTIONS)" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="emit('filter')">立即筛选</el-button>
          <el-button @click="emit('reset-filters')">重置</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="12">
        <el-col :xs="24" :lg="15">
          <el-table :data="props.customers" v-loading="props.loading" highlight-current-row row-key="id" :max-height="620" @current-change="emit('select-customer', $event)">
            <el-table-column prop="name" label="客户公司" min-width="170" show-overflow-tooltip />
            <el-table-column prop="industry" label="行业" width="90" />
            <el-table-column prop="customerType" label="客户类型" min-width="110" show-overflow-tooltip />
            <el-table-column prop="level" label="等级" width="70" />
            <el-table-column prop="region" label="区域" width="90" />
            <el-table-column prop="ownerName" label="负责人" width="90" />
            <el-table-column prop="mainPhone" label="电话" min-width="120" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="cooperationStage" label="阶段" min-width="110" show-overflow-tooltip />
            <el-table-column label="健康度" width="90">
              <template #default="{ row }">
                <el-tag :type="healthTagType(row.healthLevel)">{{ row.healthLevel || '关注' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="nextFollowupDate" label="下次跟进" width="110" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="emit('open-followup', { customerId: row.id })">跟进</el-button>
                <el-button text type="primary" @click="emit('open-customer', row)">编辑</el-button>
                <el-button text type="danger" @click="emit('delete-customer', row)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-col>

        <el-col :xs="24" :lg="9">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-head-row crm-subhead-row">
                <span>联系人</span>
                <el-space class="crm-head-actions">
                  <el-select v-model="props.customerFilters.decisionLevel" placeholder="决策级别" clearable style="width: 120px">
                    <el-option v-for="item in optionValues('contactDecisionLevel', CONTACT_DECISION_OPTIONS)" :key="item" :label="item" :value="item" />
                  </el-select>
                  <el-button type="primary" size="small" @click="emit('open-contact')">新增联系人</el-button>
                </el-space>
              </div>
            </template>
            <el-empty v-if="!props.selectedCustomer" description="请先选择客户公司" />
            <el-table v-else :data="props.contacts" size="small">
              <el-table-column prop="name" label="姓名" width="80" />
              <el-table-column prop="department" label="部门" min-width="90" />
              <el-table-column prop="position" label="职位" min-width="90" />
              <el-table-column prop="decisionLevel" label="级别" width="70" />
              <el-table-column prop="mobile" label="手机" min-width="100" />
              <el-table-column label="主联系人" width="70">
                <template #default="{ row }">{{ row.primaryContact ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="emit('open-contact', row)">编</el-button>
                  <el-button text type="danger" @click="emit('delete-contact', row)">删</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card shadow="never" class="panel-card" style="margin-top: 12px">
            <template #header>
              <div class="panel-head-row crm-subhead-row">
                <span>跟进时间线</span>
                <el-button class="crm-head-actions" type="primary" size="small" @click="emit('open-followup')">新增跟进</el-button>
              </div>
            </template>
            <el-empty v-if="!props.selectedCustomer" description="请先选择客户公司" />
            <el-empty v-else-if="!props.followups.length" description="暂无跟进记录，建议先补一条" />
            <el-timeline v-else>
              <el-timeline-item v-for="item in props.followups" :key="item.id" :timestamp="item.followupTime" placement="top">
                <div style="display:flex;justify-content:space-between;gap:8px">
                  <div>
                    <strong>{{ item.followupType }}</strong> · {{ item.resultLevel || '中' }}
                    <p style="margin:6px 0 8px">{{ item.content }}</p>
                    <div class="followup-meta-row">
                      <small>负责人：{{ item.ownerName || '--' }} ｜ 下次：{{ item.nextFollowupDate || '--' }}</small>
                      <el-tag size="small" :type="followupStatus(item.nextFollowupDate).type">{{ followupStatus(item.nextFollowupDate).text }}</el-tag>
                    </div>
                  </div>
                  <div>
                    <el-button text type="primary" @click="emit('open-followup', item)">编</el-button>
                    <el-button text type="danger" @click="emit('delete-followup', item)">删</el-button>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-card>

          <el-card shadow="never" class="panel-card" style="margin-top: 12px;" v-if="selectedCustomer">
            <template #header>
              <h3>关联项目</h3>
            </template>
            <el-table :data="relatedProjects" stripe size="small" max-height="300">
              <el-table-column prop="name" label="项目名称" min-width="200" />
              <el-table-column prop="stage" label="阶段" width="120" />
              <el-table-column prop="status" label="状态" width="100" />
              <el-table-column prop="progress" label="进度" width="100">
                <template #default="scope">
                  <el-progress :percentage="scope.row.progress || 0" :stroke-width="6" />
                </template>
              </el-table-column>
              <el-table-column prop="riskLevel" label="风险" width="80" />
            </el-table>
            <el-empty v-if="!relatedProjects.length" description="暂无关联项目" :image-size="40" />
          </el-card>
        </el-col>
      </el-row>
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
.followup-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.crm-subhead-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
}

.crm-head-actions {
  margin-left: auto;
}
</style>
