import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import authService from '../services/authService'
import Loader from '../components/common/Loader'

const Login = () => {
  const [email, setEmail]       = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading]   = useState(false)

  const { login }  = useAuth()
  const toast      = useToast()
  const navigate   = useNavigate()
  const location   = useLocation()

  // Redirect back to where the user came from (or home)
  const from = location.state?.from?.pathname || '/'

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      // authService.login returns AuthResponse (unwrapped by api interceptor)
      const authResponse = await authService.login(email, password)
      login(authResponse)
      toast.success(`Welcome back, ${authResponse.user?.name || 'there'}!`)
      navigate(from, { replace: true })
    } catch (err) {
      // Show the backend error message or a friendly fallback
      toast.error(err.message || 'Invalid email or password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-secondary">Welcome Back</h1>
        <p className="text-gray-500 mt-2">Sign in to your account</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-gray-700 font-medium mb-2">
            Email Address
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="Enter your email"
            required
            autoComplete="email"
          />
        </div>

        <div>
          <label className="block text-gray-700 font-medium mb-2">
            Password
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="Enter your password"
            required
            autoComplete="current-password"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="btn-primary w-full flex justify-center items-center gap-2"
        >
          {loading ? <Loader size="sm" /> : 'Sign In'}
        </button>

        <p className="text-center text-gray-600 text-sm pt-2">
          Don't have an account?{' '}
          <Link to="/register" className="text-primary font-semibold hover:underline">
            Sign Up
          </Link>
        </p>
      </form>
    </div>
  )
}

export default Login
