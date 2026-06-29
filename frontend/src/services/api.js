import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20000,
})

let authGuardAttached = false
let authExpiredNotified = false

function isAuthExpiredMessage(message) {
  const text = String(message || '')
  return /登录已失效|请重新登录|未登录|token.*失效|token.*过期/i.test(text)
}

function notifyAuthExpired(onAuthExpired, message) {
  if (authExpiredNotified) return
  authExpiredNotified = true
  onAuthExpired?.(message)
  setTimeout(() => {
    authExpiredNotified = false
  }, 300)
}

export function attachAuthToken(getToken) {
  api.interceptors.request.use((config) => {
    const token = getToken?.()
    if (token) config.headers['X-Auth-Token'] = token
    return config
  })
}

export function attachAuthGuard(onAuthExpired) {
  if (authGuardAttached) return
  authGuardAttached = true

  api.interceptors.response.use(
    (response) => {
      const body = response?.data
      if (body?.success === false && isAuthExpiredMessage(body?.message)) {
        notifyAuthExpired(onAuthExpired, body?.message)
      }
      return response
    },
    (error) => {
      const status = error?.response?.status
      const message = error?.response?.data?.message || error?.message
      if (status === 401 || isAuthExpiredMessage(message)) {
        notifyAuthExpired(onAuthExpired, message)
      }
      return Promise.reject(error)
    },
  )
}
