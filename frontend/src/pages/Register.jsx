import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import authService from '../services/authService'
import Loader from '../components/common/Loader'

// Move Field component outside to prevent recreation on each render
const Field = ({ label, type = 'text', value, onChange, placeholder, autoComplete, pattern, minLength, multiline = false }) => (
  <div>
    <label className="block text-slate-700 font-semibold mb-1.5 text-sm">{label}</label>
    {multiline ? (
      <textarea
        value={value} 
        onChange={onChange}
        className="input-glass resize-none"
        placeholder={placeholder} 
        required
        autoComplete={autoComplete}
        rows={3}
      />
    ) : (
      <input
        type={type} 
        value={value} 
        onChange={onChange}
        className="input-glass"
        placeholder={placeholder} 
        required
        autoComplete={autoComplete}
        pattern={pattern}
        minLength={minLength}
      />
    )}
  </div>
)

const Register = () => {
  const [name, setName]                       = useState('')
  const [email, setEmail]                     = useState('')
  const [phone, setPhone]                     = useState('')
  const [address, setAddress]                 = useState('')
  const [password, setPassword]               = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPw, setShowPw]                   = useState(false)
  const [loading, setLoading]                 = useState(false)

  const { login } = useAuth()
  const toast     = useToast()
  const navigate  = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (password !== confirmPassword) { toast.error('Passwords do not match'); return }
    if (password.length < 8) { toast.error('Password must be at least 8 characters'); return }
    if (address.trim().length < 10) { toast.error('Please enter a complete address'); return }

    setLoading(true)
    try {
      const authResponse = await authService.register({ name, email, phone, address, password })
      login(authResponse)
      toast.success(`Welcome, ${authResponse.user?.name || name}!`)

      // Send OTP for email/phone verification (non-blocking — user can skip)
      try {
        await authService.sendOtp(email)
        navigate('/verify-otp', { state: { email }, replace: true })
      } catch {
        // OTP failed (email not configured) — still proceed to home
        navigate('/', { replace: true })
      }
    } catch (err) {
      toast.error(err.message || 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div className="text-center mb-7">
        <h1 className="text-2xl font-black text-secondary">Create Account</h1>
        <p className="text-slate-500 mt-1 text-sm">Join Zwigato — it&apos;s free</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <Field 
          label="Full Name"     
          value={name}  
          onChange={(e) => setName(e.target.value)}
          placeholder="Your full name" 
          autoComplete="name" 
        />

        <Field 
          label="Email Address" 
          type="email" 
          value={email} 
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@example.com" 
          autoComplete="email" 
        />

        <Field 
          label="Phone Number"  
          type="tel"  
          value={phone} 
          onChange={(e) => setPhone(e.target.value)}
          placeholder="10-digit mobile number" 
          autoComplete="tel" 
          pattern="^[+]?[0-9]{10,15}$" 
        />

        <Field 
          label="Address"  
          value={address} 
          onChange={(e) => setAddress(e.target.value)}
          placeholder="Enter your complete delivery address (House/Flat, Street, Area, City)" 
          autoComplete="street-address" 
          multiline={true}
        />

        {/* Password with toggle */}
        <div>
          <label className="block text-slate-700 font-semibold mb-1.5 text-sm">Password</label>
          <div className="relative">
            <input
              type={showPw ? 'text' : 'password'}
              value={password} 
              onChange={(e) => setPassword(e.target.value)}
              className="input-glass pr-12"
              placeholder="At least 8 characters" 
              required 
              autoComplete="new-password" 
              minLength={8}
            />
            <button 
              type="button" 
              onClick={() => setShowPw((v) => !v)} 
              tabIndex={-1}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-primary transition-colors"
            >
              {showPw
                ? <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" /></svg>
                : <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
              }
            </button>
          </div>
        </div>

        <Field 
          label="Confirm Password" 
          type="password" 
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          placeholder="Re-enter password" 
          autoComplete="new-password" 
        />

        <button 
          type="submit" 
          disabled={loading}
          className="btn-primary w-full flex justify-center items-center gap-2 py-3.5 mt-2"
        >
          {loading ? <Loader size="sm" /> : 'Create Account'}
        </button>

        <p className="text-center text-slate-500 text-sm pt-1">
          Already have an account?{' '}
          <Link to="/login" className="text-primary font-bold hover:underline">Sign In</Link>
        </p>
      </form>
    </div>
  )
}

export default Register
