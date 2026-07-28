/**
 * AuthContext
 *
 * Stores the authenticated user and JWT token in localStorage via
 * useLocalStorage so the session survives page refreshes.
 *
 * The backend returns AuthResponse:
 *   { accessToken, refreshToken, tokenType, expiresIn, user }
 *
 * login(authResponse) — accepts the full AuthResponse from the backend
 *                        and extracts accessToken + user.
 * logout()            — clears both keys.
 * isAuthenticated     — true when a token exists.
 */
import { createContext, useContext } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'

const AuthContext = createContext()

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useLocalStorage('user', null)
  const [token, setToken] = useLocalStorage('token', null)

  /**
   * Call this after a successful login or register.
   * Accepts the full AuthResponse object from the backend.
   *
   * @param {AuthResponse} authResponse  - { accessToken, user, ... }
   */
  const login = (authResponse) => {
    // Support both formats:
    //   login(authResponse)         — full AuthResponse object (normal flow)
    //   login(userObj, tokenStr)    — legacy / demo mode fallback
    if (authResponse && authResponse.accessToken) {
      setToken(authResponse.accessToken)
      setUser(authResponse.user)
    } else if (typeof authResponse === 'object' && arguments.length >= 2) {
      // eslint-disable-next-line prefer-rest-params
      setUser(authResponse)
      setToken(arguments[1])
    } else {
      setUser(authResponse)
    }
  }

  const logout = () => {
    setUser(null)
    setToken(null)
  }

  const isAuthenticated = !!token

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
