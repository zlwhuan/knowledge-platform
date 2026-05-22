<script setup>
import { Delete } from '@element-plus/icons-vue'
import { computed, reactive, watch } from 'vue'
import {
  projectAcceptanceStatusOptions as projectAcceptanceStatusOptionsFallback,
  projectContractStatusOptions as projectContractStatusOptionsFallback,
  projectFormFields,
  projectPaymentStatusOptions as projectPaymentStatusOptionsFallback,
  projectRiskOptions as projectRiskOptionsFallback,
  projectServiceStatusOptions as projectServiceStatusOptionsFallback,
  projectStageOptions as projectStageOptionsFallback,
  projectStatusOptions as projectStatusOptionsFallback,
} from '../composables/projectDialogOptions'

const props = defineProps({
  dialogs: { type: Object, required: true },
  saving: { type: Object, required: true },
  forms: { type: Object, required: true },
  users: { type: Array, default: () => [] },
  customers: { type: Array, default: () => [] },
  currentUserName: { type: String, default: '' },
  dictionaryOptions: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['close-project-form', 'save-project', 'close-project-progress', 'save-project-progress'])

const submitAttempted = reactive({
  projectForm: false,
  projectProgress: false,
})

const requiredProjectFields = projectFormFields.filter((field) => field.required).map((field) => field.model)

const projectFormSections = [
  { key: 'basic', title: '基础信息', models: ['name', 'customerName', 'stage', 'status', 'progress', 'riskLevel'] },
  { key: 'owners', title: '负责人分工', models: ['salesOwner', 'projectManager', 'implementationOwner', 'documentOwner', 'serviceOwner'] },
  { key: 'contacts', title: '项目关联联系人', models: ['projectContactLinks'] },
  { key: 'commercial', title: '商务与状态', models: ['contractAmount', 'receivedAmount', 'contractStatus', 'paymentStatus', 'acceptanceStatus', 'serviceStatus'] },
  { key: 'schedule', title: '时间与节点', models: ['startDate', 'plannedEndDate', 'acceptanceDate', 'warrantyUntil'] },
  { key: 'desc', title: '补充说明', models: ['description'] },
]

const groupedProjectFormFields = computed(() => {
  const fieldMap = new Map(projectFormFields.map((field) => [field.model, field]))
  return Object.fromEntries(projectFormSections.map((section) => [
    section.key,
    section.models.map((model) => fieldMap.get(model)).filter(Boolean),
  ]))
})

const userOptions = computed(() => {
  const mapped = props.users
    .filter((user) => user && user.enabled !== false)
    .map((user) => {
      const value = String(user.displayName || user.username || '').trim()
      return {
        label: value || String(user.username || '').trim(),
        value: value || String(user.username || '').trim(),
      }
    })
    .filter((item) => item.value)

  const current = String(props.currentUserName || '').trim()
  if (current && !mapped.some((item) => item.value === current)) {
    mapped.unshift({ label: `${current}（当前用户）`, value: current })
  }
  return mapped
})

const selectedCustomer = computed(() => {
  const targetName = String(props.forms?.projectForm?.customerName || '').trim()
  if (!targetName) return null
  return props.customers.find((item) => String(item?.name || '').trim() === targetName) || null
})

const customerContactOptions = computed(() => (selectedCustomer.value?.contacts || [])
  .map((contact) => {
    const id = contact?.id
    if (id == null) return null
    const contactName = String(contact?.name || '').trim()
    if (!contactName) return null
    const customerName = String(selectedCustomer.value?.name || '').trim() || '--'
    const department = String(contact?.department || '').trim()
    const position = String(contact?.position || '').trim()
    const mobile = String(contact?.mobile || '').trim()
    return {
      key: `${customerName}::${id}`,
      customerName,
      contactId: String(id),
      contactName,
      department,
      position,
      mobile,
      label: contactName,
      optionText: `${contactName}｜${position || '--'}`,
    }
  })
  .filter(Boolean))

const relatedCustomerTypeOptions = computed(() => {
  const result = []
  const seen = new Set()
  props.customers.forEach((customer) => {
    const type = String(customer?.customerType || '').trim()
    if (!type || seen.has(type)) return
    seen.add(type)
    result.push({ label: type, value: type })
  })
  return result
})

function relatedCustomerOptionsByType(customerType) {
  const targetType = String(customerType || '').trim()
  return props.customers
    .filter((customer) => !targetType || String(customer?.customerType || '').trim() === targetType)
    .map((customer) => {
      const name = String(customer?.name || '').trim()
      if (!name) return null
      return { label: name, value: name }
    })
    .filter(Boolean)
}

function findCustomerByName(name) {
  const targetName = String(name || '').trim()
  if (!targetName) return null
  return props.customers.find((item) => String(item?.name || '').trim() === targetName) || null
}

const allRelatedContactOptions = computed(() => props.customers
  .flatMap((customer) => {
    const customerName = String(customer?.name || '').trim()
    const customerType = String(customer?.customerType || '').trim()
    if (!customerName) return []
    return (customer?.contacts || [])
      .map((contact) => {
        const id = contact?.id
        if (id == null) return null
        const contactName = String(contact?.name || '').trim()
        if (!contactName) return null
        const department = String(contact?.department || '').trim()
        const position = String(contact?.position || '').trim()
        const mobile = String(contact?.mobile || '').trim()
        return {
          key: `${customerName}::${id}`,
          customerName,
          customerType,
          contactId: String(id),
          contactName,
          department,
          position,
          mobile,
          label: contactName,
          optionText: `${contactName}｜${position || '--'}｜${customerName}`,
        }
      })
      .filter(Boolean)
  }))

function relatedContactOptions(rowOrCustomerName) {
  const row = typeof rowOrCustomerName === 'object' && rowOrCustomerName !== null
    ? rowOrCustomerName
    : { relatedCustomerName: rowOrCustomerName }

  const targetCustomerName = String(row?.relatedCustomerName || '').trim()
  const targetCustomerType = String(row?.relatedCustomerType || '').trim()

  return allRelatedContactOptions.value.filter((item) => {
    if (targetCustomerName) return item.customerName === targetCustomerName
    if (targetCustomerType) return item.customerType === targetCustomerType
    return true
  })
}

function addCustomerContactLink(forms) {
  if (!Array.isArray(forms.projectForm.customerContactLinks)) forms.projectForm.customerContactLinks = []
  forms.projectForm.customerContactLinks.push({
    sourceType: 'customer',
    contactKey: '',
    customerName: String(selectedCustomer.value?.name || '').trim(),
    contactId: '',
    contactName: '',
    department: '',
    position: '',
    mobile: '',
  })
}

function addRelatedContactLink(forms) {
  if (!Array.isArray(forms.projectForm.relatedContactLinks)) forms.projectForm.relatedContactLinks = []
  forms.projectForm.relatedContactLinks.push({
    sourceType: 'related',
    relatedCustomerType: '',
    relatedCustomerName: '',
    contactKey: '',
    customerName: '',
    contactId: '',
    contactName: '',
    department: '',
    position: '',
    mobile: '',
  })
}

function syncProjectCustomerType(forms) {
  const customerName = String(forms?.projectForm?.customerName || '').trim()
  const customer = findCustomerByName(customerName)
  forms.projectForm.customerType = String(customer?.customerType || '').trim()
}

function onCustomerContactSelect(forms, index) {
  const current = forms.projectForm.customerContactLinks[index]
  if (!current) return
  const target = customerContactOptions.value.find((item) => item.key === current.contactKey)
  if (!target) return
  Object.assign(current, {
    sourceType: 'customer',
    customerName: target.customerName,
    contactId: target.contactId,
    contactName: target.contactName,
    department: target.department,
    position: target.position,
    mobile: target.mobile,
  })
  if (!String(forms.projectForm.customerName || '').trim()) {
    forms.projectForm.customerName = target.customerName
  }
  syncProjectCustomerType(forms)
}

function onRelatedCustomerTypeChange(forms, index) {
  const current = forms.projectForm.relatedContactLinks[index]
  if (!current) return

  const selectedCustomer = findCustomerByName(current.relatedCustomerName)
  const selectedType = String(current.relatedCustomerType || '').trim()
  const customerType = String(selectedCustomer?.customerType || '').trim()

  if (selectedCustomer && selectedType && customerType !== selectedType) {
    current.relatedCustomerName = ''
    current.customerName = ''
    current.contactKey = ''
    current.contactId = ''
    current.contactName = ''
    current.department = ''
    current.position = ''
    current.mobile = ''
    return
  }

  if (current.contactKey) {
    const options = relatedContactOptions(current)
    const stillExists = options.some((item) => item.key === current.contactKey)
    if (!stillExists) {
      current.contactKey = ''
      current.contactId = ''
      current.contactName = ''
      current.department = ''
      current.position = ''
      current.mobile = ''
    }
  }
}

function resolveRelatedRow(forms, rowOrIndex) {
  if (typeof rowOrIndex === 'object' && rowOrIndex) return rowOrIndex
  const index = Number(rowOrIndex)
  if (Number.isNaN(index)) return null
  return forms?.projectForm?.relatedContactLinks?.[index] || null
}

function syncRelatedRowCustomerType(forms, rowOrIndex) {
  const row = resolveRelatedRow(forms, rowOrIndex)
  if (!row) return
  const name = String(row.relatedCustomerName || row.customerName || '').trim()
  const selectedCustomer = findCustomerByName(name)
  row.relatedCustomerType = String(selectedCustomer?.customerType || row.relatedCustomerType || '').trim()
}

function onRelatedCustomerChange(forms, rowOrIndex) {
  const current = resolveRelatedRow(forms, rowOrIndex)
  if (!current) return

  const selectedCustomer = findCustomerByName(current.relatedCustomerName)
  current.relatedCustomerType = String(selectedCustomer?.customerType || '').trim()

  if (current.contactKey) {
    const options = relatedContactOptions(current)
    const stillExists = options.some((item) => item.key === current.contactKey)
    if (!stillExists) {
      current.contactKey = ''
      current.contactId = ''
      current.contactName = ''
      current.department = ''
      current.position = ''
      current.mobile = ''
    }
  }

  current.customerName = String(current.relatedCustomerName || '').trim()
  syncRelatedRowCustomerType(forms, current)
}

function onRelatedContactSelect(forms, rowOrIndex) {
  const current = resolveRelatedRow(forms, rowOrIndex)
  if (!current) return
  const options = relatedContactOptions(current)
  const target = options.find((item) => item.key === current.contactKey)
  if (!target) return
  Object.assign(current, {
    sourceType: 'related',
    relatedCustomerType: target.customerType || '',
    relatedCustomerName: target.customerName,
    customerName: target.customerName,
    contactId: target.contactId,
    contactName: target.contactName,
    department: target.department,
    position: target.position,
    mobile: target.mobile,
  })
  syncRelatedRowCustomerType(forms, current)
}

function removeCustomerContactLink(forms, index) {
  forms.projectForm.customerContactLinks.splice(index, 1)
}

function removeRelatedContactLink(forms, index) {
  forms.projectForm.relatedContactLinks.splice(index, 1)
}

function normalizeRelatedCustomerType() {
  const rows = props.forms?.projectForm?.relatedContactLinks
  if (!Array.isArray(rows) || !rows.length) return
  const customerTypeMap = new Map(
    props.customers
      .map((customer) => [String(customer?.name || '').trim(), String(customer?.customerType || '').trim()])
      .filter(([name]) => Boolean(name)),
  )

  rows.forEach((row) => {
    if (!row) return
    const name = String(row.relatedCustomerName || row.customerName || '').trim()
    if (!name) return
    if (!String(row.relatedCustomerName || '').trim()) row.relatedCustomerName = name
    if (!String(row.relatedCustomerType || '').trim()) {
      row.relatedCustomerType = customerTypeMap.get(name) || ''
    }
  })
}

watch(
  () => [props.dialogs?.projectFormOpen, props.customers?.length, props.forms?.projectForm?.relatedContactLinks?.length],
  () => {
    normalizeRelatedCustomerType()
    syncProjectCustomerType(props.forms)
  },
  { immediate: true },
)

watch(
  () => props.forms?.projectForm?.relatedContactLinks,
  (rows) => {
    if (!Array.isArray(rows)) return
    rows.forEach((row) => {
      if (!row) return
      const key = String(row.contactKey || '').trim()
      if (key) {
        const target = allRelatedContactOptions.value.find((item) => item.key === key)
        if (target) {
          row.relatedCustomerName = target.customerName
          row.customerName = target.customerName
          row.relatedCustomerType = target.customerType || row.relatedCustomerType || ''
        }
      }
      syncRelatedRowCustomerType(props.forms, row)
    })
  },
  { deep: true, immediate: true },
)

watch(
  () => props.forms?.projectForm?.customerName,
  () => syncProjectCustomerType(props.forms),
)

const customerOptions = computed(() => {
  const mapped = props.customers
    .map((customer) => {
      const name = String(customer?.name || '').trim()
      const shortName = String(customer?.shortName || '').trim()
      if (!name) return null
      return {
        label: shortName && shortName !== name ? `${name}（${shortName}）` : name,
        value: name,
      }
    })
    .filter(Boolean)

  const uniqueByName = []
  const seen = new Set()
  mapped.forEach((item) => {
    if (seen.has(item.value)) return
    seen.add(item.value)
    uniqueByName.push(item)
  })
  uniqueByName.sort((a, b) => a.value.localeCompare(b.value, 'zh-Hans-CN'))

  const current = String(props.forms?.projectForm?.customerName || '').trim()
  if (current && !uniqueByName.some((item) => item.value === current)) {
    uniqueByName.unshift({ label: `${current}（当前值）`, value: current })
  }
  return uniqueByName
})

const stageOptions = computed(() => props.dictionaryOptions?.projectStage?.length ? props.dictionaryOptions.projectStage : projectStageOptionsFallback)
const statusOptions = computed(() => props.dictionaryOptions?.projectStatus?.length ? props.dictionaryOptions.projectStatus : projectStatusOptionsFallback)
const riskOptions = computed(() => props.dictionaryOptions?.projectRisk?.length ? props.dictionaryOptions.projectRisk : projectRiskOptionsFallback)

const statusOptionMap = computed(() => ({
  contractStatus: props.dictionaryOptions?.projectContractStatus?.length ? props.dictionaryOptions.projectContractStatus : projectContractStatusOptionsFallback,
  paymentStatus: props.dictionaryOptions?.projectPaymentStatus?.length ? props.dictionaryOptions.projectPaymentStatus : projectPaymentStatusOptionsFallback,
  acceptanceStatus: props.dictionaryOptions?.projectAcceptanceStatus?.length ? props.dictionaryOptions.projectAcceptanceStatus : projectAcceptanceStatusOptionsFallback,
  serviceStatus: props.dictionaryOptions?.projectServiceStatus?.length ? props.dictionaryOptions.projectServiceStatus : projectServiceStatusOptionsFallback,
}))

function resolveSelectOptions(field) {
  return statusOptionMap.value[field.optionsKey] || []
}

function isBlank(value) {
  return String(value ?? '').trim().length === 0
}

function fieldError(scope, value, label) {
  if (!submitAttempted[scope]) return ''
  if (!isBlank(value)) return ''
  return `请填写${label}`
}

function validateProjectForm(forms) {
  submitAttempted.projectForm = true
  return requiredProjectFields.every((key) => !isBlank(forms.projectForm[key]))
}

function validateProjectProgress(forms) {
  submitAttempted.projectProgress = true
  return ![
    isBlank(forms.projectProgressForm.stage),
    isBlank(forms.projectProgressForm.status),
    isBlank(forms.projectProgressForm.recordTime),
    isBlank(forms.projectProgressForm.summary),
  ].some(Boolean)
}


function resetSubmitState(scope) {
  if (!submitAttempted[scope]) return
  submitAttempted[scope] = false
}

function onSaveProject(forms) {
  if (!validateProjectForm(forms)) return
  emit('save-project')
}

function onSaveProjectProgress(forms) {
  if (!validateProjectProgress(forms)) return
  emit('save-project-progress')
}

function formatMoneyInput(value) {
  if (value === '' || value == null) return ''
  const [integerPart, decimalPart] = String(value).split('.')
  const integer = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return decimalPart != null ? `${integer}.${decimalPart}` : integer
}

function parseMoneyInput(value) {
  if (value === '' || value == null) return ''
  return String(value).replace(/,/g, '').trim()
}

watch(() => props.forms.projectProgressForm.status, (status, prevStatus) => {
  if (status !== '已完成') return
  if (Number(props.forms.projectProgressForm.progress || 0) !== 100) {
    props.forms.projectProgressForm.progress = 100
  }
  // 仅在状态“切换为已完成”时清空，避免打开编辑弹窗时误清空历史数据
  if (prevStatus && prevStatus !== '已完成') {
    props.forms.projectProgressForm.nextAction = ''
    props.forms.projectProgressForm.nextActionDueDate = ''
  }
})

watch(() => Number(props.forms.projectProgressForm.progress || 0), (progress, prevProgress) => {
  if (progress >= 100 && props.forms.projectProgressForm.status !== '已完成') {
    props.forms.projectProgressForm.status = '已完成'
    return
  }
  // 从100回调到<100时，若状态仍是已完成，自动切回进行中，允许继续维护下一步
  if (prevProgress >= 100 && progress < 100 && props.forms.projectProgressForm.status === '已完成') {
    props.forms.projectProgressForm.status = '进行中'
  }
})

</script>

<template>
  <el-dialog class="project-form-dialog" :model-value="dialogs.projectFormDialogOpen" :title="forms.projectForm.id ? '编辑项目' : '新建项目'" fullscreen @close="resetSubmitState('projectForm'); emit('close-project-form')">
    <div class="project-form-sections">
      <section v-for="section in projectFormSections" :key="section.key" class="project-form-section">
        <div class="project-form-section-title">{{ section.title }}</div>
        <el-form label-position="top" class="project-form-grid project-dialog-form-grid project-form-group-grid">
          <el-form-item
            v-for="field in groupedProjectFormFields[section.key]"
            :key="field.model"
            :label="field.component === 'project-contact-link-editor' ? '' : field.label"
            :required="field.required && field.component !== 'project-contact-link-editor'"
            :class="field.component === 'project-contact-link-editor' ? 'project-dialog-span-2' : (field.span === 2 ? 'project-dialog-span-2' : '')"
            :error="field.required && field.component !== 'project-contact-link-editor' ? fieldError('projectForm', forms.projectForm[field.model], field.label) : ''"
          >
            <el-select
              v-if="field.component === 'user-select'"
              v-model="forms.projectForm[field.model]"
              clearable
              filterable
              placeholder="请选择用户"
            >
              <el-option v-for="(item, index) in userOptions" :key="`${field.model}-${item.value}-${index}`" :label="item.label" :value="item.value" />
            </el-select>
            <el-select
              v-else-if="field.component === 'select'"
              v-model="forms.projectForm[field.model]"
              clearable
              filterable
              placeholder="请选择"
            >
              <el-option v-for="item in resolveSelectOptions(field)" :key="`${field.model}-${item.value}`" :label="item.label" :value="item.value" />
            </el-select>
            <el-select
              v-else-if="field.component === 'customer-select'"
              v-model="forms.projectForm[field.model]"
              clearable
              filterable
              placeholder="请选择客户（来自客户管理）"
            >
              <el-option v-for="item in customerOptions" :key="`${field.model}-${item.value}`" :label="item.label" :value="item.value" />
            </el-select>
            <el-input
              v-else-if="field.component === 'input'"
              v-model="forms.projectForm[field.model]"
              :maxlength="field.maxlength"
              :show-word-limit="field.wordLimit"
              :disabled="field.disabled"
            />
            <el-input
              v-else-if="field.component === 'textarea'"
              v-model="forms.projectForm[field.model]"
              type="textarea"
              :rows="field.rows || 4"
            />
            <div v-else-if="field.component === 'project-contact-link-editor'" class="project-contact-link-editor" style="display: flex; gap: 14px; flex-wrap: nowrap; align-items: flex-start; width: 100%;">
              <div class="project-contact-columns" style="display:flex;gap:14px;flex-wrap:nowrap;align-items:flex-start;width:100%;">
                <section class="project-contact-card project-contact-card-customer">
                  <header class="project-contact-card-header">
                    <strong>客户本身联系人</strong>
                    <el-button plain size="small" @click="addCustomerContactLink(forms)">添加</el-button>
                  </header>
                  <el-table :data="forms.projectForm.customerContactLinks" border table-layout="auto" empty-text="暂无客户联系人" style="width: 100%">
                    <el-table-column label="联系人" min-width="160" show-overflow-tooltip>
                      <template #default="scope">
                        <el-select v-model="scope.row.contactKey" filterable clearable placeholder="选择客户联系人" @change="onCustomerContactSelect(forms, scope.$index)">
                          <el-option
                            v-for="option in customerContactOptions"
                            :key="option.key"
                            :label="option.label"
                            :value="option.key"
                            :disabled="forms.projectForm.customerContactLinks.some((link, linkIndex) => linkIndex !== scope.$index && link.contactKey === option.key) || forms.projectForm.relatedContactLinks.some((link) => link.contactKey === option.key)"
                          >
                            <span>{{ option.optionText }}</span>
                          </el-option>
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="职位" min-width="110" show-overflow-tooltip>
                      <template #default="scope">{{ scope.row.position || '--' }}</template>
                    </el-table-column>
                    <el-table-column label="电话" min-width="130" show-overflow-tooltip>
                      <template #default="scope">{{ scope.row.mobile || '--' }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="68" fixed="right">
                      <template #default="scope">
                        <el-button link type="danger" @click="removeCustomerContactLink(forms, scope.$index)"><el-icon><Delete /></el-icon></el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </section>

                <section class="project-contact-card project-contact-card-related">
                  <header class="project-contact-card-header">
                    <strong>其它相关人员</strong>
                    <el-button plain size="small" @click="addRelatedContactLink(forms)">添加</el-button>
                  </header>
                  <el-table :data="forms.projectForm.relatedContactLinks" border table-layout="auto" empty-text="暂无其它相关人员" style="width: 100%">
                    <el-table-column label="客户类型" min-width="90" show-overflow-tooltip>
                      <template #default="scope">
                        <el-select v-model="scope.row.relatedCustomerType" filterable clearable placeholder="客户类型" @change="onRelatedCustomerTypeChange(forms, scope.$index)">
                          <el-option v-for="typeItem in relatedCustomerTypeOptions" :key="typeItem.value" :label="typeItem.label" :value="typeItem.value" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="客户名称" min-width="160" show-overflow-tooltip>
                      <template #default="scope">
                        <el-select v-model="scope.row.relatedCustomerName" filterable clearable placeholder="客户" @change="onRelatedCustomerChange(forms, scope.row)">
                          <el-option v-for="customer in relatedCustomerOptionsByType(scope.row.relatedCustomerType)" :key="customer.value" :label="customer.label" :value="customer.value" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="联系人" min-width="160" show-overflow-tooltip>
                      <template #default="scope">
                        <el-select v-model="scope.row.contactKey" filterable clearable placeholder="联系人" @change="onRelatedContactSelect(forms, scope.row)">
                          <el-option
                            v-for="option in relatedContactOptions(scope.row)"
                            :key="option.key"
                            :label="option.label"
                            :value="option.key"
                            :disabled="forms.projectForm.customerContactLinks.some((link) => link.contactKey === option.key) || forms.projectForm.relatedContactLinks.some((link, linkIndex) => linkIndex !== scope.$index && link.contactKey === option.key)"
                          >
                            <span>{{ option.optionText }}</span>
                          </el-option>
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="职位" min-width="110" show-overflow-tooltip>
                      <template #default="scope">{{ scope.row.position || '--' }}</template>
                    </el-table-column>
                    <el-table-column label="电话" min-width="130" show-overflow-tooltip>
                      <template #default="scope">{{ scope.row.mobile || '--' }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="68" fixed="right">
                      <template #default="scope">
                        <el-button link type="danger" @click="removeRelatedContactLink(forms, scope.$index)"><el-icon><Delete /></el-icon></el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </section>
              </div>
            </div>
            <el-input
              v-else-if="field.component === 'progress-text'"
              :model-value="`${forms.projectForm[field.model]}${field.suffix || ''}`"
              disabled
            />
            <el-input
              v-else-if="field.component === 'risk-text'"
              :model-value="forms.projectForm[field.model] || field.fallback || '--'"
              disabled
            />
            <el-input
              v-else-if="field.component === 'readonly-text'"
              :model-value="forms.projectForm[field.model] || field.fallback || '--'"
              disabled
            />
            <el-input-number
              v-else-if="field.component === 'number'"
              v-model="forms.projectForm[field.model]"
              :min="field.min ?? 0"
              :precision="field.precision ?? 0"
              :step="field.step ?? 1"
              :controls-position="field.step ? 'right' : undefined"
              :formatter="field.isMoney ? formatMoneyInput : undefined"
              :parser="field.isMoney ? parseMoneyInput : undefined"
            />
            <el-date-picker
              v-else-if="field.component === 'date'"
              v-model="forms.projectForm[field.model]"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
      </section>
    </div>
    <template #footer>
      <el-button @click="emit('close-project-form')">取消</el-button>
      <el-button type="primary" :loading="saving.projectFormSaving" @click="onSaveProject(forms)">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog :model-value="dialogs.projectProgressDialogOpen" :title="forms.projectProgressForm.id ? '编辑项目进度' : '推进项目进度（留痕）'" width="min(760px,92vw)" @close="resetSubmitState('projectProgress'); emit('close-project-progress')">
    <el-form label-position="top" class="project-form-grid project-activity-dialog-grid">
      <el-form-item label="推进阶段" required :error="fieldError('projectProgress', forms.projectProgressForm.stage, '推进阶段')">
        <el-select v-model="forms.projectProgressForm.stage">
          <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="当前状态" required :error="fieldError('projectProgress', forms.projectProgressForm.status, '当前状态')">
        <el-select v-model="forms.projectProgressForm.status" @change="onProgressStatusChange">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="完成进度">
        <el-input-number v-model="forms.projectProgressForm.progress" :min="0" :max="100" :step="5" @change="onProgressValueChange" />
      </el-form-item>
      <el-form-item label="风险等级">
        <el-select v-model="forms.projectProgressForm.riskLevel">
          <el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="责任人">
        <el-select v-model="forms.projectProgressForm.ownerName" filterable placeholder="请选择责任人">
          <el-option v-for="(item, index) in userOptions" :key="`progress-owner-${item.value}-${index}`" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="记录时间" required :error="fieldError('projectProgress', forms.projectProgressForm.recordTime, '记录时间')"><el-date-picker v-model="forms.projectProgressForm.recordTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
      <el-form-item label="本次进展摘要" class="project-dialog-span-2" required :error="fieldError('projectProgress', forms.projectProgressForm.summary, '本次进展摘要')"><el-input v-model="forms.projectProgressForm.summary" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
      <el-form-item v-if="forms.projectProgressForm.status !== '已完成'" label="下一步动作" class="project-dialog-span-2"><el-input v-model="forms.projectProgressForm.nextAction" /></el-form-item>
      <el-form-item v-if="forms.projectProgressForm.status !== '已完成'" label="下一步截止日期"><el-date-picker v-model="forms.projectProgressForm.nextActionDueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('close-project-progress')">取消</el-button>
      <el-button type="primary" :loading="saving.projectProgressSaving" @click="onSaveProjectProgress(forms)">保存进度</el-button>
    </template>
  </el-dialog>

</template>

<style scoped>
.project-contact-link-editor {
  display: grid;
  gap: 10px;
}

.project-contact-columns {
  display: flex;
  width: 100%;
  gap: 14px;
  align-items: start;
  flex-wrap: nowrap;
}

.project-contact-card {
  box-sizing: border-box;
  flex: 1 1 0 !important;
  width: auto !important;
  max-width: none !important;
  min-width: 0 !important;
  border: 1px solid #e4e9f2;
  border-radius: 10px;
  background: #fbfdff;
  overflow: hidden;
}

.project-contact-card-customer {
  flex: 0.75 1 0 !important;
}

.project-contact-card-related {
  flex: 1.25 1 0 !important;
}

.project-contact-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #edf1f7;
  background: #f6f9ff;
  color: #24344d;
}

.project-contact-card-body {
  display: grid;
  gap: 8px;
  padding: 10px 12px 12px;
  scrollbar-gutter: stable;
  overflow-x: hidden;
}

.project-contact-link-header,
.project-contact-link-row {
  display: grid;
  width: 100%;
  gap: 8px;
  align-items: center;
}

.project-contact-link-row > *,
.project-contact-link-header > * {
  min-width: 0;
}

.project-contact-link-row :deep(.el-select),
.project-contact-link-row :deep(.el-input) {
  width: 100%;
}

.project-contact-link-header {
  padding: 8px 12px 0;
  color: #627089;
  font-size: 12px;
}

.project-contact-link-header.customer-header,
.project-contact-link-row.customer-row {
  grid-template-columns: 48% 20% 20% 56px;
}

.project-contact-link-header.related-row-header,
.project-contact-link-row.related-row {
  grid-template-columns: 16% 20% 24% 14% 16% 56px;
}

.project-contact-empty {
  padding: 12px;
  text-align: center;
  color: #8a97ab;
  border: 1px dashed #d9e2ef;
  border-radius: 8px;
  background: #fff;
  font-size: 12px;
}

.project-contact-link-footnote {
  color: #8a97ab;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .project-contact-columns {
    display: grid;
    grid-template-columns: 1fr;
  }

  .project-contact-card {
    width: 100% !important;
    max-width: 100% !important;
    min-width: 100% !important;
    flex: 1 1 auto !important;
  }
}
</style>
