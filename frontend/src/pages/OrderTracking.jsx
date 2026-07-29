import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, Link } from 'react-router-dom'
import orderService from '../services/orderService'
import OrderStatus from '../components/order/OrderStatus'
import Loader from '../components/common/Loader'

// The pipeline every active order flows through
const STATUS_PIPELINE = [
  'PLACED',
  'CONFIRMED',
  'PREPARING',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
]

// Advance one step every 60 seconds
const ADVANCE_INTERVAL_MS = 60_000

const OrderTracking = () => {
  const { id }               = useParams()
  const [order, setOrder]    = useState(null)
  const [loading, setLoading] = useState(true)
  const [timeLeft, setTimeLeft] = useState(ADVANCE_INTERVAL_MS / 1000) // countdown seconds

  // Keep a ref so the interval can always read the latest status
  const orderRef   = useRef(null)
  const timerRef   = useRef(null)
  const countRef   = useRef(null)

  // ── Load order from backend ─────────────────────────────────
  const loadOrder = useCallback(async (showFullLoader = false) => {
    if (showFullLoader) setLoading(true)
    try {
      const data = await orderService.getOrderById(id)
      setOrder(data)
      orderRef.current = data
    } catch {
      if (showFullLoader) {
        const fallback = {
          id, status: 'PLACED', deliveryAddress: 'Not available',
          orderItems: [], totalAmount: 0,
        }
        setOrder(fallback)
        orderRef.current = fallback
      }
    } finally {
      if (showFullLoader) setLoading(false)
    }
  }, [id])

  // ── Advance status — now handled server-side by the scheduler.
  // We just poll the backend every 60s to get the real updated status.
  const advanceStatus = useCallback(async () => {
    const current = orderRef.current
    if (!current) return

    const currentStatus = current.status
    if (currentStatus === 'DELIVERED' || currentStatus === 'CANCELLED') {
      clearInterval(timerRef.current)
      clearInterval(countRef.current)
      return
    }

    // Poll backend for the latest status (scheduler has already advanced it)
    try {
      const serverData = await orderService.getOrderById(id)
      if (serverData?.status) {
        setOrder(serverData)
        orderRef.current = serverData
      }
    } catch { /* keep current state */ }

    setTimeLeft(ADVANCE_INTERVAL_MS / 1000) // reset countdown

    // Stop if terminal after poll
    if (orderRef.current?.status === 'DELIVERED' || orderRef.current?.status === 'CANCELLED') {
      clearInterval(timerRef.current)
      clearInterval(countRef.current)
    }
  }, [id])

  // ── Bootstrap ────────────────────────────────────────────────
  useEffect(() => {
    loadOrder(true).then(() => {
      // Don't start timers if already terminal
      const status = orderRef.current?.status
      if (status === 'DELIVERED' || status === 'CANCELLED') return

      // Countdown display (every second)
      countRef.current = setInterval(() => {
        setTimeLeft(t => {
          if (t <= 1) return ADVANCE_INTERVAL_MS / 1000
          return t - 1
        })
      }, 1000)

      // Status advance (every 60 s)
      timerRef.current = setInterval(() => {
        advanceStatus()
      }, ADVANCE_INTERVAL_MS)
    })

    return () => {
      clearInterval(timerRef.current)
      clearInterval(countRef.current)
    }
  }, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  if (loading || !order) return <Loader fullPage />

  const items        = order.items ?? order.orderItems ?? []
  const isTerminal   = order.status === 'DELIVERED' || order.status === 'CANCELLED'
  const currentIdx   = STATUS_PIPELINE.indexOf(order.status)
  const stepsLeft    = STATUS_PIPELINE.length - 1 - (currentIdx >= 0 ? currentIdx : 0)

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-black text-secondary">Order Tracking</h1>
          <p className="text-slate-400 text-sm mt-1">Order #{order.id}</p>
        </div>
        <Link to="/orders" className="btn-glass text-sm px-4 py-2">
          ← Back to Orders
        </Link>
      </div>

      {/* Status stepper */}
      <div className="glass p-6 sm:p-8 mb-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold text-secondary">Delivery Status</h2>
          {!isTerminal && (
            <div className="flex items-center gap-2">
              {/* Countdown ring */}
              <div className="relative w-10 h-10">
                <svg className="w-10 h-10 -rotate-90" viewBox="0 0 36 36">
                  <circle cx="18" cy="18" r="15.9" fill="none"
                    stroke="rgba(186,230,253,0.40)" strokeWidth="2.5" />
                  <circle cx="18" cy="18" r="15.9" fill="none"
                    stroke="#0EA5E9" strokeWidth="2.5"
                    strokeDasharray={`${(timeLeft / (ADVANCE_INTERVAL_MS / 1000)) * 100} 100`}
                    strokeLinecap="round"
                    style={{ transition: 'stroke-dasharray 1s linear' }} />
                </svg>
                <span className="absolute inset-0 flex items-center justify-center text-xs font-bold text-primary">
                  {timeLeft}s
                </span>
              </div>
              <span className="text-xs text-slate-400 hidden sm:block">next update</span>
            </div>
          )}
        </div>
        <OrderStatus currentStatus={order.status} />

        {/* Progress message */}
        {!isTerminal && (
          <div className="mt-5 glass-subtle rounded-xl px-4 py-3 flex items-center gap-3">
            <span className="text-xl animate-pulse">
              {order.status === 'PLACED'           ? '📝' :
               order.status === 'CONFIRMED'        ? '✅' :
               order.status === 'PREPARING'        ? '👨‍🍳' :
               order.status === 'OUT_FOR_DELIVERY' ? '🛵' : '⏳'}
            </span>
            <div>
              <p className="text-sm font-semibold text-secondary">
                {order.status === 'PLACED'           ? 'Order received — waiting for restaurant confirmation' :
                 order.status === 'CONFIRMED'        ? 'Restaurant confirmed your order!' :
                 order.status === 'PREPARING'        ? 'Your food is being prepared 🔥' :
                 order.status === 'OUT_FOR_DELIVERY' ? 'Your order is on the way!' : ''}
              </p>
              <p className="text-xs text-slate-400 mt-0.5">
                {stepsLeft} step{stepsLeft !== 1 ? 's' : ''} to delivery &middot; advancing in {timeLeft}s
              </p>
            </div>
          </div>
        )}

        {order.status === 'DELIVERED' && (
          <div className="mt-5 glass-subtle rounded-xl px-4 py-3 flex items-center gap-3">
            <span className="text-2xl">🎉</span>
            <div>
              <p className="text-sm font-bold text-green-600">Order Delivered!</p>
              <p className="text-xs text-slate-400 mt-0.5">Enjoy your meal. Thank you for ordering!</p>
            </div>
          </div>
        )}
      </div>

      {/* Details grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5 mb-5">
        {/* Address */}
        <div className="glass-white p-5">
          <h3 className="font-bold text-secondary mb-3 flex items-center gap-2">
            <svg className="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Delivery Address
          </h3>
          <p className="text-slate-500 text-sm leading-relaxed">{order.deliveryAddress || 'Not specified'}</p>
        </div>

        {/* ETA */}
        <div className="glass-white p-5">
          <h3 className="font-bold text-secondary mb-3 flex items-center gap-2">
            <svg className="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Estimated Delivery
          </h3>
          {order.status === 'DELIVERED' ? (
            <p className="text-green-600 font-bold text-lg">Delivered ✓</p>
          ) : order.status === 'CANCELLED' ? (
            <p className="text-red-500 font-bold text-lg">Cancelled</p>
          ) : (
            <p className="text-primary font-semibold text-lg">
              ~{stepsLeft} min{stepsLeft !== 1 ? 's' : ''} remaining
            </p>
          )}
          <p className="text-slate-400 text-xs mt-1">Restaurant: {order.restaurantName || '—'}</p>
        </div>
      </div>

      {/* Order items */}
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

          <div className="mt-4 pt-4 space-y-1.5 text-sm" style={{ borderTop: '1px solid rgba(186,230,253,0.40)' }}>
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
