import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../services/api'

function createDefaultCustomerFilters() {
  return { keyword: '', ownerName: '', status: '', level: '', region: '', decisionLevel: '' }
}

function createDefaultCustomerForm() {
  return {
    id: null,
    name: '',
    shortName: '',
    industry: '',
    customerType: '',
    level: 'A',
    region: '',
    address: '',
    website: '',
    mainPhone: '',
    email: '',
    ownerName: '',
    source: '',
    status: '跟进中',
    cooperationStage: '初步接触',
    tags: '',
    notes: '',
  }
}

function createDefaultContactForm(customerId = null) {
  return {
    id: null,
    customerId,
    name: '',
    position: '',
    department: '',
    gender: '',
    mobile: '',
    officePhone: '',
    email: '',
    wechat: '',
    qq: '',
    decisionLevel: '影响者',
    primaryContact: false,
    notes: '',
  }
}

function createDefaultFollowupForm(customerId = null) {
  return {
    id: null,
    customerId,
    followupType: '电话',
    content: '',
    ownerName: '',
    nextFollowupDate: '',
    resultLevel: '中',
    projectId: null,
    projectName: '',
    followupTime: '',
  }
}

function normalizeText(value) {
  return typeof value === 'string' ? value.trim() : value
}

function sanitizePayload(payload) {
  return Object.fromEntries(
    Object.entries(payload).map(([key, value]) => [key, normalizeText(value)])
  )
}

function isValidPhone(value) {
  if (!value) return true
  return /^\+?[\d\-\s()]{6,20}$/.test(value)
}

function isValidEmail(value) {
  if (!value) return true
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

export function useCustomerManagement({ confirmDelete } = {}) {
  const loading = ref(false)
  const customerSaving = ref(false)
  const contactSaving = ref(false)
  const followupSaving = ref(false)
  const customerDialogOpen = ref(false)
  const contactDialogOpen = ref(false)
  const followupDialogOpen = ref(false)

  const customers = ref([])
  const selectedCustomerId = ref(null)
  const customerPage = ref(1)
  const customerPageSize = ref(20)
  const customerTotal = ref(0)
  const customerPages = ref(0)
  const customerFilters = reactive(createDefaultCustomerFilters())
  const customerForm = reactive(createDefaultCustomerForm())
  const contactForm = reactive(createDefaultContactForm())
  const followupForm = reactive(createDefaultFollowupForm())

  const selectedCustomer = computed(() => {
    if (selectedCustomerDetail.value && selectedCustomerDetail.value.id === selectedCustomerId.value) {
      return selectedCustomerDetail.value
    }
    return customers.value.find((item) => item.id === selectedCustomerId.value) || null
  })
  const selectedCustomerDetail = ref(null)
  const contacts = computed(() => {
    const all = selectedCustomerDetail.value?.contacts || selectedCustomer.value?.contacts || []
    if (!customerFilters.decisionLevel) return all
    return all.filter((item) => item.decisionLevel === customerFilters.decisionLevel)
  })
  const followups = computed(() => {
    const items = selectedCustomerDetail.value?.followups || selectedCustomer.value?.followups || []
    return [...items].sort((a, b) => {
      const timeA = new Date(a?.followupTime || a?.createdAt || 0).getTime()
      const timeB = new Date(b?.followupTime || b?.createdAt || 0).getTime()
      return timeB - timeA
    })
  })

  function showToast(message, type = 'success') {
    ElMessage({ message, type, showClose: true, duration: 2200 })
  }

  async function confirmDeleteAction(message, title = '删除确认') {
    if (confirmDelete) return confirmDelete(message, title)
    return true
  }

  async function loadCustomers(preferredId = null) {
    loading.value = true
    try {
      const params = { page: customerPage.value, size: customerPageSize.value }
      if (customerFilters.keyword) params.keyword = customerFilters.keyword
      if (customerFilters.ownerName) params.ownerName = customerFilters.ownerName
      if (customerFilters.status) params.status = customerFilters.status
      if (customerFilters.level) params.level = customerFilters.level
      if (customerFilters.region) params.region = customerFilters.region
      const { data } = await api.get('/customers/paged', { params })
      customers.value = data.data || []
      customerTotal.value = data.total || 0
      customerPages.value = data.pages || 0
      const targetId = preferredId ?? selectedCustomerId.value ?? customers.value[0]?.id ?? null
      selectedCustomerId.value = customers.value.find((item) => item.id === targetId)?.id || customers.value[0]?.id || null
      if (selectedCustomerId.value) await loadCustomerDetail(selectedCustomerId.value)
    } catch {
      customers.value = []
      selectedCustomerId.value = null
      customerTotal.value = 0
      customerPages.value = 0
    } finally {
      loading.value = false
    }
  }

  function changeCustomerPage(page) {
    customerPage.value = page
    loadCustomers()
  }

  function changeCustomerSize(size) {
    customerPageSize.value = size
    customerPage.value = 1
    loadCustomers()
  }

  function selectCustomer(row) {
    if (row?.id) {
      selectedCustomerId.value = row.id
      loadCustomerDetail(row.id)
    }
  }

  async function loadCustomerDetail(id) {
    try {
      const { data } = await api.get(`/customers/${id}`)
      const detail = data.data
      if (!detail) return
      selectedCustomerDetail.value = detail
    } catch {
      selectedCustomerDetail.value = null
    }
  }

  function resetCustomerForm() { Object.assign(customerForm, createDefaultCustomerForm()) }
  function resetContactForm(customerId = selectedCustomer.value?.id || null) { Object.assign(contactForm, createDefaultContactForm(customerId)) }
  function resetFollowupForm(customerId = selectedCustomer.value?.id || null) { Object.assign(followupForm, createDefaultFollowupForm(customerId)) }

  function openCustomerDialog(row = null) {
    if (row) {
      Object.assign(customerForm, createDefaultCustomerForm(), row)
      selectedCustomerId.value = row.id
    } else resetCustomerForm()
    customerDialogOpen.value = true
  }

  function openContactDialog(contact = null) {
    const customerId = selectedCustomer.value?.id
    if (!customerId) return showToast('请先选择客户公司', 'warning')
    if (contact) Object.assign(contactForm, createDefaultContactForm(customerId), contact, { customerId })
    else resetContactForm(customerId)
    contactDialogOpen.value = true
  }

  function openFollowupDialog(followup = null) {
    const targetCustomerId = followup?.customerId || selectedCustomer.value?.id
    if (!targetCustomerId) return showToast('请先选择客户公司', 'warning')
    selectedCustomerId.value = targetCustomerId
    if (followup?.id) Object.assign(followupForm, createDefaultFollowupForm(targetCustomerId), followup, { customerId: targetCustomerId })
    else resetFollowupForm(targetCustomerId)
    followupDialogOpen.value = true
  }

  function closeCustomerDialog() { customerDialogOpen.value = false; resetCustomerForm() }
  function closeContactDialog() { contactDialogOpen.value = false; resetContactForm() }
  function closeFollowupDialog() { followupDialogOpen.value = false; resetFollowupForm() }

  function validateCustomerForm() {
    if (!String(customerForm.name || '').trim()) return showToast('请填写客户公司名称', 'warning')
    if (!isValidPhone(customerForm.mainPhone)) return showToast('联系电话格式不正确', 'warning')
    if (!isValidEmail(customerForm.email)) return showToast('邮箱格式不正确', 'warning')
    return true
  }
  function validateContactForm() {
    if (!String(contactForm.name || '').trim()) return showToast('请填写联系人姓名', 'warning')
    if (!isValidPhone(contactForm.mobile)) return showToast('手机格式不正确', 'warning')
    if (!isValidPhone(contactForm.officePhone)) return showToast('办公电话格式不正确', 'warning')
    if (!isValidEmail(contactForm.email)) return showToast('邮箱格式不正确', 'warning')
    return true
  }
  function validateFollowupForm() {
    if (!String(followupForm.followupType || '').trim()) return showToast('请选择跟进类型', 'warning')
    if (!String(followupForm.content || '').trim()) return showToast('请填写跟进内容', 'warning')
    return true
  }

  async function saveCustomer() {
    if (!validateCustomerForm()) return
    customerSaving.value = true
    const editing = !!customerForm.id
    try {
      const payload = sanitizePayload({ ...customerForm })
      const request = editing ? api.put(`/customers/${customerForm.id}`, payload) : api.post('/customers', payload)
      const { data } = await request
      closeCustomerDialog()
      await loadCustomers(data.data?.id || customerForm.id)
      showToast(editing ? '客户已更新' : '客户已创建')
    } catch (error) {
      showToast(error?.response?.data?.message || '客户保存失败', 'error')
    } finally { customerSaving.value = false }
  }

  async function deleteCustomer(row) {
    const confirmed = await confirmDeleteAction(`确认删除客户“${row?.name || ''}”吗？联系人和跟进记录都会一起删除。`, '删除客户')
    if (!confirmed) return
    try {
      await api.delete(`/customers/${row.id}`)
      await loadCustomers()
      showToast('客户已删除')
    } catch (error) { showToast(error?.response?.data?.message || '客户删除失败', 'error') }
  }

  async function saveContact() {
    const customerId = contactForm.customerId || selectedCustomer.value?.id
    if (!customerId) return showToast('请先选择客户公司', 'warning')
    if (!validateContactForm()) return
    contactSaving.value = true
    const editing = !!contactForm.id
    try {
      const payload = sanitizePayload({ ...contactForm })
      const request = editing ? api.put(`/customers/contacts/${contactForm.id}`, payload) : api.post(`/customers/${customerId}/contacts`, payload)
      await request
      closeContactDialog(); await loadCustomers(customerId)
      await loadCustomerDetail(customerId)
      showToast(editing ? '联系人已更新' : '联系人已创建')
    } catch (error) { showToast(error?.response?.data?.message || '联系人保存失败', 'error') }
    finally { contactSaving.value = false }
  }

  async function deleteContact(contact) {
    const confirmed = await confirmDeleteAction(`确认删除联系人“${contact?.name || ''}”吗？`, '删除联系人')
    if (!confirmed) return
    try {
      await api.delete(`/customers/contacts/${contact.id}`)
      await loadCustomers(selectedCustomer.value?.id)
      await loadCustomerDetail(selectedCustomer.value?.id)
      showToast('联系人已删除')
    } catch (error) { showToast(error?.response?.data?.message || '联系人删除失败', 'error') }
  }

  async function saveFollowup() {
    const customerId = followupForm.customerId || selectedCustomer.value?.id
    if (!customerId) return showToast('请先选择客户公司', 'warning')
    if (!validateFollowupForm()) return
    followupSaving.value = true
    const editing = !!followupForm.id
    try {
      const payload = sanitizePayload({ ...followupForm })
      const request = editing ? api.put(`/customers/followups/${followupForm.id}`, payload) : api.post(`/customers/${customerId}/followups`, payload)
      await request
      closeFollowupDialog(); await loadCustomers(customerId)
      await loadCustomerDetail(customerId)
      showToast(editing ? '跟进记录已更新' : '跟进记录已创建')
    } catch (error) { showToast(error?.response?.data?.message || '跟进记录保存失败', 'error') }
    finally { followupSaving.value = false }
  }

  async function deleteFollowup(followup) {
    const confirmed = await confirmDeleteAction('确认删除这条跟进记录吗？', '删除跟进')
    if (!confirmed) return
    try {
      await api.delete(`/customers/followups/${followup.id}`)
      await loadCustomers(selectedCustomer.value?.id)
      await loadCustomerDetail(selectedCustomer.value?.id)
      showToast('跟进记录已删除')
    } catch (error) { showToast(error?.response?.data?.message || '跟进记录删除失败', 'error') }
  }

  function resetCustomerFilters() {
    Object.assign(customerFilters, createDefaultCustomerFilters())
    customerPage.value = 1
    loadCustomers(selectedCustomerId.value)
  }

  return {
    loading, customerSaving, contactSaving, followupSaving,
    customerDialogOpen, contactDialogOpen, followupDialogOpen,
    customers, selectedCustomer, contacts, followups,
    customerFilters, customerForm, contactForm, followupForm,
    customerPage, customerPageSize, customerTotal, customerPages,
    loadCustomers, selectCustomer, changeCustomerPage, changeCustomerSize,
    openCustomerDialog, openContactDialog, openFollowupDialog,
    closeCustomerDialog, closeContactDialog, closeFollowupDialog,
    saveCustomer, deleteCustomer, saveContact, deleteContact, saveFollowup, deleteFollowup,
    resetCustomerFilters,
  }
}
