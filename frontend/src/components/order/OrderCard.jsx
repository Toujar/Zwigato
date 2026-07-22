import React from 'react'
import { Link } from 'react-router-dom'

const OrderCard = ({ order }) => {
  const { id, date, placedAt, items = [], total, totalAmount, status } = order

  const orderId = id || 'ORD-UNKNOWN'
  const orderDate = date || (placedAt ? new Date(placedAt).toLocaleDateString() : 'Recent')
  const orderTotal = total || totalAmount || 0

  const getStatusBadge = (statusStr) => {
    switch (statusStr?.toUpperCase()) {
      case 'DELIVERED':
        return 'bg-green-100 text-green-700 border-green-200'
      case 'CANCELLED':
        return 'bg-red-100 text-red-700 border-red-200'
      case 'OUT_FOR_DELIVERY':
      case 'PREPARING':
      case 'CONFIRMED':
      case 'PLACED':
        return 'bg-orange-100 text-orange-700 border-orange-200'
      default:
        return 'bg-gray-100 text-gray-700 border-gray-200'
    }
  }

  const itemListText = Array.isArray(items)
    ? items.map((i) => (typeof i === 'string' ? i : i.name || i.foodItemName)).join(', ')
    : 'Food items'

  return (
    <div className="bg-white rounded-xl p-6 food-card-shadow border border-gray-50 hover:shadow-md transition-shadow">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-4 gap-2">
        <div>
          <h3 className="font-bold text-lg text-secondary">Order #{orderId}</h3>
          <p className="text-gray-400 text-sm">{orderDate}</p>
        </div>
        <span
          className={`px-3 py-1 rounded-full text-xs font-semibold border ${getStatusBadge(
            status
          )}`}
        >
          {status}
        </span>
      </div>

      <p className="text-gray-600 text-sm mb-4 line-clamp-2">{itemListText}</p>

      <div className="flex justify-between items-center pt-3 border-t border-gray-100">
        <p className="font-bold text-lg text-primary">₹{orderTotal.toFixed(2)}</p>

        <Link
          to={`/orders/${orderId}/tracking`}
          className="text-sm font-semibold text-primary hover:underline flex items-center"
        >
          Track Order
          <svg
            className="w-4 h-4 ml-1"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 5l7 7-7 7"
            />
          </svg>
        </Link>
      </div>
    </div>
  )
}

export default OrderCard
