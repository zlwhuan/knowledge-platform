export function useLibraryActions({
  api,
  items,
  selectedItem,
  form,
  detailDialogOpen,
  loadItems,
  openDetail,
  fillForm,
  startLibraryCreateContent,
  showToast,
  currentView,
  confirmDelete,
}) {
  function openLibraryDetail(item) {
    openDetail(item)
  }

  async function removeAttachment(attachment) {
    if (!attachment?.id) {
      showToast('未识别到可删除的附件', 'warning')
      return
    }
    const ok = confirmDelete
      ? await confirmDelete(`确认删除附件“${attachment?.originalFileName || '未命名附件'}”吗？`, '删除附件')
      : true
    if (!ok) return
    try {
      await api.delete(`/attachments/${attachment.id}`)
      showToast('附件已删除')
      if (selectedItem.value?.id) {
        await loadItems()
        const latest = items.value.find((item) => item.id === selectedItem.value.id)
        if (latest) openDetail(latest)
      }
    } catch (error) {
      showToast(error?.response?.data?.message || '附件删除失败', 'error')
    }
  }

  function openDetailByLibrary(item) {
    openLibraryDetail(item)
  }

  function editLibraryItem(item) {
    fillForm(item)
  }

  function createProjectRelatedItem(project) {
    startLibraryCreateContent()
    form.projectId = project?.id ? String(project.id) : ''
    if (project?.name && !String(form.title || '').trim()) form.title = `${project.name} - `
    currentView.value = 'compose'
  }

  return {
    openLibraryDetail,
    openDetailByLibrary,
    editLibraryItem,
    removeAttachment,
    createProjectRelatedItem,
  }
}
