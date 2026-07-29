import { useState, useEffect } from 'react'
import { useToast } from '../../context/ToastContext'
import orderService from '../../services/orderService'
import Loader from '../../components/common/Loader'

const STATUS_STYLE = {
  PLACED:           'badge-gray',
  CONFIRMED:        'badge-blue',
  PREPARING:        'badge-orange',
  OUT_FOR_DELIVERY: 'badge-blue',
  DELIVERED:        'badge-green',
  CANCELLED:        'badge-red',
}

// What an owner can advance status to
const NEXT_STATUS = {
  PLACED:    'CONFIRMED',
  CONFIRMED: 'PREPARING',
  PREPARING: 'OUT_FOR_DELIVERY',
}

const OwnerOrders = () => {
  const toast                     = useToast()
  const [orders, setOrders]       = useState([])
  const [loading, setLoading]     = useState(true)
  const [statusFilter, setFilter] = useState('ALL')
  const [updating, setUpdating]   = useState(null)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const res  = await orderService.getOrders({ page:0, size:50 })
        const list = res?.content ?? (Array.isArray(res) ? res : [])
        setOrders(list)
      } catch (err) { toast.error(err.message || 'Failed to load orders') }
      finally { setLoading(false) }
    }
    load()
  }, [])

  const advance = async (orderId, newStatus) => {
    setUpdating(orderId)
    try {
      const updated = await orderService.updateStatus(orderId, newStatus)
      setOrders(l => l.map(o => o.id === orderId ? { ...o, status: updated.status || newStatus } : o))
      toast.success(`Order #${orderId} → ${newStatus.replace(/_/g,' ')}`)
    } catch (err) { toast.error(err.message) }
    finally { setUpdating(null) }
  }

  const filtered = statusFilter === 'ALL' ? orders : orders.filter(o => o.status === statusFilter)

  if (loading) return <Loader fullPage />

  const FILTERS = ['ALL','PLACED','CONFIRMED','PREPARING','OUT_FOR_DELIVERY','DELIVERED','CANCELLED']

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-black text-secondary">Incoming Orders</h1>
        <p className="text-slate-500 text-sm mt-1">{orders.length} total orders</p>
      </div>

      {/* Status filter */}
      <div className="flex gap-2 flex-wrap mb-6">
        {FILTERS.map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${
              statusFilter === s ? 'btn-primary' : 'btn-glass'
            }`}>
            {s.replace(/_/g,' ')}
            {s !== 'ALL' && (
              <span className="ml-1 opacity-70">
                ({orders.filter(o => o.status === s).length})
              </span>
            )}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="glass text-center py-16 px-8">
          <div className="text-5xl mb-4">📋</div>
          <h3 className="text-xl font-bold text-secondary mb-2">No orders here</h3>
          <p className="text-slate-500">Orders from customers will appear here</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map(order => {
            const items    = order.items ?? order.orderItems ?? []
            const nextSt   = NEXT_STATUS[order.status]
            const dateStr  = order.placedAt
              ? new Date(order.placedAt).toLocaleString('en-IN', { day:'numeric', month:'short', hour:'2-digit', minute:'2-digit' })
              : '—'

            return (
              <div key={order.id} className="glass p-5">
                <div className="flex flex-wrap gap-3 justify-between items-start mb-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-black text-secondary">Order #{String(order.id).slice(-6)}</span>
                      <span className={`badge ${STATUS_STYLE[order.status] || 'badge-gray'}`}>
                        {order.status?.replace(/_/g,' ')}
                      </span>
                    </div>
                    <p className="text-slate-400 text-xs mt-0.5">{dateStr}</p>
                    {order.restaurantName && (
                      <p className="text-primary text-xs font-semibold mt-0.5">{order.restaurantName}</p>
                    )}
                  </div>
                  <span className="font-black text-xl text-primary">₹{Number(order.totalAmount).toFixed(2)}</span>
                </div>

                {/* Delivery address */}
                <p className="text-slate-500 text-sm mb-3">
                  📍 {order.deliveryAddress || '—'}
                </p>

                {/* Items */}
                {items.length > 0 && (
                  <div className="glass-subtle rounded-xl p-3 mb-4 space-y-1">
                    {items.map((it, idx) => (
                      <div key={it.id ?? idx} className="flex justify-between text-sm">
                        <span className="text-slate-600">{it.foodItemName || it.name} × {it.quantity}</span>
                        <span className="text-slate-700 font-semibold">
                          ₹{(it.subtotal ?? it.unitPrice * it.quantity).toFixed(2)}
                        </span>
                      </div>
                    ))}
                  </div>
                )}

                {/* Actions */}
                {nextSt && (
                  <button
                    onClick={() => advance(order.id, nextSt)}
                    disabled={updating === order.id}
                    className="btn-primary text-sm px-5 py-2 flex items-center gap-2"
                  >
                    {updating === order.id ? <Loader size="sm" /> : null}
                    Mark as {nextSt.replace(/_/g,' ')} →
                  </button>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default OwnerOrders
