import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import authService from '../services/authService'
import Loader from '../components/common/Loader'

const Register = () => {
  const [name, setName]                   = useState('')
  const [email, setEmail]                 = useState('')
  const [phone, setPhone]                 = useState('')
  const [password, setPassword]           = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading]             = useState(false)

  const { login } = useAuth()
  const toast     = useToast()
  const navigate  = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (password !== confirmPassword) {
      toast.error('Passwords do not match')
      return
    }
    if (password.length < 8) {
      toast.error('Password must be at least 8 characters')
      return
    }

    setLoading(true)
    try {
      // backend returns AuthResponse with accessToken + user
      const authResponse = await authService.register({ name, email, phone, password })
      login(authResponse)
      toast.success(`Welcome, ${authResponse.user?.name || name}! Account created.`)
      navigate('/', { replace: true })
    } catch (err) {
      toast.error(err.message || 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-secondary">Create Account</h1>
        <p className="text-gray-500 mt-2">Sign up to get started</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-gray-700 font-medium mb-1 text-sm">Full Name</label>
          <input
            type="text" value={name} onChange={(e) => setName(e.target.value)}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="Enter your name" required autoComplete="name"
          />
        </div>

        <div>
          <label className="block text-gray-700 font-medium mb-1 text-sm">Email Address</label>
          <input
            type="email" value={email} onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="Enter your email" required autoComplete="email"
          />
        </div>

        <div>
          <label className="block text-gray-700 font-medium mb-1 text-sm">Phone Number</label>
          <input
            type="tel" value={phone} onChange={(e) => setPhone(e.target.value)}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="e.g. 9876543210" required autoComplete="tel"
            pattern="^[+]?[0-9]{10,15}$"
          />
        </div>

        <div>
          <label className="block text-gray-700 font-medium mb-1 text-sm">Password</label>
          <input
            type="password" value={password} onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="At least 8 characters" required autoComplete="new-password"
            minLength={8}
          />
        </div>

        <div>
          <label className="block text-gray-700 font-medium mb-1 text-sm">Confirm Password</label>
          <input
            type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="Re-enter password" required autoComplete="new-password"
          />
        </div>

        <button
          type="submit" disabled={loading}
          className="btn-primary w-full flex justify-center items-center gap-2 mt-6"
        >
          {loading ? <Loader size="sm" /> : 'Create Account'}
        </button>

        <p className="text-center text-gray-600 text-sm pt-2">
          Already have an account?{' '}
          <Link to="/login" className="text-primary font-semibold hover:underline">Sign In</Link>
        </p>
      </form>
    </div>
  )
}

export default Register
