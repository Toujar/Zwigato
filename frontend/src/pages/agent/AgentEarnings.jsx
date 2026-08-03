import { useState, useEffect } from 'react'
import deliveryService from '../../services/deliveryService'
import Loader from '../../components/common/Loader'

// Delivery fee per order (flat ₹40 — matches backend constant)
const FEE = 40

const AgentEarnings = () => {
  const [orders, setOrders]   = useState([])
  const [loading, setLoading] = useState(true)
  const [period, setPeriod]   = useState('all') // all | today | week | month

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const res  = await deliveryService.getMyDeliveries({ page:0, size:200 })
        const list = res?.content ?? (Array.isArray(res) ? res : [])
        // Only count delivered orders for earnings
        setOrders(list.filter(o => o.status === 'DELIVERED'))
      } catch { setOrders([]) }
      finally { setLoading(false) }
    }
    load()
  }, [])

  // ── Helpers ──────────────────────────────────────────────────────
  const now    = new Date()
  const todayS = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const weekS  = todayS - 6 * 86400_000
  const monthS = new Date(now.getFullYear(), now.getMonth(), 1).getTime()

  const inPeriod = (order) => {
    if (period === 'all') return true
    const t = order.placedAt ? new Date(order.placedAt).getTime() : 0
    if (period === 'today') return t >= todayS
    if (period === 'week')  return t >= weekS
    if (period === 'month') return t >= monthS
    return true
  }

  const filtered  = orders.filter(inPeriod)
  const total     = filtered.length * FEE
  const allTotal  = orders.length * FEE
  const todayEarn = orders.filter(o => {
    const t = o.placedAt ? new Date(o.placedAt).getTime() : 0
    return t >= todayS
  }).length * FEE

  // Group filtered orders by date for the list
  const byDate = filtered.reduce((acc, o) => {
    const d = o.placedAt
      ? new Date(o.placedAt).toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' })
      : 'Unknown date'
    if (!acc[d]) acc[d] = []
    acc[d].push(o)
    return acc
  }, {})

  const PERIODS = [
    { key:'today', label:'Today' },
    { key:'week',  label:'This Week' },
    { key:'month', label:'This Month' },
    { key:'all',   label:'All Time' },
  ]

  if (loading) return <Loader fullPage />

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-black text-secondary">My Earnings</h1>
        <p className="text-slate-500 text-sm mt-1">₹{FEE} per successful delivery</p>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {[
          { label:'Total Deliveries', value: orders.length,   icon:'✅', color:'#22C55E' },
          { label:"Today's Earnings", value:`₹${todayEarn}`,  icon:'📅', color:'#0EA5E9' },
          { label:'All-time Earned',  value:`₹${allTotal}`,   icon:'💰', color:'#8B5CF6' },
          { label:'Avg per Day',
            value: orders.length > 0
              ? `₹${Math.round(allTotal / Math.max(
                  Math.ceil((Date.now() - new Date(orders[orders.length-1]?.placedAt || Date.now()).getTime()) / 86400_000),
                  1)
                )}`
              : '₹0',
            icon:'📈', color:'#F97316' },
        ].map(({ label, value, icon, color }) => (
          <div key={label} className="glass p-4">
            <div className="flex items-center gap-2 mb-2">
              <div className="w-8 h-8 rounded-xl flex items-center justify-center text-sm"
                style={{ background:`${color}22`, border:`1px solid ${color}33` }}>
                {icon}
              </div>
              <span className="text-slate-500 text-xs font-medium">{label}</span>
            </div>
            <p className="text-2xl font-black text-secondary">{value}</p>
          </div>
        ))}
      </div>

      {/* Period filter */}
      <div className="flex gap-2 flex-wrap mb-6">
        {PERIODS.map(p => (
          <button key={p.key} onClick={() => setPeriod(p.key)}
            className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all ${
              period === p.key ? 'btn-primary' : 'btn-glass'
            }`}>
            {p.label}
            <span className="ml-1.5 opacity-60 text-xs">
              (₹{orders.filter(inPeriod).length * FEE})
            </span>
          </button>
        ))}
      </div>

      {/* Highlighted total for selected period */}
      <div className="glass p-5 mb-6 flex items-center justify-between">
        <div>
          <p className="text-slate-500 text-sm">
            {PERIODS.find(p => p.key === period)?.label} earnings
          </p>
          <p className="text-4xl font-black text-green-600 mt-1">₹{total}</p>
        </div>
        <div className="text-right">
          <p className="text-slate-400 text-sm">{filtered.length} deliveries</p>
          <p className="text-slate-400 text-xs mt-0.5">₹{FEE} × {filtered.length}</p>
        </div>
      </div>

      {/* Delivery history grouped by date */}
      {filtered.length === 0 ? (
        <div className="glass-subtle rounded-2xl p-10 text-center">
          <div className="text-4xl mb-3">📭</div>
          <p className="font-semibold text-secondary mb-1">No deliveries in this period</p>
          <p className="text-slate-400 text-sm">Accept orders to start earning!</p>
        </div>
      ) : (
        <div className="space-y-6">
          {Object.entries(byDate).map(([date, dayOrders]) => (
            <div key={date}>
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-bold text-secondary text-sm">{date}</h3>
                <span className="text-green-600 font-black text-sm">
                  +₹{dayOrders.length * FEE}
                </span>
              </div>
              <div className="space-y-2">
                {dayOrders.map(order => (
                  <div key={order.id}
                    className="glass-white flex items-center justify-between p-4 rounded-2xl">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-9 h-9 rounded-xl flex items-center justify-center text-lg shrink-0"
                        style={{ background:'rgba(34,197,94,0.12)', border:'1px solid rgba(34,197,94,0.20)' }}>
                        ✅
                      </div>
                      <div className="min-w-0">
                        <p className="font-semibold text-secondary text-sm">
                          Order #{String(order.id).slice(-6)}
                        </p>
                        <p className="text-slate-400 text-xs truncate">
                          {order.restaurantName} → {order.deliveryAddress?.slice(0,35)}…
                        </p>
                      </div>
                    </div>
                    <div className="shrink-0 text-right ml-3">
                      <p className="font-black text-green-600">+₹{FEE}</p>
                      <p className="text-slate-400 text-xs">
                        {order.placedAt
                          ? new Date(order.placedAt).toLocaleTimeString('en-IN',
                              { hour:'2-digit', minute:'2-digit' })
                          : ''}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default AgentEarnings
