/**
 * Central Axios instance.
 *
 * Every backend response is wrapped in ApiResponse<T>:
 *   { success, message, data, timestamp }
 *
 * The response interceptor unwraps .data automatically so services
 * receive the plain payload (e.g. RestaurantResponse, OrderResponse).
 *
 * Error handling:
 *  - 401 → clear tokens, redirect to /login
 *  - All other errors → reject with a plain Error(message) so callers
 *    can read err.message without worrying about Axios internals.
 */
import axios from 'axios'

const api = axios.create({
  // When the Vite dev proxy is active, use a relative /api path.
  // In production, point VITE_API_BASE_URL to the deployed backend.
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ── Request interceptor — attach JWT ──────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const raw = localStorage.getItem('token')
    if (raw) {
      // localStorage stores JSON-stringified values via useLocalStorage;
      // strip surrounding quotes if present.
      const token = raw.replace(/^"|"$/g, '')
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response interceptor — unwrap ApiResponse<T> ──────────────────────
api.interceptors.response.use(
  (response) => {
    // Backend always sends { success, message, data, timestamp }
    // Return the inner `data` field so service methods get the payload.
    const body = response.data
    if (body && typeof body === 'object' && 'data' in body) {
      return body.data
    }
    // Fallback: return the raw response body (e.g. plain strings)
    return body
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login'
        }
      }

      // Surface the backend's message if available
      const message =
        data?.message ||
        data?.error ||
        `Request failed with status ${status}`
      return Promise.reject(new Error(message))
    }

    if (error.request) {
      return Promise.reject(
        new Error('No response from server. Check your connection.')
      )
    }

    return Promise.reject(error)
  }
)

export default api
