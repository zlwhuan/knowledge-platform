export function createDefaultProjectFilters() {
  return { keyword: '', customerName: '', stage: '', status: '', owner: '', riskLevel: '', dueState: '' }
}

export function createDefaultProjectForm() {
  return {
    id: null,
    name: '',
    customerName: '',
    customerType: '',
    stage: '商机立项',
    status: '进行中',
    progress: 0,
    riskLevel: '低',
    salesOwner: '',
    projectManager: '',
    implementationOwner: '',
    documentOwner: '',
    serviceOwner: '',
    contractAmount: 0,
    receivedAmount: 0,
    contractStatus: '待签约',
    paymentStatus: '待回款',
    acceptanceStatus: '待验收',
    serviceStatus: '跟进中',
    startDate: '',
    plannedEndDate: '',
    acceptanceDate: '',
    warrantyUntil: '',
    projectContactIds: [],
    customerContactLinks: [],
    relatedContactLinks: [],
    projectContactLinks: [],
    description: '',
  }
}

export function createDefaultProjectActivityForm(row = null) {
  return { id: null, projectId: row?.id || null, recordType: '普通记录', ownerName: '', content: '', recordTime: '' }
}

function getCurrentDateTimeValue() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d}T${hh}:${mm}:${ss}`
}

export function createDefaultProjectProgressForm(row = null) {
  return {
    id: null,
    projectId: row?.id || null,
    stage: row?.stage || '实施交付',
    status: row?.status || '进行中',
    progress: row?.progress || 0,
    riskLevel: row?.riskLevel || '低',
    summary: '',
    nextActionDueDate: '',
    ownerName: '',
    recordTime: getCurrentDateTimeValue(),
  }
}

export function mapProjectToForm(row) {
  return {
    ...createDefaultProjectForm(),
    id: row.id,
    name: row.name || '',
    customerName: row.customerName || '',
    customerType: row.customerType || '',
    stage: row.stage || '商机立项',
    status: row.status || '进行中',
    progress: row.progress || 0,
    riskLevel: row.riskLevel || '低',
    salesOwner: row.salesOwner || '',
    projectManager: row.projectManager || '',
    implementationOwner: row.implementationOwner || '',
    documentOwner: row.documentOwner || '',
    serviceOwner: row.serviceOwner || '',
    contractAmount: row.contractAmount || 0,
    receivedAmount: row.receivedAmount || 0,
    contractStatus: row.contractStatus || '待签约',
    paymentStatus: row.paymentStatus || '待回款',
    acceptanceStatus: row.acceptanceStatus || '待验收',
    serviceStatus: row.serviceStatus || '跟进中',
    startDate: row.startDate || '',
    plannedEndDate: row.plannedEndDate || '',
    acceptanceDate: row.acceptanceDate || '',
    warrantyUntil: row.warrantyUntil || '',
    projectContactIds: String(row.projectContactIds || '').trim() ? String(row.projectContactIds).split(',').map((item) => item.trim()).filter(Boolean) : [],
    customerContactLinks: (() => {
      try {
        const parsed = JSON.parse(row.projectContactLinks || '[]')
        if (!Array.isArray(parsed)) return []
        return parsed.filter((item) => String(item?.sourceType || 'customer').trim() === 'customer')
      } catch {
        return []
      }
    })(),
    relatedContactLinks: (() => {
      try {
        const parsed = JSON.parse(row.projectContactLinks || '[]')
        if (!Array.isArray(parsed)) return []
        return parsed
          .filter((item) => String(item?.sourceType || '').trim() === 'related')
          .map((item) => ({
            ...item,
            relatedCustomerType: item.relatedCustomerType || '',
            relatedCustomerName: item.relatedCustomerName || item.customerName || '',
          }))
      } catch {
        return []
      }
    })(),
    projectContactLinks: (() => {
      try {
        const parsed = JSON.parse(row.projectContactLinks || '[]')
        return Array.isArray(parsed) ? parsed : []
      } catch {
        return []
      }
    })(),
    description: row.description || '',
  }
}

export function mapActivityToForm(activity) {
  return {
    ...createDefaultProjectActivityForm(),
    id: activity.id,
    projectId: activity.projectId || null,
    recordType: activity.recordType || '普通记录',
    ownerName: activity.ownerName || '',
    content: activity.content || '',
    recordTime: activity.recordTime || '',
  }
}

export function mapProgressToForm(record, row = null) {
  return {
    ...createDefaultProjectProgressForm(row),
    id: record.id,
    projectId: record.projectId || row?.id || null,
    stage: record.stage || row?.stage || '实施交付',
    status: record.status || row?.status || '进行中',
    progress: record.progress ?? row?.progress ?? 0,
    riskLevel: record.riskLevel || row?.riskLevel || '低',
    summary: record.summary || '',
    nextAction: record.nextAction ?? '',
    nextActionDueDate: record.nextActionDueDate ?? '',
    ownerName: record.ownerName || '',
    recordTime: record.recordTime || '',
  }
}
