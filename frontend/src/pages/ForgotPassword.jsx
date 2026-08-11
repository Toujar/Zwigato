import { useState } from 'react'
import { Link } from 'react-router-dom'
import authService from '../services/authService'
import { useToast } from '../context/ToastContext'
import Loader from '../components/common/Loader'

const ForgotPassword = () => {
  const [email, setEmail]     = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent]       = useState(false)
  const toast = useToast()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await authService.forgotPassword(email)
      setSent(true)
    } catch (err) {
      toast.error(err.message || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  if (sent) {
    return (
      <div className="text-center">
        <div className="w-16 h-16 rounded-2xl flex items-center justify-center text-3xl mx-auto mb-5"
          style={{ background:'rgba(34,197,94,0.12)', border:'1px solid rgba(34,197,94,0.25)' }}>
          📧
        </div>
        <h1 className="text-2xl font-black text-secondary mb-2">Check Your Email</h1>
        <p className="text-slate-500 text-sm mb-1">
          If <span className="font-semibold text-secondary">{email}</span> is registered,
          we sent a reset link to that address.
        </p>
        <p className="text-slate-400 text-xs mb-6">
          The link expires in 15 minutes. Check your spam folder too.
        </p>
        <Link to="/login" className="btn-primary px-8">Back to Login</Link>
      </div>
    )
  }

  return (
    <div>
      <div className="text-center mb-7">
        <h1 className="text-2xl font-black text-secondary">Forgot Password?</h1>
        <p className="text-slate-500 mt-1 text-sm">
          Enter your email and we'll send a reset link
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="block text-slate-700 font-semibold mb-1.5 text-sm">
            Email Address
          </label>
          <input
            type="email" value={email} onChange={e => setEmail(e.target.value)}
            className="input-glass" placeholder="you@example.com"
            required autoComplete="email"
          />
        </div>

        <button type="submit" disabled={loading}
          className="btn-primary w-full flex justify-center items-center gap-2 py-3.5">
          {loading ? <Loader size="sm" /> : 'Send Reset Link'}
        </button>

        <p className="text-center text-slate-500 text-sm">
          Remembered it?{' '}
          <Link to="/login" className="text-primary font-bold hover:underline">Sign In</Link>
        </p>
      </form>
    </div>
  )
}

export default ForgotPassword
