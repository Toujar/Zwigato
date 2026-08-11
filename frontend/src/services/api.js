/**
 * Central Axios instance with automatic JWT refresh rotation.
 *
 * Flow:
 *  1. Every request attaches the access token from localStorage.
 *  2. On 401 → attempt one silent refresh using the stored refresh token.
 *  3. If refresh succeeds → store new tokens, replay the original request.
 *  4. If refresh fails → clear tokens, redirect to /login.
 *
 * This means users are never logged out mid-session just because
 * their 24-hour access token expired — as long as the 7-day
 * refresh token is still valid.
 */
import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ── Helpers ───────────────────────────────────────────────────────────

const getRaw = (key) => {
  const raw = localStorage.getItem(key)
  return raw ? raw.replace(/^"|"$/g, '') : null
}

const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
}

// Prevent multiple concurrent refresh calls
let isRefreshing = false
let failedQueue  = []   // { resolve, reject }[]

const processQueue = (error, token = null) => {
  failedQueue.forEach(({ resolve, reject }) =>
    error ? reject(error) : resolve(token)
  )
  failedQueue = []
}

// ── Request interceptor — attach access token ─────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = getRaw('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response interceptor — unwrap + silent refresh on 401 ─────────────
api.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'data' in body) return body.data
    return body
  },
  async (error) => {
    const originalRequest = error.config

    // Only attempt refresh on 401 and only once per request
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh-token') &&
      !originalRequest.url?.includes('/auth/login')
    ) {
      const refreshToken = getRaw('refreshToken')

      if (!refreshToken) {
        // No refresh token stored — hard logout
        clearAuth()
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login'
        }
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // Another refresh is already in flight — queue this request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((newToken) => {
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return api(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // Call refresh endpoint directly (bypass our interceptor to avoid loops)
        const res = await axios.post(
          `${BASE_URL}/auth/refresh-token`,
          null,
          { headers: { 'Refresh-Token': refreshToken } }
        )

        // Unwrap ApiResponse envelope
        const authData = res.data?.data ?? res.data
        const newAccessToken  = authData.accessToken
        const newRefreshToken = authData.refreshToken

        // Persist new tokens
        localStorage.setItem('token',        JSON.stringify(newAccessToken))
        localStorage.setItem('refreshToken', JSON.stringify(newRefreshToken))
        if (authData.user) {
          localStorage.setItem('user', JSON.stringify(authData.user))
        }

        // Update default headers for future requests
        api.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`

        // Replay all queued requests with the new token
        processQueue(null, newAccessToken)

        // Replay the original request
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        clearAuth()
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login'
        }
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // Non-401 errors — surface backend message
    if (error.response) {
      const { data } = error.response
      const message = data?.message || data?.error ||
        `Request failed with status ${error.response.status}`
      return Promise.reject(new Error(message))
    }

    if (error.request) {
      return Promise.reject(new Error('No response from server. Check your connection.'))
    }

    return Promise.reject(error)
  }
)

export default api
