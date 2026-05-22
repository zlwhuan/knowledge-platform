import { reactive } from 'vue'

// Simple reactive store for auth state
// (Using reactive object instead of Pinia since we can't guarantee pinia is installed)
const state = reactive({
  token: localStorage.getItem('auth_token') || null,
  user: JSON.parse(localStorage.getItem('auth_user') || 'null'),
})

export function useAuthStore() {
  function setAuth(token, user) {
    state.token = token
    state.user = user
    localStorage.setItem('auth_token', token)
    localStorage.setItem('auth_user', JSON.stringify(user))
  }

  function clearAuth() {
    state.token = null
    state.user = null
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
  }

  function isLoggedIn() {
    return !!state.token
  }

  return {
    token: state.token,
    user: state.user,
    setAuth,
    clearAuth,
    isLoggedIn,
  }
}
