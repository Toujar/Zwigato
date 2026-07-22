import React from 'react'
import { Link } from 'react-router-dom'

const CartSummary = ({ totalItems, totalPrice, deliveryFee = 40, serviceFee = 10, isCheckout = false }) => {
  const finalTotal = totalPrice + (totalItems > 0 ? deliveryFee + serviceFee : 0)

  return (
    <div className="bg-white rounded-xl p-6 food-card-shadow sticky top-24">
      <h3 className="text-xl font-bold mb-4 text-secondary">Order Summary</h3>

      <div className="space-y-3 text-gray-600">
        <div className="flex justify-between">
          <span>Subtotal ({totalItems} items)</span>
          <span>₹{totalPrice.toFixed(2)}</span>
        </div>
        <div className="flex justify-between">
          <span>Delivery Fee</span>
          <span>₹{totalItems > 0 ? deliveryFee : 0}</span>
        </div>
        <div className="flex justify-between">
          <span>Service Fee</span>
          <span>₹{totalItems > 0 ? serviceFee : 0}</span>
        </div>
        <hr className="border-gray-200" />
        <div className="flex justify-between font-bold text-lg text-secondary">
          <span>Total</span>
          <span>₹{finalTotal.toFixed(2)}</span>
        </div>
      </div>

      {!isCheckout && (
        <Link
          to="/checkout"
          className={`btn-primary w-full mt-6 text-center block ${
            totalItems === 0 ? 'pointer-events-none opacity-50' : ''
          }`}
        >
          Proceed to Checkout
        </Link>
      )}
    </div>
  )
}

export default CartSummary
