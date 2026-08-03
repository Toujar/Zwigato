import { useState, useEffect, useCallback } from 'react'
import { useToast } from '../../context/ToastContext'
import deliveryService from '../../services/deliveryService'
import Loader from '../../components/common/Loader'

const AgentAvailable = () => {
  const toast               = useToast()
  const [orders, setOrders] = useState([])
  const [loading, setLoading]   = useState(true)
  const [accepting, setAccepting] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await deliveryService.getAvailableOrders()
      setOrders(Array.isArray(data) ? data : [])
    } catch (err) { toast.error(err.message || 'Failed to load orders') }
    finally { setLoading(false) }
  }, []) // eslint-disable-line

  useEffect(() => { load() }, [])

  const handleAccept = async (orderId) => {
    setAccepting(orderId)
    try {
      await deliveryService.acceptDelivery(orderId)
      toast.success(`Order #${String(orderId).slice(-6)} accepted! Head for pickup 🛵`)
      setOrders(l => l.filter(o => o.id !== orderId))
    } catch (err) { toast.error(err.message || 'Failed to accept order') }
    finally { setAccepting(null) }
  }

  if (loading) return <Loader fullPage />

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-black text-secondary">Available Deliveries</h1>
          <p className="text-slate-500 text-sm mt-1">
            {orders.length} order{orders.length !== 1 ? 's' : ''} waiting for a delivery agent
          </p>
        </div>
        <button onClick={load} className="btn-glass text-sm px-4 py-2 flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Refresh
        </button>
      </div>

      {orders.length === 0 ? (
        <div className="glass text-center py-20 px-8">
          <div className="text-5xl mb-4">📭</div>
          <h3 className="text-xl font-bold text-secondary mb-2">No orders available right now</h3>
          <p className="text-slate-500 mb-6">New orders will appear here when restaurants mark them ready for pickup.</p>
          <button onClick={load} className="btn-primary">Check Again</button>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map(order => {
            const items   = order.items ?? order.orderItems ?? []
            const dateStr = order.placedAt
              ? new Date(order.placedAt).toLocaleString('en-IN', { day:'numeric', month:'short', hour:'2-digit', minute:'2-digit' })
              : '—'
            const fee = Number(order.deliveryFee ?? 40)

            return (
              <div key={order.id} className="glass p-5">
                {/* Header */}
                <div className="flex flex-wrap justify-between items-start gap-3 mb-4">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-black text-secondary text-lg">
                        Order #{String(order.id).slice(-6)}
                      </span>
                      <span className="badge badge-orange">{order.status?.replace(/_/g,' ')}</span>
                    </div>
                    <p className="text-slate-400 text-xs">{dateStr}</p>
                  </div>
                  {/* Earning highlight */}
                  <div className="text-right">
                    <p className="text-xs text-slate-400 mb-0.5">Your earning</p>
                    <p className="text-2xl font-black text-green-600">₹{fee}</p>
                  </div>
                </div>

                {/* Restaurant → Customer */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-4">
                  <div className="glass-subtle rounded-xl p-3">
                    <p className="text-xs font-bold text-slate-500 mb-1">🏪 PICKUP FROM</p>
                    <p className="font-semibold text-secondary text-sm">{order.restaurantName}</p>
                    <p className="text-slate-400 text-xs mt-0.5">{order.restaurantAddress}</p>
                  </div>
                  <div className="glass-subtle rounded-xl p-3">
                    <p className="text-xs font-bold text-slate-500 mb-1">📍 DELIVER TO</p>
                    <p className="font-semibold text-secondary text-sm line-clamp-2">
                      {order.deliveryAddress}
                    </p>
                  </div>
                </div>

                {/* Items summary */}
                {items.length > 0 && (
                  <div className="mb-4 text-sm text-slate-500">
                    🍽 {items.slice(0,3).map(i => `${i.foodItemName || i.name} ×${i.quantity}`).join(', ')}
                    {items.length > 3 && ` +${items.length - 3} more`}
                  </div>
                )}

                {/* Order value + accept button */}
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-xs text-slate-400">Order value </span>
                    <span className="font-bold text-secondary">₹{Number(order.totalAmount).toFixed(0)}</span>
                  </div>
                  <button
                    onClick={() => handleAccept(order.id)}
                    disabled={accepting === order.id}
                    className="btn-primary flex items-center gap-2 px-6 py-2.5"
                    style={{ background: 'linear-gradient(135deg,#22C55E,#4ADE80)',
                      boxShadow: '0 4px 16px rgba(34,197,94,0.40)' }}
                  >
                    {accepting === order.id
                      ? <Loader size="sm" />
                      : <><span>Accept & Deliver</span><span>🛵</span></>
                    }
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default AgentAvailable
