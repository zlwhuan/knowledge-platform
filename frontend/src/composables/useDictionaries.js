import { computed, reactive, ref } from 'vue'
import { api } from '../services/api'
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
import {
  projectAcceptanceStatusOptions,
  projectActivityTypeOptions,
  projectContractStatusOptions,
  projectPaymentStatusOptions,
  projectRiskOptions,
  projectServiceStatusOptions,
  projectStageOptions,
  projectStatusOptions,
} from './projectDialogOptions'

export const dictionaryTypeMeta = [
  { type: 'customer_status', label: '客户状态', module: '客户管理' },
  { type: 'customer_level', label: '客户等级', module: '客户管理' },
  { type: 'customer_region', label: '客户区域', module: '客户管理' },
  { type: 'customer_industry', label: '客户行业', module: '客户管理' },
  { type: 'customer_type', label: '客户类型', module: '客户管理' },
  { type: 'customer_source', label: '客户来源', module: '客户管理' },
  { type: 'customer_stage', label: '合作阶段', module: '客户管理' },
  { type: 'contact_decision_level', label: '联系人决策层级', module: '客户管理' },
  { type: 'contact_gender', label: '联系人性别', module: '客户管理' },
  { type: 'followup_type', label: '跟进方式', module: '客户管理' },
  { type: 'followup_result', label: '跟进结果', module: '客户管理' },
  { type: 'project_stage', label: '项目阶段', module: '项目追踪' },
  { type: 'project_status', label: '项目状态', module: '项目追踪' },
  { type: 'project_risk', label: '项目风险', module: '项目追踪' },
  { type: 'project_activity_type', label: '项目动态类型', module: '项目追踪' },
  { type: 'project_contract_status', label: '合同状态', module: '项目追踪' },
  { type: 'project_payment_status', label: '回款状态', module: '项目追踪' },
  { type: 'project_acceptance_status', label: '验收状态', module: '项目追踪' },
  { type: 'project_service_status', label: '服务状态', module: '项目追踪' },
  { type: 'knowledge_type', label: '知识库类型', module: '知识库' },
]

const fallbackValues = {
  customer_status: CUSTOMER_STATUS_OPTIONS,
  customer_level: CUSTOMER_LEVEL_OPTIONS,
  customer_region: CUSTOMER_REGION_OPTIONS,
  customer_industry: CUSTOMER_INDUSTRY_OPTIONS,
  customer_type: CUSTOMER_TYPE_OPTIONS,
  customer_source: CUSTOMER_SOURCE_OPTIONS,
  customer_stage: CUSTOMER_STAGE_OPTIONS,
  contact_decision_level: CONTACT_DECISION_OPTIONS,
  contact_gender: CONTACT_GENDER_OPTIONS,
  followup_type: FOLLOWUP_TYPE_OPTIONS,
  followup_result: FOLLOWUP_RESULT_OPTIONS,
  project_stage: projectStageOptions.map((item) => item.value),
  project_status: projectStatusOptions.map((item) => item.value),
  project_risk: projectRiskOptions.map((item) => item.value),
  project_activity_type: projectActivityTypeOptions.map((item) => item.value),
  project_contract_status: projectContractStatusOptions.map((item) => item.value),
  project_payment_status: projectPaymentStatusOptions.map((item) => item.value),
  project_acceptance_status: projectAcceptanceStatusOptions.map((item) => item.value),
  project_service_status: projectServiceStatusOptions.map((item) => item.value),
  knowledge_type: ['文档', '制度', '方案', '培训'],
}

function createFallbackMap() {
  return Object.fromEntries(
    Object.entries(fallbackValues).map(([key, values]) => [
      key,
      values.map((value, index) => ({
        id: `fallback-${key}-${index}`,
        dictType: key,
        itemValue: value,
        itemLabel: value,
        sortOrder: (index + 1) * 10,
        enabled: true,
      })),
    ]),
  )
}

export function useDictionaries() {
  const loading = ref(false)
  const saving = ref(false)
  const grouped = reactive(createFallbackMap())
  const draft = reactive({ id: null, dictType: 'customer_status', itemValue: '', itemLabel: '', sortOrder: 10, enabled: true, remark: '' })

  const dictionaryGroups = computed(() => dictionaryTypeMeta.map((meta) => ({
    ...meta,
    items: grouped[meta.type] || [],
  })))

  function getOptions(dictType) {
    return (grouped[dictType] || []).filter((item) => item.enabled !== false)
  }

  async function loadDictionaries() {
    loading.value = true
    try {
      const { data } = await api.get('/dictionaries')
      const incoming = data?.data || []
      const next = createFallbackMap()
      incoming.forEach((group) => {
        next[group.dictType] = group.items || []
      })
      Object.keys(grouped).forEach((key) => delete grouped[key])
      Object.entries(next).forEach(([key, value]) => { grouped[key] = value })
    } finally {
      loading.value = false
    }
  }

  function resetDraft(dictType = draft.dictType) {
    draft.id = null
    draft.dictType = dictType
    draft.itemValue = ''
    draft.itemLabel = ''
    draft.remark = ''
    draft.enabled = true
    draft.sortOrder = ((grouped[dictType]?.length || 0) + 1) * 10
  }

  function editItem(item) {
    draft.id = item.id
    draft.dictType = item.dictType
    draft.itemValue = item.itemValue
    draft.itemLabel = item.itemLabel
    draft.remark = item.remark || ''
    draft.enabled = item.enabled !== false
    draft.sortOrder = item.sortOrder ?? 0
  }

  async function saveDictionaryItem() {
    saving.value = true
    try {
      const payload = {
        dictType: String(draft.dictType || '').trim(),
        itemValue: String(draft.itemValue || '').trim(),
        itemLabel: String(draft.itemLabel || '').trim(),
        sortOrder: Number(draft.sortOrder || 0),
        enabled: !!draft.enabled,
        remark: String(draft.remark || '').trim(),
      }
      if (draft.id) await api.put(`/dictionaries/${draft.id}`, payload)
      else await api.post('/dictionaries', payload)
      await loadDictionaries()
      resetDraft(payload.dictType)
    } finally {
      saving.value = false
    }
  }

  async function removeDictionaryItem(id) {
    await api.delete(`/dictionaries/${id}`)
    await loadDictionaries()
  }

  return {
    dictionariesLoading: loading,
    dictionariesSaving: saving,
    dictionaryGroups,
    dictionaryDraft: draft,
    getOptions,
    loadDictionaries,
    resetDictionaryDraft: resetDraft,
    editDictionaryItem: editItem,
    saveDictionaryItem,
    removeDictionaryItem,
  }
}
