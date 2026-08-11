/**
 * AuthContext
 *
 * Stores access token, refresh token, and user in localStorage.
 * The refresh token is read by api.js interceptor to silently
 * refresh the access token when it expires (24h window).
 */
import { createContext, useContext } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'

const AuthContext = createContext()

export const AuthProvider = ({ children }) => {
  const [user,         setUser]         = useLocalStorage('user',         null)
  const [token,        setToken]        = useLocalStorage('token',        null)
  const [refreshToken, setRefreshToken] = useLocalStorage('refreshToken', null)

  const login = (authResponse) => {
    if (authResponse?.accessToken) {
      setToken(authResponse.accessToken)
      setRefreshToken(authResponse.refreshToken || null)
      setUser(authResponse.user)
    } else {
      // Legacy fallback (profile update path)
      setUser(authResponse)
    }
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    setRefreshToken(null)
  }

  const isAuthenticated = !!token

  return (
    <AuthContext.Provider value={{ user, token, refreshToken, isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
