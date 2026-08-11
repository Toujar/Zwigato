/**
 * VerifyOtp page
 *
 * Shown after registration to verify the user's phone/email via OTP.
 * Receives email via navigation state: navigate('/verify-otp', { state: { email } })
 *
 * Flow:
 *  1. Backend sends OTP to email on registration
 *  2. User enters 6-digit code here
 *  3. On success → redirect to home
 *  4. "Resend" button re-requests a new OTP (rate-limited to 60s)
 */
import { useState, useEffect, useRef } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import authService from '../services/authService'
import { useToast } from '../context/ToastContext'
import Loader from '../components/common/Loader'

const RESEND_COOLDOWN = 60   // seconds

const VerifyOtp = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const toast    = useToast()

  const email    = location.state?.email || ''
  const [otp, setOtp]         = useState(['', '', '', '', '', ''])
  const [loading, setLoading] = useState(false)
  const [resendCd, setResendCd] = useState(RESEND_COOLDOWN)
  const [resending, setResending] = useState(false)
  const inputRefs = useRef([])
  const cdRef     = useRef(null)

  // Redirect if no email in state
  useEffect(() => {
    if (!email) navigate('/register', { replace: true })
  }, [email, navigate])

  // Countdown for resend button
  useEffect(() => {
    cdRef.current = setInterval(() => {
      setResendCd(c => {
        if (c <= 1) { clearInterval(cdRef.current); return 0 }
        return c - 1
      })
    }, 1000)
    return () => clearInterval(cdRef.current)
  }, [])

  const handleChange = (i, val) => {
    if (!/^\d*$/.test(val)) return
    const next = [...otp]
    next[i] = val.slice(-1)
    setOtp(next)
    if (val && i < 5) inputRefs.current[i + 1]?.focus()
  }

  const handleKeyDown = (i, e) => {
    if (e.key === 'Backspace' && !otp[i] && i > 0) {
      inputRefs.current[i - 1]?.focus()
    }
  }

  const handlePaste = (e) => {
    const text = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    if (text.length === 6) {
      setOtp(text.split(''))
      inputRefs.current[5]?.focus()
    }
  }

  const handleVerify = async () => {
    const code = otp.join('')
    if (code.length < 6) { toast.error('Enter all 6 digits'); return }
    setLoading(true)
    try {
      await authService.verifyOtp(email, code)
      toast.success('Phone verified successfully! 🎉')
      navigate('/', { replace: true })
    } catch (err) {
      toast.error(err.message || 'Invalid OTP. Try again.')
      setOtp(['', '', '', '', '', ''])
      inputRefs.current[0]?.focus()
    } finally {
      setLoading(false)
    }
  }

  const handleResend = async () => {
    setResending(true)
    try {
      await authService.sendOtp(email)
      toast.success('New OTP sent to your email')
      setResendCd(RESEND_COOLDOWN)
      setOtp(['', '', '', '', '', ''])
      inputRefs.current[0]?.focus()
      cdRef.current = setInterval(() => {
        setResendCd(c => { if (c <= 1) { clearInterval(cdRef.current); return 0 } return c - 1 })
      }, 1000)
    } catch (err) {
      toast.error(err.message || 'Failed to resend OTP')
    } finally {
      setResending(false)
    }
  }

  return (
    <div>
      <div className="text-center mb-7">
        <div className="text-4xl mb-3">📱</div>
        <h1 className="text-2xl font-black text-secondary">Verify Your Email</h1>
        <p className="text-slate-500 mt-1 text-sm">
          We sent a 6-digit code to
        </p>
        <p className="font-semibold text-secondary text-sm">{email}</p>
      </div>

      {/* OTP input boxes */}
      <div className="flex justify-center gap-3 mb-6" onPaste={handlePaste}>
        {otp.map((digit, i) => (
          <input
            key={i}
            ref={el => inputRefs.current[i] = el}
            type="text"
            inputMode="numeric"
            value={digit}
            onChange={e => handleChange(i, e.target.value)}
            onKeyDown={e => handleKeyDown(i, e)}
            maxLength={1}
            className="w-12 h-14 text-center text-2xl font-black rounded-2xl outline-none
                       transition-all duration-200 caret-primary"
            style={{
              background: digit ? 'rgba(14,165,233,0.10)' : 'rgba(224,242,254,0.50)',
              border: `2px solid ${digit ? '#0EA5E9' : 'rgba(186,230,253,0.60)'}`,
              boxShadow: digit ? '0 0 0 3px rgba(14,165,233,0.15)' : 'none',
            }}
          />
        ))}
      </div>

      <button
        onClick={handleVerify}
        disabled={loading || otp.join('').length < 6}
        className="btn-primary w-full flex justify-center items-center gap-2 py-3.5 mb-4">
        {loading ? <Loader size="sm" /> : 'Verify & Continue'}
      </button>

      {/* Resend */}
      <div className="text-center">
        {resendCd > 0 ? (
          <p className="text-slate-400 text-sm">
            Resend OTP in <span className="font-bold text-primary">{resendCd}s</span>
          </p>
        ) : (
          <button onClick={handleResend} disabled={resending}
            className="text-primary font-bold text-sm hover:underline flex items-center gap-1 mx-auto">
            {resending ? <Loader size="sm" /> : '↺ Resend OTP'}
          </button>
        )}
      </div>

      <p className="text-center text-slate-400 text-xs mt-4">
        Wrong email?{' '}
        <Link to="/register" className="text-primary hover:underline font-semibold">
          Go back
        </Link>
      </p>
    </div>
  )
}

export default VerifyOtp
