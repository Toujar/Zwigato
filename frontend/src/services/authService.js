/**
 * Auth Service
 *
 * POST /auth/register  → { accessToken, tokenType, expiresIn, user }
 * POST /auth/login     → { accessToken, tokenType, expiresIn, user }
 * POST /auth/refresh-token (header: Refresh-Token)
 *
 * The api interceptor unwraps ApiResponse.data automatically,
 * so these methods receive AuthResponse directly.
 */
import api from './api'

const authService = {
  /**
   * Register a new CUSTOMER account.
   * @param {{ name, email, password, phone, address? }} data
   * @returns {Promise<AuthResponse>} { accessToken, user, ... }
   */
  register: async (data) => {
    return api.post('/auth/register', data)
  },

  /**
   * Login with email + password.
   * @param {string} email
   * @param {string} password
   * @returns {Promise<AuthResponse>}
   */
  login: async (email, password) => {
    return api.post('/auth/login', { email, password })
  },

  /**
   * Refresh an expired access token using the stored refresh token.
   * @param {string} refreshToken
   * @returns {Promise<AuthResponse>}
   */
  refreshToken: async (refreshToken) => {
    return api.post('/auth/refresh-token', null, {
      headers: { 'Refresh-Token': refreshToken },
    })
  },

  // ── Password reset ────────────────────────────────────────────
  forgotPassword: async (email) =>
    api.post('/auth/forgot-password', { email }),

  verifyResetToken: async (token) =>
    api.post('/auth/verify-reset-token', { token }),

  resetPassword: async (token, newPassword) =>
    api.post('/auth/reset-password', { token, newPassword }),

  // ── OTP ──────────────────────────────────────────────────────
  sendOtp: async (email) =>
    api.post('/auth/otp/send', { email }),

  verifyOtp: async (email, otp) =>
    api.post('/auth/otp/verify', { email, otp }),
}

export default authService
