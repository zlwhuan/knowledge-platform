function parseDate(value) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function getOpenProgressTodos(row) {
  const records = [...(row.progressRecords || [])]
    .sort((a, b) => new Date(b.recordTime || b.createdAt || 0) - new Date(a.recordTime || a.createdAt || 0))

  const latestByStage = new Map()
  records.forEach((item) => {
    const stageKey = String(item.stage || row.stage || '--').trim() || '--'
    if (!latestByStage.has(stageKey)) latestByStage.set(stageKey, item)
  })

  return Array.from(latestByStage.values()).filter((item) => {
    const status = String(item.status || '').trim()
    const progress = Number(item.progress || 0)
    const nextAction = String(item.nextAction || '').trim()
    const dueDate = parseDate(item.nextActionDueDate)
    if (status === '已完成' || progress >= 100) return false
    return !!nextAction && !!dueDate
  })
}

function getNearestFollowUp(row) {
  const candidates = getOpenProgressTodos(row)
    .map((item) => ({ raw: item.nextActionDueDate, date: parseDate(item.nextActionDueDate) }))
    .filter((item) => item.date)
    .sort((left, right) => left.date - right.date)
  return candidates[0] || null
}

function getDueStatus(row) {
  const nearest = getNearestFollowUp(row)
  if (!nearest) return { label: '未设置', type: 'info', diffDays: null }
  const today = new Date()
  const base = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const target = new Date(nearest.date.getFullYear(), nearest.date.getMonth(), nearest.date.getDate())
  const diff = Math.round((target - base) / 86400000)
  if (diff < 0) return { label: `逾期 ${Math.abs(diff)} 天`, type: 'danger', diffDays: diff }
  if (diff <= 7) return { label: `${diff} 天内`, type: 'warning', diffDays: diff }
  return { label: '正常', type: 'success', diffDays: diff }
}

function matchDueState(row, dueState) {
  if (!dueState) return true
  const dueStatus = getDueStatus(row)
  const diff = dueStatus.diffDays

  if (diff === null) return dueState === 'unset'

  if (dueState === 'overdue') return diff < 0
  if (dueState === 'today') return diff === 0
  if (dueState === 'dueSoon') return diff > 0 && diff <= 7
  if (dueState === 'normal') return diff > 7
  return true
}

export function useProjectTrackerTable() {
  return {
    getNearestFollowUp,
    getDueStatus,
    matchDueState,
  }
}
