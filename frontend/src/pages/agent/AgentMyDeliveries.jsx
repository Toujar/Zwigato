import { useState, useEffect, useRef } from 'react'
import { useToast } from '../../context/ToastContext'
import deliveryService from '../../services/deliveryService'
import { geocodeAddress, getDrivingRoute } from '../../services/geoService'
import Loader from '../../components/common/Loader'

const STATUS_STYLE = {
  OUT_FOR_DELIVERY: 'badge-blue',
  DELIVERED:        'badge-green',
  CANCELLED:        'badge-red',
  PLACED:           'badge-gray',
  CONFIRMED:        'badge-blue',
  PREPARING:        'badge-orange',
}

/**
 * Returns seconds remaining from deliveryStartedAt until totalMinutes elapses.
 * Clamps to 0 so it never goes negative.
 */
const calcSecsLeft = (deliveryStartedAt, totalMinutes) => {
  if (!deliveryStartedAt || !totalMinutes) return 0
  const elapsed = Date.now() - new Date(deliveryStartedAt).getTime()
  return Math.max(0, Math.round((totalMinutes * 60 * 1000 - elapsed) / 1000))
}

const formatCountdown = (secs) => {
  if (secs <= 0) return null          // time's up — show the button
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return m > 0 ? `${m}m ${s}s` : `${s}s`
}

/** Per-order countdown — own component so it re-renders independently */
const DeliveryTimer = ({ order, onMarkDelivered, marking }) => {
  const dbMins    = order.restaurantDeliveryTime || 30
  const [totalMins, setTotalMins] = useState(dbMins)
  const totalMinsRef = useRef(dbMins)          // ← always current for the interval closure
  const [routeResolved, setRouteResolved] = useState(false)
  const [secs, setSecs] = useState(() => calcSecsLeft(order.updatedAt, dbMins))
  const timerRef  = useRef(null)

  // ── Geocode + route calculation ──────────────────────────────
  useEffect(() => {
    if (order.status !== 'OUT_FOR_DELIVERY') return
    if (!order.restaurantAddress || !order.deliveryAddress) return

    let cancelled = false

    const fetchRoute = async () => {
      try {
        const [origin, dest] = await Promise.all([
          geocodeAddress(order.restaurantAddress),
          geocodeAddress(order.deliveryAddress),
        ])
        if (cancelled || !origin || !dest) return

        const route = await getDrivingRoute(origin, dest)
        if (cancelled || !route) return

        const mins = parseInt(route.durationMin, 10)
        if (!isNaN(mins) && mins > 0) {
          totalMinsRef.current = mins          // update ref first
          setTotalMins(mins)                   // then state for render
          setSecs(calcSecsLeft(order.updatedAt, mins))
          setRouteResolved(true)
        }
      } catch { /* keep DB estimate */ }
    }

    fetchRoute()
    return () => { cancelled = true }
  }, [order.id, order.restaurantAddress, order.deliveryAddress, order.updatedAt, order.status])

  // ── Second-by-second tick — reads from ref, never stale ──────
  useEffect(() => {
    if (order.status !== 'OUT_FOR_DELIVERY') return
    clearInterval(timerRef.current)
    timerRef.current = setInterval(() => {
      const left = calcSecsLeft(order.updatedAt, totalMinsRef.current)
      setSecs(left)
      if (left === 0) clearInterval(timerRef.current)
    }, 1000)
    return () => clearInterval(timerRef.current)
  }, [order.id, order.updatedAt, order.status])
  // Note: totalMins NOT in deps — interval reads from ref, not closure state

  const fee         = Number(order.deliveryFee ?? 40)
  const isActive    = order.status === 'OUT_FOR_DELIVERY'
  const timeUp      = secs <= 0
  const countdown   = formatCountdown(secs)
  const progress    = totalMins > 0
    ? Math.min(1, 1 - secs / (totalMins * 60))
    : 1

  return (
    <>
      {/* Delivery timer bar — only while active */}
      {isActive && (
        <div className="mb-4">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-xs font-semibold text-slate-500">
              {timeUp
                ? '🎉 Estimated delivery time reached!'
                : `⏱ Estimated: ${totalMins} min${totalMins !== 1 ? 's' : ''}${routeResolved ? ' (route)' : ''}`}
            </span>
            <span className={`text-xs font-black ${timeUp ? 'text-green-600 animate-pulse' : 'text-primary'}`}>
              {timeUp ? 'Time is up!' : countdown + ' left'}
            </span>
          </div>
          <div className="h-1.5 rounded-full overflow-hidden"
            style={{ background: 'rgba(186,230,253,0.35)' }}>
            <div
              className="h-full rounded-full transition-all duration-1000"
              style={{
                width: `${progress * 100}%`,
                background: timeUp
                  ? 'linear-gradient(90deg,#22C55E,#4ADE80)'
                  : 'linear-gradient(90deg,#0EA5E9,#38BDF8)',
              }}
            />
          </div>
        </div>
      )}

      {/* Mark delivered — LOCKED until estimated time elapses */}
      {isActive && (
        timeUp ? (
          /* ✅ Unlocked */
          <button
            onClick={() => onMarkDelivered(order.id)}
            disabled={marking === order.id}
            className="btn-primary flex items-center gap-2 px-5 py-2.5 text-sm animate-scale-in"
            style={{
              background: 'linear-gradient(135deg,#22C55E,#4ADE80)',
              boxShadow: '0 4px 14px rgba(34,197,94,0.40)',
            }}
          >
            {marking === order.id
              ? <Loader size="sm" />
              : <><span>Mark as Delivered</span><span>✓</span></>
            }
          </button>
        ) : (
          /* 🔒 Locked — shows countdown */
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 px-5 py-2.5 rounded-2xl text-sm font-semibold
                            cursor-not-allowed select-none"
              style={{
                background: 'rgba(186,230,253,0.35)',
                border: '1.5px solid rgba(186,230,253,0.55)',
                color: '#94A3B8',
              }}>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
              </svg>
              <span>Mark as Delivered</span>
            </div>
            <span className="text-xs text-slate-400">
              Unlocks in <span className="font-bold text-primary">{countdown}</span>
            </span>
          </div>
        )
      )}

      {order.status === 'DELIVERED' && (
        <div className="flex items-center gap-2 text-green-600 text-sm font-semibold">
          <span>✅</span> Delivered · ₹{fee} earned
        </div>
      )}
    </>
  )
}

/* ── Main page ──────────────────────────────────────────────────── */

const AgentMyDeliveries = () => {
  const toast = useToast()
  const [orders, setOrders]   = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter]   = useState('ALL')
  const [marking, setMarking] = useState(null)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const res  = await deliveryService.getMyDeliveries({ page: 0, size: 50 })
        const list = res?.content ?? (Array.isArray(res) ? res : [])
        setOrders(list)
      } catch (err) { toast.error(err.message || 'Failed to load deliveries') }
      finally { setLoading(false) }
    }
    load()
  }, []) // eslint-disable-line

  const handleMarkDelivered = async (orderId) => {
    setMarking(orderId)
    try {
      const updated = await deliveryService.markDelivered(orderId)
      setOrders(l => l.map(o => o.id === orderId
        ? { ...o, status: updated.status || 'DELIVERED' } : o))
      toast.success(`Order #${String(orderId).slice(-6)} marked as Delivered 🎉`)
    } catch (err) { toast.error(err.message || 'Failed to update status') }
    finally { setMarking(null) }
  }

  const filtered = filter === 'ALL' ? orders : orders.filter(o => o.status === filter)
  const FILTERS  = ['ALL', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED']

  if (loading) return <Loader fullPage />

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-black text-secondary">My Deliveries</h1>
        <p className="text-slate-500 text-sm mt-1">{orders.length} total assignments</p>
      </div>

      {/* Filter pills */}
      <div className="flex gap-2 flex-wrap mb-6">
        {FILTERS.map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${
              filter === s ? 'btn-primary' : 'btn-glass'
            }`}>
            {s.replace(/_/g,' ')}
            <span className="ml-1 opacity-60">
              ({s === 'ALL' ? orders.length : orders.filter(o => o.status === s).length})
            </span>
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="glass text-center py-16 px-8">
          <div className="text-5xl mb-4">🛵</div>
          <h3 className="text-xl font-bold text-secondary mb-2">No deliveries here</h3>
          <p className="text-slate-500">Accept available orders to start delivering!</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map(order => {
            const items   = order.items ?? order.orderItems ?? []
            const fee     = Number(order.deliveryFee ?? 40)
            const dateStr = order.placedAt
              ? new Date(order.placedAt).toLocaleString('en-IN',
                  { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
              : '—'
            const isActive = order.status === 'OUT_FOR_DELIVERY'

            return (
              <div key={order.id}
                className={`glass p-5 transition-all ${isActive ? 'ring-1 ring-green-400/40' : ''}`}>
                {/* Header */}
                <div className="flex flex-wrap justify-between items-start gap-3 mb-3">
                  <div>
                    <div className="flex items-center gap-2 mb-0.5">
                      <span className="font-black text-secondary">
                        Order #{String(order.id).slice(-6)}
                      </span>
                      <span className={`badge ${STATUS_STYLE[order.status] || 'badge-gray'}`}>
                        {order.status?.replace(/_/g, ' ')}
                      </span>
                    </div>
                    <p className="text-slate-400 text-xs">{dateStr}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-slate-400">Your earning</p>
                    <p className={`text-xl font-black ${
                      order.status === 'DELIVERED' ? 'text-green-600' : 'text-primary'
                    }`}>
                      ₹{fee}
                    </p>
                  </div>
                </div>

                {/* Location cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
                  <div className="glass-subtle rounded-xl p-3">
                    <p className="text-xs font-bold text-slate-400 mb-0.5">🏪 PICKUP FROM</p>
                    <p className="font-semibold text-secondary text-sm">{order.restaurantName}</p>
                  </div>
                  <div className="glass-subtle rounded-xl p-3">
                    <p className="text-xs font-bold text-slate-400 mb-0.5">📍 DELIVER TO</p>
                    <p className="text-secondary text-sm line-clamp-2">{order.deliveryAddress}</p>
                  </div>
                </div>

                {/* Items preview */}
                {items.length > 0 && (
                  <p className="text-sm text-slate-500 mb-4">
                    🍽 {items.slice(0, 2).map(i =>
                      `${i.foodItemName || i.name} ×${i.quantity}`).join(', ')}
                    {items.length > 2 && ` +${items.length - 2} more`}
                  </p>
                )}

                {/* Timer + locked/unlocked button */}
                <DeliveryTimer
                  order={order}
                  onMarkDelivered={handleMarkDelivered}
                  marking={marking}
                />
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default AgentMyDeliveries
