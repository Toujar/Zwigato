import { useState, useEffect } from 'react'
import orderService from '../services/orderService'
import OrderCard from '../components/order/OrderCard'
import Loader from '../components/common/Loader'

const dummyOrders = [
  {
    id: 'ORD123456',
    date: '2024-03-15',
    items: ['Butter Chicken', 'Naan'],
    total: 348,
    status: 'DELIVERED',
  },
  {
    id: 'ORD123455',
    date: '2024-03-10',
    items: ['Pizza', 'Garlic Bread'],
    total: 420,
    status: 'CANCELLED',
  },
]

const OrderHistory = () => {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchOrders = async () => {
      setLoading(true)
      try {
        const data = await orderService.getOrders()
        if (Array.isArray(data) && data.length > 0) {
          setOrders(data)
        } else {
          setOrders(dummyOrders)
        }
      } catch (err) {
        setOrders(dummyOrders)
      } finally {
        setLoading(false)
      }
    }

    fetchOrders()
  }, [])

  if (loading) {
    return <Loader fullPage />
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold mb-8 text-secondary">Your Orders</h1>

      {orders.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 rounded-xl">
          <p className="text-gray-500 text-lg">You haven't placed any orders yet.</p>
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
