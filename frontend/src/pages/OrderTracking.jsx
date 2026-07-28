import { useState, useEffect, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'
import orderService from '../services/orderService'
import OrderStatus from '../components/order/OrderStatus'
import Loader from '../components/common/Loader'

const POLL_INTERVAL_MS = 30_000 // poll every 30 s for status updates

const OrderTracking = () => {
  const { id }            = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const timerRef          = useRef(null)

  const loadOrder = async (showFullLoader = false) => {
    if (showFullLoader) setLoading(true)
    try {
      const data = await orderService.getOrderById(id)
      setOrder(data)
    } catch {
      // keep stale data if poll fails
      if (showFullLoader) {
        setOrder({
          id,
          status: 'PLACED',
          deliveryAddress: 'Not available',
          orderItems: [],
          totalAmount: 0,
        })
      }
    } finally {
      if (showFullLoader) setLoading(false)
    }
  }

  useEffect(() => {
    loadOrder(true)

    // Auto-refresh status every 30 s until DELIVERED or CANCELLED
    timerRef.current = setInterval(() => {
      if (order?.status === 'DELIVERED' || order?.status === 'CANCELLED') {
        clearInterval(timerRef.current)
        return
      }
      loadOrder(false)
    }, POLL_INTERVAL_MS)

    return () => clearInterval(timerRef.current)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  if (loading || !order) return <Loader fullPage />

  const items = order.items ?? order.orderItems ?? []

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-secondary">Order Tracking</h1>
          <p className="text-gray-500 text-sm mt-1">Order # {order.id}</p>
        </div>
        <Link to="/orders" className="text-primary hover:underline font-semibold text-sm">
          ← Back to Orders
        </Link>
      </div>

      {/* Status stepper */}
      <div className="bg-white rounded-xl p-6 sm:p-8 card-shadow mb-6">
        <h2 className="text-xl font-bold mb-6 text-secondary border-b pb-3">
          Delivery Status
          <span className="ml-3 text-xs text-gray-400 font-normal">
            (refreshes every 30 s)
          </span>
        </h2>
        <OrderStatus currentStatus={order.status} />
      </div>

      {/* Details grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        {/* Delivery address */}
        <div className="bg-white rounded-xl p-6 card-shadow">
          <h3 className="font-bold text-secondary mb-3 flex items-center gap-2">
            <svg className="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Delivery Address
          </h3>
          <p className="text-gray-600 text-sm leading-relaxed">
            {order.deliveryAddress || 'Not specified'}
          </p>
        </div>

        {/* ETA */}
        <div className="bg-white rounded-xl p-6 card-shadow">
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
            <p className="text-green-600 font-semibold text-lg">25 – 40 mins</p>
          )}
          <p className="text-gray-400 text-xs mt-1">Restaurant: {order.restaurantName || '—'}</p>
        </div>
      </div>

      {/* Order items */}
      {items.length > 0 && (
        <div className="bg-white rounded-xl p-6 card-shadow">
          <h3 className="font-bold text-secondary mb-4">Items Ordered</h3>
          <div className="divide-y divide-gray-100">
            {items.map((item, i) => (
              <div key={item.id ?? i} className="flex justify-between items-center py-3">
                <div className="flex items-center gap-3">
                  {item.imageUrl && (
                    <img src={item.imageUrl} alt={item.foodItemName || item.name}
                      className="w-10 h-10 rounded-lg object-cover" />
                  )}
                  <div>
                    <p className="font-medium text-secondary text-sm">
                      {item.foodItemName || item.name}
                    </p>
                    <p className="text-gray-400 text-xs">× {item.quantity}</p>
                  </div>
                </div>
                <p className="font-semibold text-secondary text-sm">
                  ₹{(item.subtotal ?? item.unitPrice * item.quantity).toFixed(2)}
                </p>
              </div>
            ))}
          </div>

          <div className="border-t border-gray-100 mt-4 pt-4 space-y-1 text-sm">
            <div className="flex justify-between text-gray-600">
              <span>Subtotal</span>
              <span>₹{order.subtotal?.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-gray-600">
              <span>Delivery Fee</span>
              <span>₹{order.deliveryFee?.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-gray-600">
              <span>Tax (5%)</span>
              <span>₹{order.tax?.toFixed(2)}</span>
            </div>
            <div className="flex justify-between font-bold text-secondary text-base pt-2 border-t border-gray-100">
              <span>Total</span>
              <span>₹{order.totalAmount?.toFixed(2)}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default OrderTracking
