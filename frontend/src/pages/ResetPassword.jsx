import { useState, useEffect } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import authService from '../services/authService'
import { useToast } from '../context/ToastContext'
import Loader from '../components/common/Loader'

const ResetPassword = () => {
  const [searchParams]          = useSearchParams()
  const token                   = searchParams.get('token') || ''
  const [tokenValid, setTokenValid] = useState(null) // null=checking, true, false
  const [password, setPassword] = useState('')
  const [confirm, setConfirm]   = useState('')
  const [showPw, setShowPw]     = useState(false)
  const [loading, setLoading]   = useState(false)
  const [done, setDone]         = useState(false)
  const toast    = useToast()
  const navigate = useNavigate()

  // Verify token on mount
  useEffect(() => {
    if (!token) { setTokenValid(false); return }
    authService.verifyResetToken(token)
      .then(() => setTokenValid(true))
      .catch(() => setTokenValid(false))
  }, [token])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (password !== confirm) { toast.error('Passwords do not match'); return }
    if (password.length < 8)  { toast.error('Password must be at least 8 characters'); return }
    setLoading(true)
    try {
      await authService.resetPassword(token, password)
      setDone(true)
      setTimeout(() => navigate('/login', { replace: true }), 3000)
    } catch (err) {
      toast.error(err.message || 'Failed to reset password')
    } finally {
      setLoading(false)
    }
  }

  if (tokenValid === null) return (
    <div className="text-center py-8">
      <Loader fullPage={false} />
      <p className="text-slate-500 text-sm mt-3">Verifying your link…</p>
    </div>
  )

  if (!tokenValid) return (
    <div className="text-center">
      <div className="text-4xl mb-4">⚠️</div>
      <h1 className="text-xl font-black text-secondary mb-2">Link Expired</h1>
      <p className="text-slate-500 text-sm mb-6">
        This password reset link is invalid or has expired.<br />
        Reset links are valid for 15 minutes.
      </p>
      <Link to="/forgot-password" className="btn-primary px-8">Request New Link</Link>
    </div>
  )

  if (done) return (
    <div className="text-center">
      <div className="w-16 h-16 rounded-2xl flex items-center justify-center text-3xl mx-auto mb-5"
        style={{ background:'rgba(34,197,94,0.12)', border:'1px solid rgba(34,197,94,0.25)' }}>
        ✅
      </div>
      <h1 className="text-2xl font-black text-secondary mb-2">Password Updated!</h1>
      <p className="text-slate-500 text-sm mb-1">Your password has been changed successfully.</p>
      <p className="text-slate-400 text-xs">Redirecting to login in 3 seconds…</p>
    </div>
  )

  return (
    <div>
      <div className="text-center mb-7">
        <h1 className="text-2xl font-black text-secondary">Set New Password</h1>
        <p className="text-slate-500 mt-1 text-sm">Choose a strong password</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        {[
          { label:'New Password',     val:password, set:setPassword, auto:'new-password' },
          { label:'Confirm Password', val:confirm,  set:setConfirm,  auto:'new-password' },
        ].map(({ label, val, set, auto }) => (
          <div key={label}>
            <label className="block text-slate-700 font-semibold mb-1.5 text-sm">{label}</label>
            <div className="relative">
              <input
                type={showPw ? 'text' : 'password'} value={val}
                onChange={e => set(e.target.value)}
                className="input-glass pr-12" placeholder="At least 8 characters"
                required minLength={8} autoComplete={auto}
              />
              <button type="button" tabIndex={-1}
                onClick={() => setShowPw(v => !v)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-primary">
                {showPw ? '🙈' : '👁'}
              </button>
            </div>
          </div>
        ))}

        <button type="submit" disabled={loading}
          className="btn-primary w-full flex justify-center items-center gap-2 py-3.5">
          {loading ? <Loader size="sm" /> : 'Update Password'}
        </button>
      </form>
    </div>
  )
}

export default ResetPassword
