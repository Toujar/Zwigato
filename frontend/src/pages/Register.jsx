import { useState, useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import authService from '../services/authService'
import Loader from '../components/common/Loader'

// Icon components
const UserIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
  </svg>
)

const EmailIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
  </svg>
)

const PhoneIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
  </svg>
)

const LocationIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
  </svg>
)

const LockIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
  </svg>
)

// Move Field component outside to prevent recreation on each render
const Field = ({ label, type = 'text', value, onChange, placeholder, autoComplete, pattern, minLength, multiline = false, icon }) => (
  <div>
    <label className="block text-slate-700 font-semibold mb-1.5 text-sm">{label}</label>
    <div className="relative">
      {icon && (
        <div className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none">
          {icon}
        </div>
      )}
      {multiline ? (
        <textarea
          value={value} 
          onChange={onChange}
          className={`input-glass resize-none ${icon ? 'pl-12' : ''}`}
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
          className={`input-glass ${icon ? 'pl-12' : ''}`}
          placeholder={placeholder} 
          required
          autoComplete={autoComplete}
          pattern={pattern}
          minLength={minLength}
        />
      )}
    </div>
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
  const [showConfirmPw, setShowConfirmPw]     = useState(false)
  const [loading, setLoading]                 = useState(false)

  const { login } = useAuth()
  const toast     = useToast()
  const navigate  = useNavigate()

  // Password strength calculator
  const passwordStrength = useMemo(() => {
    if (!password) return { score: 0, label: '', color: '' }
    
    let score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++
    if (/\d/.test(password)) score++
    if (/[^A-Za-z0-9]/.test(password)) score++
    
    if (score <= 2) return { score, label: 'Weak', color: 'bg-red-500' }
    if (score === 3) return { score, label: 'Fair', color: 'bg-orange-500' }
    if (score === 4) return { score, label: 'Good', color: 'bg-yellow-500' }
    return { score, label: 'Strong', color: 'bg-green-500' }
  }, [password])

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
    <div className="relative">
      {/* Animated background blobs */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none -z-10">
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-sky-200 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob"></div>
        <div className="absolute top-0 right-1/4 w-96 h-96 bg-blue-200 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000"></div>
        <div className="absolute bottom-0 left-1/3 w-96 h-96 bg-cyan-200 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-4000"></div>
      </div>

      <div className="text-center mb-7">
        <h1 className="text-3xl font-black text-secondary mb-2">Create Account</h1>
        <p className="text-slate-500 text-sm">Join Zwigato and start ordering delicious food</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <Field 
          label="Full Name"     
          value={name}  
          onChange={(e) => setName(e.target.value)}
          placeholder="Your full name" 
          autoComplete="name"
          icon={<UserIcon />}
        />

        <Field 
          label="Email Address" 
          type="email" 
          value={email} 
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@example.com" 
          autoComplete="email"
          icon={<EmailIcon />}
        />

        <Field 
          label="Phone Number"  
          type="tel"  
          value={phone} 
          onChange={(e) => setPhone(e.target.value)}
          placeholder="10-digit mobile number" 
          autoComplete="tel" 
          pattern="^[+]?[0-9]{10,15}$"
          icon={<PhoneIcon />}
        />

        <Field 
          label="Delivery Address"  
          value={address} 
          onChange={(e) => setAddress(e.target.value)}
          placeholder="House/Flat, Street, Area, City" 
          autoComplete="street-address" 
          multiline={true}
          icon={<LocationIcon />}
        />

        {/* Password with toggle and strength indicator */}
        <div>
          <label className="block text-slate-700 font-semibold mb-1.5 text-sm">Password</label>
          <div className="relative">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none">
              <LockIcon />
            </div>
            <input
              type={showPw ? 'text' : 'password'}
              value={password} 
              onChange={(e) => setPassword(e.target.value)}
              className="input-glass pl-12 pr-12"
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
          
          {/* Password strength indicator */}
          {password && (
            <div className="mt-2">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs text-slate-500">Password strength:</span>
                <span className={`text-xs font-semibold ${
                  passwordStrength.score <= 2 ? 'text-red-600' :
                  passwordStrength.score === 3 ? 'text-orange-600' :
                  passwordStrength.score === 4 ? 'text-yellow-600' : 'text-green-600'
                }`}>
                  {passwordStrength.label}
                </span>
              </div>
              <div className="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                <div 
                  className={`h-full transition-all duration-300 ${passwordStrength.color}`}
                  style={{ width: `${(passwordStrength.score / 5) * 100}%` }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Confirm Password with toggle */}
        <div>
          <label className="block text-slate-700 font-semibold mb-1.5 text-sm">Confirm Password</label>
          <div className="relative">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none">
              <LockIcon />
            </div>
            <input
              type={showConfirmPw ? 'text' : 'password'}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="input-glass pl-12 pr-12"
              placeholder="Re-enter password" 
              required
              autoComplete="new-password"
            />
            <button 
              type="button" 
              onClick={() => setShowConfirmPw((v) => !v)} 
              tabIndex={-1}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-primary transition-colors"
            >
              {showConfirmPw
                ? <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" /></svg>
                : <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
              }
            </button>
          </div>
        </div>

        {/* Trust indicators */}
        <div className="flex items-center gap-4 pt-2 pb-1 text-xs text-slate-500">
          <div className="flex items-center gap-1.5">
            <svg className="w-4 h-4 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <span>Secure & encrypted</span>
          </div>
          <div className="flex items-center gap-1.5">
            <svg className="w-4 h-4 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            <span>Lightning fast setup</span>
          </div>
        </div>

        <button 
          type="submit" 
          disabled={loading}
          className="btn-primary w-full flex justify-center items-center gap-2 py-3.5 mt-2"
        >
          {loading ? <Loader size="sm" /> : (
            <>
              <span>Create Account</span>
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
              </svg>
            </>
          )}
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
