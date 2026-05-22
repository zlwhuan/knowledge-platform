export function useProjectTrackerView(emit) {
  function forwardWithPayload(eventName) {
    return (payload) => emit(eventName, payload)
  }

  function forwardVariadic(eventName) {
    return (...payload) => emit(eventName, ...payload)
  }

  return {
    onSwitchSubview: forwardWithPayload('switch-subview'),
    onRefresh: () => emit('refresh'),
    onOpenProgress: forwardVariadic('open-progress'),
    onOpenEdit: forwardWithPayload('open-edit'),
    onSelectProject: forwardWithPayload('select-project'),
    onResetFilters: () => emit('reset-filters'),
    onFilter: () => emit('filter'),
    onDeleteProject: forwardWithPayload('delete-project'),
    onDeleteProgress: forwardWithPayload('delete-progress'),
    onOpenRelatedItem: forwardWithPayload('open-related-item'),
    onCreateRelatedItem: forwardWithPayload('create-related-item'),
    onCloseProjectForm: () => emit('close-project-form'),
    onSaveProject: () => emit('save-project'),
    onCloseProjectProgress: () => emit('close-project-progress'),
    onSaveProjectProgress: () => emit('save-project-progress'),
  }
}
