import React from 'react'
import { Link, useParams } from 'react-router-dom'

const OrderSuccess = () => {
  const { id } = useParams()

  return (
    <div className="min-h-[75vh] flex flex-col items-center justify-center px-4 py-12 text-center">
      {/* Animated success icon */}
      <div className="w-28 h-28 rounded-full flex items-center justify-center mb-6 animate-float"
        style={{
          background: 'linear-gradient(135deg, rgba(34,197,94,0.18) 0%, rgba(74,222,128,0.25) 100%)',
          border: '2px solid rgba(34,197,94,0.35)',
          boxShadow: '0 8px 32px rgba(34,197,94,0.22)',
        }}>
        <svg className="w-14 h-14 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
        </svg>
      </div>

      <h1 className="text-4xl font-black text-secondary mb-3">Order Placed! 🎉</h1>
      <p className="text-slate-500 text-lg max-w-md mb-2 leading-relaxed">
        We&apos;re on it — your food is being prepared and will be on its way soon.
      </p>
      {id && (
        <p className="text-sm text-slate-400 font-semibold mb-8">
          Order ID: <span className="text-primary font-bold">#{id}</span>
        </p>
      )}

      <div className="flex flex-col sm:flex-row gap-3">
        {id && (
          <Link to={`/orders/${id}/tracking`} className="btn-primary">
            Track My Order
          </Link>
        )}
        <Link to="/" className="btn-glass">
          Back to Home
        </Link>
      </div>
    </div>
  )
}

export default OrderSuccess
