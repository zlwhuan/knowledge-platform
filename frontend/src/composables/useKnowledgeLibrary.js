import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../services/api'

const knowledgeFieldLabels = {
  title: '标题',
  type: '类型',
  categoryId: '分类',
  summary: '摘要',
  tags: '标签',
  source: '来源',
  projectId: '关联项目',
}

export function useKnowledgeLibrary(options = {}) {
  const { confirmDelete } = options
  const libraryLoading = ref(false)
  const categoriesLoading = ref(false)
  const saving = ref(false)
  const detailDialogOpen = ref(false)
  const selectedItemId = ref(null)
  const selectedItemDetail = ref(null)
  const selectedCategoryId = ref(null)
  const uploadQueue = ref([])
  const selectedItems = ref([])
  const pendingDeleteAttachmentIds = ref([])

  const libraryPage = ref(1)
  const libraryPageSize = ref(20)
  const libraryTotal = ref(0)
  const libraryPages = ref(0)

  const libraryFilters = reactive({ keyword: '', type: '', categoryId: '', projectId: '' })
  const categories = ref([])
  const items = ref([])
  const form = reactive({
    id: null,
    title: '',
    type: '文档',
    categoryId: '',
    projectId: '',
    summary: '',
    contentMarkdown: '',
    tags: '',
    source: '',
    operationLog: '',
    attachments: [],
  })
  const batchEditDialog = reactive({
    open: false,
    categoryId: '',
    source: '',
    tags: '',
    saving: false,
  })

  const flatCategories = computed(() => {
    const result = []
    const walk = (nodes = [], depth = 0) => {
      nodes.forEach((node) => {
        result.push({ ...node, depth })
        if (node.children?.length) walk(node.children, depth + 1)
      })
    }
    walk(categories.value)
    return result
  })

  const selectedItem = computed(() => selectedItemDetail.value || items.value.find((item) => item.id === selectedItemId.value) || null)

  function toast(message, type = 'success') {
    ElMessage({ message, type, showClose: true, duration: 2200 })
  }

  function formatErrorMessage(error, fallback) {
    const message = error?.response?.data?.message || fallback
    const [field, detail] = String(message).split(':')
    if (!detail) return message
    return `${knowledgeFieldLabels[field.trim()] || field.trim()}：${detail.trim()}`
  }

  async function loadCategories() {
    categoriesLoading.value = true
    try {
      const { data } = await api.get('/categories')
      categories.value = data.data || []
    } finally {
      categoriesLoading.value = false
    }
  }

  async function loadItems() {
    libraryLoading.value = true
    try {
      const params = {}
      if (libraryFilters.keyword) params.keyword = libraryFilters.keyword
      if (libraryFilters.type) params.type = libraryFilters.type
      if (libraryFilters.categoryId) params.categoryId = libraryFilters.categoryId
      if (libraryFilters.projectId) params.projectId = libraryFilters.projectId
      if (selectedCategoryId.value && !libraryFilters.categoryId) params.categoryId = selectedCategoryId.value
      // Use paged endpoint
      params.page = libraryPage.value
      params.size = libraryPageSize.value
      const { data } = await api.get('/items/paged', { params })
      const payload = data?.data || {}
      items.value = payload.items || []
      libraryTotal.value = payload.total || 0
      libraryPages.value = payload.pages || 0
      selectedItems.value = []
    } finally {
      libraryLoading.value = false
    }
  }

  function changePage(page) {
    libraryPage.value = page
    loadItems()
  }

  async function loadAttachments(itemId) {
    try {
      const { data } = await api.get(`/items/${itemId}`)
      return data.data?.attachments || data.data?.item?.attachments || []
    } catch {
      return []
    }
  }

  async function loadItemDetail(itemId) {
    const { data } = await api.get(`/items/${itemId}`)
    return data.data?.item || data.data || null
  }

  function openAllLibrary() {
    selectedCategoryId.value = null
    libraryFilters.categoryId = ''
    loadItems()
  }

  function selectCategory(categoryId) {
    selectedCategoryId.value = categoryId
    libraryFilters.categoryId = categoryId ? String(categoryId) : ''
    loadItems()
  }

  function resetFilters() {
    Object.assign(libraryFilters, { keyword: '', type: '', categoryId: '', projectId: '' })
    selectedCategoryId.value = null
    loadItems()
  }

  function resetForm(defaultCategoryId = '') {
    Object.assign(form, {
      id: null,
      title: '',
      type: '文档',
      categoryId: defaultCategoryId ? String(defaultCategoryId) : '',
      projectId: '',
      summary: '',
      contentMarkdown: '',
      tags: '',
      source: '',
      operationLog: '',
      attachments: [],
    })
    uploadQueue.value = []
    pendingDeleteAttachmentIds.value = []
  }

  function startCreateContent(defaultCategoryId = selectedCategoryId.value) {
    resetForm(defaultCategoryId)
  }

  async function fillForm(item) {
    Object.assign(form, {
      id: item.id,
      title: item.title || '',
      type: item.type || '文档',
      categoryId: item.categoryId ? String(item.categoryId) : '',
      projectId: item.projectId ? String(item.projectId) : '',
      summary: item.summary || '',
      contentMarkdown: item.contentMarkdown || '',
      tags: item.tags || '',
      source: item.source || '',
      operationLog: item.operationLog || '',
      attachments: await loadAttachments(item.id),
    })
    uploadQueue.value = []
    pendingDeleteAttachmentIds.value = []
  }

  function markAttachmentForDeletion(attachment) {
    if (!attachment?.id) return toast('未识别到可删除的附件', 'warning')
    if (!pendingDeleteAttachmentIds.value.includes(attachment.id)) {
      pendingDeleteAttachmentIds.value.push(attachment.id)
    }
    form.attachments = (form.attachments || []).filter((item) => item.id !== attachment.id)
  }

  async function openDetail(item) {
    const itemId = item?.id || item
    if (!itemId) {
      toast('Cannot get item ID', 'warning')
      return
    }
    selectedItemId.value = itemId
    detailDialogOpen.value = true
    selectedItemDetail.value = null
    try {
      selectedItemDetail.value = await loadItemDetail(itemId)
    } catch {
      selectedItemDetail.value = null
    }
  }

  async function saveItem() {
    if (!String(form.title || '').trim()) return toast('请先填写标题', 'warning')
    if (!String(form.type || '').trim()) return toast('请先填写资料类型', 'warning')
    if (!form.categoryId) return toast('请选择资料分类', 'warning')
    if (String(form.title || '').trim().length > 200) return toast('标题不能超过 200 个字符', 'warning')
    if (String(form.type || '').trim().length > 50) return toast('资料类型不能超过 50 个字符', 'warning')
    if (String(form.summary || '').length > 1000) return toast('摘要不能超过 1000 个字符', 'warning')
    if (String(form.tags || '').length > 500) return toast('标签不能超过 500 个字符', 'warning')
    if (String(form.source || '').length > 200) return toast('来源不能超过 200 个字符', 'warning')
    saving.value = true
    try {
      const payload = {
        title: String(form.title || '').trim(),
        type: String(form.type || '').trim(),
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        projectId: form.projectId ? Number(form.projectId) : null,
        summary: form.summary,
        contentMarkdown: form.contentMarkdown,
        tags: form.tags,
        source: form.source,
        operationLog: form.operationLog,
      }
      const request = form.id ? api.put(`/items/${form.id}`, payload) : api.post('/items', payload)
      const { data } = await request
      const savedId = data.data?.id || form.id
      form.id = savedId

      if (pendingDeleteAttachmentIds.value.length) {
        const results = await Promise.allSettled(
          pendingDeleteAttachmentIds.value.map((attachmentId) => api.delete(`/attachments/${attachmentId}`)),
        )
        const failedCount = results.filter((item) => item.status === 'rejected').length
        if (failedCount) toast(`有 ${failedCount} 个附件删除失败，请重试`, 'warning')
      }

      if (savedId && uploadQueue.value.length) {
        for (const file of uploadQueue.value) {
          const formData = new FormData()
          formData.append('itemId', String(savedId))
          formData.append('file', file)
          const uploadRes = await api.post('/attachments/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
          // 上传完后后台预热预览（触发 LibreOffice 转换），不阻塞不弹窗
          const attachmentId = uploadRes?.data?.data?.attachment?.id || uploadRes?.data?.data?.id
          if (attachmentId) {
            api.get(`/attachments/${attachmentId}/preview`).catch(() => {})
          }
        }
      }

      await loadItems()
      if (savedId) {
        selectedItemId.value = savedId
        try {
          selectedItemDetail.value = await loadItemDetail(savedId)
        } catch {
          selectedItemDetail.value = null
        }
      }
      uploadQueue.value = []
      pendingDeleteAttachmentIds.value = []
      toast(form.id ? '资料已更新' : '资料已创建')
      return savedId
    } catch (error) {
      toast(formatErrorMessage(error, '资料保存失败'), 'error')
      return null
    } finally {
      saving.value = false
    }
  }

  async function deleteItem(id) {
    const ok = confirmDelete
      ? await confirmDelete('确认删除这条资料吗？', '删除资料')
      : true
    if (!ok) return
    try {
      await api.delete(`/items/${id}`)
      if (selectedItemId.value === id) {
        selectedItemId.value = null
        selectedItemDetail.value = null
        detailDialogOpen.value = false
      }
      await loadItems()
      toast('资料已删除')
    } catch (error) {
      toast(error?.response?.data?.message || '删除失败', 'error')
    }
  }

  function resetBatchEditDialog() {
    batchEditDialog.open = false
    batchEditDialog.categoryId = ''
    batchEditDialog.source = ''
    batchEditDialog.tags = ''
    batchEditDialog.saving = false
  }

  function openBatchEditDialog() {
    batchEditDialog.categoryId = ''
    batchEditDialog.source = ''
    batchEditDialog.tags = ''
    batchEditDialog.open = true
  }

  async function applyBatchEdit(ids = []) {
    if (!ids.length) return toast('请先选择资料', 'warning')
    if (!batchEditDialog.categoryId && !String(batchEditDialog.source || '').trim() && !String(batchEditDialog.tags || '').trim()) {
      return toast('请至少填写一项批量修改内容', 'warning')
    }
    batchEditDialog.saving = true
    try {
      await Promise.all(ids.map((id) => {
        const current = items.value.find((item) => item.id === id)
        if (!current) return Promise.resolve()
        return api.put(`/items/${id}`, {
          title: current.title,
          type: current.type,
          categoryId: Number(batchEditDialog.categoryId || current.categoryId || 0) || null,
          projectId: current.projectId || null,
          summary: current.summary || '',
          contentMarkdown: current.contentMarkdown || '',
          tags: String(batchEditDialog.tags || '').trim() || current.tags || '',
          source: String(batchEditDialog.source || '').trim() || current.source || '',
          operationLog: current.operationLog || '',
        })
      }))
      resetBatchEditDialog()
      await loadItems()
      toast('批量编辑完成')
    } catch (error) {
      toast(error?.response?.data?.message || '批量编辑失败', 'error')
    } finally {
      batchEditDialog.saving = false
    }
  }

  function handleFileChange(event) {
    const files = Array.from(event?.target?.files || [])
    if (!files.length) return
    const merged = [...uploadQueue.value]
    files.forEach((file) => {
      if (!merged.some((item) => item.name === file.name && item.size === file.size)) merged.push(file)
    })
    uploadQueue.value = merged
    event.target.value = ''
  }

  function removeQueuedFile(name) {
    uploadQueue.value = uploadQueue.value.filter((file) => file.name !== name)
  }

  async function bulkDelete(ids = []) {
    if (!ids.length) return toast('请先选择资料', 'warning')
    const ok = confirmDelete
      ? await confirmDelete(`确认批量删除选中的 ${ids.length} 条资料吗？`, '批量删除资料')
      : true
    if (!ok) return
    try {
      await api.post('/items/bulk/delete', { ids })
      await loadItems()
      toast('批量删除成功')
    } catch (error) {
      toast(error?.response?.data?.message || '批量删除失败', 'error')
    }
  }

  return {
    libraryLoading,
    categoriesLoading,
    saving,
    detailDialogOpen,
    selectedItemId,
    selectedItem,
    selectedItemDetail,
    selectedCategoryId,
    selectedItems,
    libraryFilters,
    categories,
    items,
    flatCategories,
    form,
    uploadQueue,
    batchEditDialog,
    loadCategories,
    loadItems,
    openAllLibrary,
    selectCategory,
    resetFilters,
    startCreateContent,
    fillForm,
    markAttachmentForDeletion,
    openDetail,
    saveItem,
    deleteItem,
    resetBatchEditDialog,
    openBatchEditDialog,
    applyBatchEdit,
    handleFileChange,
    removeQueuedFile,
    bulkDelete,
    libraryPage,
    libraryPageSize,
    libraryTotal,
    libraryPages,
    changePage,
  }
}
