// login(email, password)   -> POST /api/auth/login
// register(data)           -> POST /api/auth/register
// logout()                 -> clears token from localStorage
// refreshToken()           -> POST /api/auth/refresh-token

import api from './api'

const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password })
    return response.data
  },

  register: async (data) => {
    const response = await api.post('/auth/register', data)
    return response.data
  },

  logout: () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  },

  refreshToken: async () => {
    const response = await api.post('/auth/refresh-token')
    return response.data
  },
}

export default authService
