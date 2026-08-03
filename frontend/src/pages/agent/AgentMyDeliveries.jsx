import { useState, useEffect } from 'react'
import { useToast } from '../../context/ToastContext'
import deliveryService from '../../services/deliveryService'
import Loader from '../../components/common/Loader'

const STATUS_STYLE = {
  OUT_FOR_DELIVERY: 'badge-blue',
  DELIVERED:        'badge-green',
  CANCELLED:        'badge-red',
  PLACED:           'badge-gray',
  CONFIRMED:        'badge-blue',
  PREPARING:        'badge-orange',
}

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
        const res  = await deliveryService.getMyDeliveries({ page:0, size:50 })
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

  const filtered = filter === 'ALL' ? orders
    : orders.filter(o => o.status === filter)

  if (loading) return <Loader fullPage />

  const FILTERS = ['ALL', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED']

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
            const dateStr = order.placedAt
              ? new Date(order.placedAt).toLocaleString('en-IN',
                  { day:'numeric', month:'short', hour:'2-digit', minute:'2-digit' })
              : '—'
            const fee = Number(order.deliveryFee ?? 40)
            const isActive = order.status === 'OUT_FOR_DELIVERY'

            return (
              <div key={order.id}
                className={`glass p-5 transition-all ${isActive ? 'ring-1 ring-green-400/40' : ''}`}>
                <div className="flex flex-wrap justify-between items-start gap-3 mb-3">
                  <div>
                    <div className="flex items-center gap-2 mb-0.5">
                      <span className="font-black text-secondary">Order #{String(order.id).slice(-6)}</span>
                      <span className={`badge ${STATUS_STYLE[order.status] || 'badge-gray'}`}>
                        {order.status?.replace(/_/g,' ')}
                      </span>
                    </div>
                    <p className="text-slate-400 text-xs">{dateStr}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-slate-400">Your earning</p>
                    <p className={`text-xl font-black ${order.status === 'DELIVERED' ? 'text-green-600' : 'text-primary'}`}>
                      ₹{fee}
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
                  <div className="glass-subtle rounded-xl p-3">
                    <p className="text-xs font-bold text-slate-400 mb-0.5">🏪 FROM</p>
                    <p className="font-semibold text-secondary text-sm">{order.restaurantName}</p>
                  </div>
                  <div className="glass-subtle rounded-xl p-3">
                    <p className="text-xs font-bold text-slate-400 mb-0.5">📍 DELIVER TO</p>
                    <p className="text-secondary text-sm line-clamp-2">{order.deliveryAddress}</p>
                  </div>
                </div>

                {items.length > 0 && (
                  <p className="text-sm text-slate-500 mb-3">
                    🍽 {items.slice(0,2).map(i => `${i.foodItemName || i.name} ×${i.quantity}`).join(', ')}
                    {items.length > 2 && ` +${items.length - 2} more`}
                  </p>
                )}

                {/* Mark delivered button — only on active deliveries */}
                {isActive && (
                  <button
                    onClick={() => handleMarkDelivered(order.id)}
                    disabled={marking === order.id}
                    className="btn-primary flex items-center gap-2 px-5 py-2.5 text-sm"
                    style={{ background: 'linear-gradient(135deg,#22C55E,#4ADE80)',
                      boxShadow: '0 4px 14px rgba(34,197,94,0.40)' }}
                  >
                    {marking === order.id
                      ? <Loader size="sm" />
                      : <><span>Mark as Delivered</span><span>✓</span></>
                    }
                  </button>
                )}

                {order.status === 'DELIVERED' && (
                  <div className="flex items-center gap-2 text-green-600 text-sm font-semibold">
                    <span>✅</span> Delivered · ₹{fee} earned
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default AgentMyDeliveries
