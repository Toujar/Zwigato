import React from 'react'
import { Link, useParams } from 'react-router-dom'

const OrderSuccess = () => {
  const { id } = useParams()

  return (
    <div className="min-h-[75vh] flex flex-col items-center justify-center px-4 py-12 text-center">
      <div className="w-24 h-24 bg-green-100 text-green-500 rounded-full flex items-center justify-center mb-6 animate-bounce">
        <svg
          className="w-12 h-12"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={3}
            d="M5 13l4 4L19 7"
          />
        </svg>
      </div>

      <h1 className="text-4xl font-bold text-secondary mb-3">
        Order Placed Successfully!
      </h1>
      <p className="text-gray-600 text-lg max-w-md mb-2">
        Thank you for your order. We're working on getting it prepared and delivered!
      </p>
      {id && (
        <p className="text-sm text-gray-500 font-semibold mb-8">
          Order ID: <span className="text-primary">#{id}</span>
        </p>
      )}

      <div className="flex flex-col sm:flex-row gap-4">
        {id && (
          <Link to={`/orders/${id}/tracking`} className="btn-primary">
            Track Order Status
          </Link>
        )}
        <Link
          to="/"
          className="px-6 py-3 border border-gray-300 text-gray-700 rounded-lg font-semibold hover:bg-gray-50 transition-colors"
        >
          Back to Home
        </Link>
      </div>
    </div>
  )
}

export default OrderSuccess
