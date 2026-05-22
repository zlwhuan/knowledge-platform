export const projectPhaseOptions = [
  { label: '销售', value: '销售' },
  { label: '合同', value: '合同' },
  { label: '实施', value: '实施' },
  { label: '验收', value: '验收' },
  { label: '售后', value: '售后' },
]

export const projectStageOptions = [
  { label: '商机立项', value: '商机立项' },
  { label: '合同执行', value: '合同执行' },
  { label: '实施交付', value: '实施交付' },
  { label: '验收结算', value: '验收结算' },
  { label: '售后维保', value: '售后维保' },
]

export const projectStatusOptions = [
  { label: '进行中', value: '进行中' },
  { label: '已完成', value: '已完成' },
  { label: '暂停', value: '暂停' },
]

export const projectRiskOptions = [
  { label: '低', value: '低' },
  { label: '中', value: '中' },
  { label: '高', value: '高' },
]

export const projectDueStateOptions = [
  { label: '逾期', value: 'overdue' },
  { label: '今日到期', value: 'today' },
  { label: '7天内临期', value: 'dueSoon' },
  { label: '正常', value: 'normal' },
  { label: '未设置', value: 'unset' },
]

export const projectActivityTypeOptions = [
  { label: '普通记录', value: '普通记录' },
  { label: '合同', value: '合同' },
  { label: '资料', value: '资料' },
  { label: '沟通', value: '沟通' },
  { label: '售后', value: '售后' },
]

export const projectContractStatusOptions = [
  { label: '未签约', value: '未签约' },
  { label: '签约中', value: '签约中' },
  { label: '已签约', value: '已签约' },
  { label: '合同变更中', value: '合同变更中' },
  { label: '合同终止', value: '合同终止' },
]

export const projectPaymentStatusOptions = [
  { label: '未回款', value: '未回款' },
  { label: '部分回款', value: '部分回款' },
  { label: '按计划回款', value: '按计划回款' },
  { label: '回款逾期', value: '回款逾期' },
  { label: '回款完成', value: '回款完成' },
]

export const projectAcceptanceStatusOptions = [
  { label: '未开始', value: '未开始' },
  { label: '验收准备中', value: '验收准备中' },
  { label: '验收中', value: '验收中' },
  { label: '验收通过', value: '验收通过' },
  { label: '验收未通过', value: '验收未通过' },
]

export const projectServiceStatusOptions = [
  { label: '未启动', value: '未启动' },
  { label: '服务中', value: '服务中' },
  { label: '稳定运行', value: '稳定运行' },
  { label: '服务预警', value: '服务预警' },
  { label: '服务结束', value: '服务结束' },
]

export const projectFormFields = [
  { label: '项目名称', model: 'name', component: 'input', required: true, maxlength: 100, wordLimit: true },
  { label: '客户名称', model: 'customerName', component: 'customer-select', required: true },
  { label: '客户类型', model: 'customerType', component: 'readonly-text', fallback: '--' },
  { label: '项目当前阶段', model: 'stage', component: 'input', disabled: true },
  { label: '项目当前状态', model: 'status', component: 'input', disabled: true },
  { label: '当前进度', model: 'progress', component: 'progress-text', suffix: '%' },
  { label: '当前风险', model: 'riskLevel', component: 'risk-text', fallback: '--' },
  { label: '销售负责人', model: 'salesOwner', component: 'user-select' },
  { label: '项目经理', model: 'projectManager', component: 'user-select' },
  { label: '实施负责人', model: 'implementationOwner', component: 'user-select' },
  { label: '文档负责人', model: 'documentOwner', component: 'user-select' },
  { label: '服务负责人', model: 'serviceOwner', component: 'user-select' },
  { label: '项目关联联系人', model: 'projectContactLinks', component: 'project-contact-link-editor', span: 2 },
  { label: '合同额', model: 'contractAmount', component: 'number', min: 0, precision: 2, step: 10000, isMoney: true },
  { label: '已回款', model: 'receivedAmount', component: 'number', min: 0, precision: 2, step: 10000, isMoney: true },
  { label: '合同状态', model: 'contractStatus', component: 'select', optionsKey: 'contractStatus' },
  { label: '回款状态', model: 'paymentStatus', component: 'select', optionsKey: 'paymentStatus' },
  { label: '验收状态', model: 'acceptanceStatus', component: 'select', optionsKey: 'acceptanceStatus' },
  { label: '服务状态', model: 'serviceStatus', component: 'select', optionsKey: 'serviceStatus' },
  { label: '项目启动日期', model: 'startDate', component: 'date' },
  { label: '计划完成日期', model: 'plannedEndDate', component: 'date' },
  { label: '验收日期', model: 'acceptanceDate', component: 'date' },
  { label: '维保到期', model: 'warrantyUntil', component: 'date' },
  { label: '项目说明', model: 'description', component: 'textarea', span: 2, rows: 4 },
]
