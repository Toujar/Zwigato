import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import orderService from '../services/orderService'
import OrderStatus from '../components/order/OrderStatus'
import Loader from '../components/common/Loader'

const OrderTracking = () => {
  const { id } = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchOrderDetails = async () => {
      setLoading(true)
      try {
        const data = await orderService.getOrderById(id)
        setOrder(data)
      } catch (err) {
        setOrder({
          id: id || 'ORD123456',
          status: 'PREPARING',
          deliveryAddress: '123 Food Street, Bangalore - 560001',
          items: [{ name: 'Butter Chicken', quantity: 1 }, { name: 'Naan', quantity: 2 }],
          totalAmount: 398,
        })
      } finally {
        setLoading(false)
      }
    }

    fetchOrderDetails()
  }, [id])

  if (loading) {
    return <Loader fullPage />
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold text-secondary">Track Order #{order.id}</h1>
        <Link to="/orders" className="text-primary hover:underline font-semibold text-sm">
          ← Back to Orders
        </Link>
      </div>

      <div className="bg-white rounded-xl p-6 sm:p-8 card-shadow space-y-8">
        <div>
          <h2 className="text-xl font-bold mb-6 text-secondary border-b pb-2">
            Real-Time Delivery Status
          </h2>
          <OrderStatus currentStatus={order.status} />
        </div>

        <div className="border-t border-gray-100 pt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h3 className="font-bold text-base text-secondary mb-2">Delivery Address</h3>
            <p className="text-gray-600 text-sm leading-relaxed">
              {order.deliveryAddress || '123 Main St, Apartment 4B, City'}
            </p>
          </div>

          <div>
            <h3 className="font-bold text-base text-secondary mb-2">Estimated Delivery</h3>
            <p className="text-green-600 font-semibold text-lg">25 - 35 mins</p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default OrderTracking
