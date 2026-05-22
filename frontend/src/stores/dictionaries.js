import { reactive } from 'vue'

const state = reactive({
  cache: {},
  loading: {},
})

export function useDictionaryStore() {
  function setDict(dictType, items) {
    state.cache[dictType] = items
  }

  function getDict(dictType) {
    return state.cache[dictType] || []
  }

  function setLoading(dictType, loading) {
    state.loading[dictType] = loading
  }

  function isLoading(dictType) {
    return !!state.loading[dictType]
  }

  return {
    setDict,
    getDict,
    setLoading,
    isLoading,
  }
}
