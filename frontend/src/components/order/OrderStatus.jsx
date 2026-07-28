const STEPS = [
  { key: 'PLACED',           label: 'Order Placed',    icon: '📝' },
  { key: 'CONFIRMED',        label: 'Confirmed',       icon: '✅' },
  { key: 'PREPARING',        label: 'Preparing',       icon: '👨‍🍳' },
  { key: 'OUT_FOR_DELIVERY', label: 'Out for Delivery',icon: '🛵' },
  { key: 'DELIVERED',        label: 'Delivered',       icon: '🎉' },
]
const ORDER = STEPS.map(s => s.key)

const OrderStatus = ({ currentStatus = 'PLACED' }) => {
  const upper = currentStatus?.toUpperCase()

  if (upper === 'CANCELLED') {
    return (
      <div className="glass-white border border-red-200/50 p-6 text-center">
        <div className="text-4xl mb-3">❌</div>
        <h3 className="text-lg font-bold text-red-600 mb-1">Order Cancelled</h3>
        <p className="text-slate-400 text-sm">This order has been cancelled.</p>
      </div>
    )
  }

  const currentIdx = ORDER.indexOf(upper)

  return (
    <div className="relative">
      {/* Progress line */}
      <div className="hidden md:block absolute top-6 left-[10%] right-[10%] h-0.5"
        style={{ background: 'rgba(186,230,253,0.40)' }} />
      <div className="hidden md:block absolute top-6 h-0.5 transition-all duration-700"
        style={{
          left: '10%',
          width: `${(currentIdx / (STEPS.length - 1)) * 80}%`,
          background: 'linear-gradient(90deg,#0EA5E9,#38BDF8)',
        }} />

      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 md:gap-0 relative z-10">
        {STEPS.map((step, i) => {
          const done    = i <= currentIdx
          const current = i === currentIdx
          return (
            <div key={step.key} className="flex md:flex-col items-center gap-3 md:gap-2 md:w-1/5">
              <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-xl
                               font-bold transition-all duration-500 shrink-0 ${
                current ? 'shadow-float scale-110 animate-pulse' : ''
              }`}
                style={done
                  ? { background: 'linear-gradient(135deg,#0EA5E9,#38BDF8)', boxShadow: current ? '0 4px 16px rgba(14,165,233,0.45)' : undefined }
                  : { background: 'rgba(186,230,253,0.35)', border: '1.5px solid rgba(186,230,253,0.60)' }
                }>
                <span className={done ? 'text-white' : 'opacity-40'}>{step.icon}</span>
              </div>
              <p className={`text-xs font-semibold text-center transition-colors md:mt-1 ${
                done ? 'text-primary' : 'text-slate-400'
              }`}>{step.label}</p>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default OrderStatus
