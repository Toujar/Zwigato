import { useToast } from '../../context/ToastContext'

const ICONS = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' }
const COLORS = {
  success: 'border-green-300/50 bg-white/90',
  error:   'border-red-300/50 bg-white/90',
  warning: 'border-yellow-300/50 bg-white/90',
  info:    'border-sky-300/50 bg-white/90',
}
const BAR = {
  success: 'bg-green-500',
  error:   'bg-red-500',
  warning: 'bg-yellow-500',
  info:    'bg-sky-500',
}

const ToastItem = ({ toast, onClose }) => (
  <div className={`relative flex items-center gap-3 px-4 py-3 rounded-2xl border shadow-glass-lg
                   backdrop-blur-xl animate-slide-up min-w-[260px] max-w-xs ${COLORS[toast.type] || COLORS.info}`}
    style={{ WebkitBackdropFilter: 'blur(20px)' }}>
    {/* Accent bar */}
    <div className={`absolute left-0 top-3 bottom-3 w-1 rounded-full ${BAR[toast.type] || BAR.info}`} />

    <span className="text-lg ml-1 shrink-0">{ICONS[toast.type] || ICONS.info}</span>
    <p className="flex-1 text-sm font-medium text-slate-700 leading-snug">{toast.message}</p>
    <button onClick={() => onClose(toast.id)}
      className="shrink-0 text-slate-400 hover:text-slate-600 transition-colors p-1 rounded-lg">
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
      </svg>
    </button>
  </div>
)

const ToastContainer = () => {
  const { toasts, removeToast } = useToast()
  if (!toasts.length) return null

  return (
    <div className="fixed bottom-5 right-5 z-[9999] flex flex-col gap-2.5 items-end">
      {toasts.map(t => <ToastItem key={t.id} toast={t} onClose={removeToast} />)}
    </div>
  )
}

export default ToastContainer
