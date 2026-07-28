import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import orderService from '../services/orderService'
import OrderCard from '../components/order/OrderCard'
import Loader from '../components/common/Loader'

const OrderHistory = () => {
  const [orders, setOrders]   = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState(null)

  useEffect(() => {
    const fetchOrders = async () => {
      setLoading(true)
      setError(null)
      try {
        const result = await orderService.getOrders({ page: 0, size: 20 })
        // Backend returns Page<OrderResponse> → { content: [...] }
        const list = result?.content ?? result
        setOrders(Array.isArray(list) ? list : [])
      } catch (err) {
        setError(err.message || 'Failed to load orders')
        setOrders([])
      } finally {
        setLoading(false)
      }
    }
    fetchOrders()
  }, [])

  if (loading) return <Loader fullPage />

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold mb-8 text-secondary">Your Orders</h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4 mb-6">
          {error}
        </div>
      )}

      {orders.length === 0 && !error ? (
        <div className="text-center py-16 bg-gray-50 rounded-xl">
          <div className="w-20 h-20 bg-orange-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-10 h-10 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          </div>
          <h3 className="text-xl font-bold text-gray-700 mb-2">No orders yet</h3>
          <p className="text-gray-500 mb-6">You haven't placed any orders. Start exploring restaurants!</p>
          <Link to="/" className="btn-primary">Browse Restaurants</Link>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <OrderCard key={order.id} order={order} />
          ))}
        </div>
      )}
    </div>
  )
}

export default OrderHistory
