import { useState, useEffect, useRef, useCallback, lazy, Suspense } from 'react'
import { useParams, Link } from 'react-router-dom'
import orderService from '../services/orderService'
import OrderStatus from '../components/order/OrderStatus'
import Loader from '../components/common/Loader'

// Map lazy-loaded so it doesn't block the page render
const DeliveryMap = lazy(() => import('../components/maps/DeliveryMap'))

// Poll the backend every 30 seconds while order is active
const POLL_MS = 30_000

const STATUS_MESSAGES = {
  PLACED:           { icon: '📝', text: 'Order received — waiting for restaurant confirmation' },
  CONFIRMED:        { icon: '✅', text: 'Restaurant confirmed your order!' },
  PREPARING:        { icon: '👨‍🍳', text: 'Your food is being prepared 🔥' },
  OUT_FOR_DELIVERY: { icon: '🛵', text: 'Your order is on the way!' },
}

const OrderTracking = () => {
  const { id }                    = useParams()
  const [order, setOrder]         = useState(null)
  const [loading, setLoading]     = useState(true)
  const pollRef                   = useRef(null)

  // ── Fetch order from backend ──────────────────────────────────────
  const fetchOrder = useCallback(async (initial = false) => {
    if (initial) setLoading(true)
    try {
      const data = await orderService.getOrderById(id)
      setOrder(data)
      // If terminal, stop polling
      if (data?.status === 'DELIVERED' || data?.status === 'CANCELLED') {
        clearInterval(pollRef.current)
      }
    } catch {
      if (initial) setOrder(null)
    } finally {
      if (initial) setLoading(false)
    }
  }, [id])

  // ── Start polling on mount, stop on unmount or terminal status ────
  useEffect(() => {
    fetchOrder(true).then(() => {
      pollRef.current = setInterval(() => fetchOrder(false), POLL_MS)
    })
    return () => clearInterval(pollRef.current)
  }, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  if (loading || !order) return <Loader fullPage />

  const items      = order.items ?? order.orderItems ?? []
  const isDelivered = order.status === 'DELIVERED'
  const isCancelled = order.status === 'CANCELLED'
  const isTerminal  = isDelivered || isCancelled
  const activeMsg   = STATUS_MESSAGES[order.status]

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* ── Header ── */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-black text-secondary">Order Tracking</h1>
          <p className="text-slate-400 text-sm mt-1">Order #{order.id}</p>
        </div>
        <Link to="/orders" className="btn-glass text-sm px-4 py-2">
          ← Back to Orders
        </Link>
      </div>

      {/* ── Status stepper ── */}
      <div className="glass p-6 sm:p-8 mb-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold text-secondary">Delivery Status</h2>
          {!isTerminal && (
            <span className="text-xs text-slate-400 flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse inline-block" />
              Live · refreshes every 30s
            </span>
          )}
        </div>

        <OrderStatus currentStatus={order.status} />

        {/* Active status message */}
        {activeMsg && !isTerminal && (
          <div className="mt-5 glass-subtle rounded-xl px-4 py-3 flex items-center gap-3">
            <span className="text-xl animate-pulse">{activeMsg.icon}</span>
            <p className="text-sm font-semibold text-secondary">{activeMsg.text}</p>
          </div>
        )}

        {/* Delivered banner */}
        {isDelivered && (
          <div className="mt-5 rounded-xl px-4 py-4 flex items-center gap-3"
            style={{ background: 'rgba(34,197,94,0.10)', border: '1px solid rgba(34,197,94,0.25)' }}>
            <span className="text-2xl">🎉</span>
            <div>
              <p className="text-sm font-black text-green-600">Order Delivered!</p>
              <p className="text-xs text-slate-400 mt-0.5">Enjoy your meal. Thank you for ordering!</p>
            </div>
          </div>
        )}

        {/* Cancelled banner */}
        {isCancelled && (
          <div className="mt-5 rounded-xl px-4 py-4 flex items-center gap-3"
            style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.20)' }}>
            <span className="text-2xl">❌</span>
            <div>
              <p className="text-sm font-black text-red-500">Order Cancelled</p>
              <p className="text-xs text-slate-400 mt-0.5">This order has been cancelled.</p>
            </div>
          </div>
        )}
      </div>

      {/* ── Delivery Route Map ── */}
      {order.restaurantAddress && order.deliveryAddress && (
        <div className="glass p-5 mb-5">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-bold text-secondary flex items-center gap-2">
              🗺️ {isDelivered ? 'Delivery Completed' : 'Live Delivery Route'}
            </h3>
            {isDelivered && (
              <span className="badge badge-green text-xs">✓ Delivered</span>
            )}
          </div>
          <Suspense fallback={
            <div className="glass-subtle rounded-2xl h-64 flex items-center justify-center">
              <Loader size="sm" />
            </div>
          }>
            <DeliveryMap
              restaurantAddress={order.restaurantAddress}
              deliveryAddress={order.deliveryAddress}
              restaurantName={order.restaurantName}
              orderStatus={order.status}
              height="320px"
            />
          </Suspense>
        </div>
      )}

      {/* ── Details grid ── */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5 mb-5">
        <div className="glass-white p-5">
          <h3 className="font-bold text-secondary mb-3 flex items-center gap-2">
            <svg className="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
            Delivery Address
          </h3>
          <p className="text-slate-500 text-sm leading-relaxed">{order.deliveryAddress || 'Not specified'}</p>
        </div>

        <div className="glass-white p-5">
          <h3 className="font-bold text-secondary mb-3 flex items-center gap-2">
            <svg className="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            {isDelivered ? 'Delivery Status' : 'Estimated Delivery'}
          </h3>
          {isDelivered ? (
            <p className="text-green-600 font-bold text-lg">Delivered ✓</p>
          ) : isCancelled ? (
            <p className="text-red-500 font-bold text-lg">Cancelled</p>
          ) : (
            <p className="text-primary font-semibold text-lg">In progress…</p>
          )}
          <p className="text-slate-400 text-xs mt-1">Restaurant: {order.restaurantName || '—'}</p>
        </div>
      </div>

      {/* ── Order items ── */}
      {items.length > 0 && (
        <div className="glass p-6">
          <h3 className="font-bold text-secondary mb-4">Items Ordered</h3>
          <div className="divide-y" style={{ borderColor: 'rgba(186,230,253,0.40)' }}>
            {items.map((item, i) => (
              <div key={item.id ?? i} className="flex justify-between items-center py-3">
                <div className="flex items-center gap-3">
                  {item.imageUrl && (
                    <img src={item.imageUrl} alt={item.foodItemName || item.name}
                      className="w-10 h-10 rounded-xl object-cover" />
                  )}
                  <div>
                    <p className="font-semibold text-secondary text-sm">{item.foodItemName || item.name}</p>
                    <p className="text-slate-400 text-xs">× {item.quantity}</p>
                  </div>
                </div>
                <p className="font-bold text-primary text-sm">
                  ₹{(item.subtotal ?? item.unitPrice * item.quantity).toFixed(2)}
                </p>
              </div>
            ))}
          </div>
          <div className="mt-4 pt-4 space-y-1.5 text-sm"
            style={{ borderTop: '1px solid rgba(186,230,253,0.40)' }}>
            <div className="flex justify-between text-slate-500">
              <span>Subtotal</span><span>₹{order.subtotal?.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-slate-500">
              <span>Delivery Fee</span><span>₹{order.deliveryFee?.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-slate-500">
              <span>Tax (5%)</span><span>₹{order.tax?.toFixed(2)}</span>
            </div>
            <div className="flex justify-between font-black text-secondary text-base pt-2"
              style={{ borderTop: '1px solid rgba(186,230,253,0.40)' }}>
              <span>Total</span><span className="text-primary">₹{order.totalAmount?.toFixed(2)}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default OrderTracking
