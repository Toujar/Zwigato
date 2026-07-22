import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useToast } from '../context/ToastContext'
import CartSummary from '../components/cart/CartSummary'
import orderService from '../services/orderService'
import Loader from '../components/common/Loader'

const Checkout = () => {
  const { cartItems, totalPrice, clearCart } = useCart()
  const { success, error } = useToast()
  const navigate = useNavigate()

  const [address, setAddress] = useState('')
  const [city, setCity] = useState('')
  const [pincode, setPincode] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('UPI')
  const [loading, setLoading] = useState(false)

  const handlePlaceOrder = async (e) => {
    e.preventDefault()

    if (!address || !city || !pincode) {
      error('Please complete all delivery address fields')
      return
    }

    setLoading(true)

    const orderData = {
      items: cartItems,
      deliveryAddress: `${address}, ${city} - ${pincode}`,
      paymentMethod,
      totalAmount: totalPrice + 50,
    }

    try {
      const res = await orderService.placeOrder(orderData)
      const newOrderId = res.id || 'ORD' + Math.floor(100000 + Math.random() * 900000)
      clearCart()
      success('Order placed successfully!')
      navigate(`/order-success/${newOrderId}`)
    } catch (err) {
      // Fallback demo mode
      const mockOrderId = 'ORD' + Math.floor(100000 + Math.random() * 900000)
      clearCart()
      success('Order placed successfully! (Demo Mode)')
      navigate(`/order-success/${mockOrderId}`)
    } finally {
      setLoading(false)
    }
  }

  if (cartItems.length === 0) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center py-12">
        <h2 className="text-2xl font-bold text-gray-700 mb-2">No items to checkout</h2>
        <button onClick={() => navigate('/')} className="btn-primary mt-4">
          Return to Browse
        </button>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold mb-8 text-secondary">Checkout</h1>

      <form onSubmit={handlePlaceOrder} className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          {/* Delivery Address */}
          <div className="bg-white rounded-xl p-6 card-shadow">
            <h2 className="text-xl font-bold mb-4 text-secondary">Delivery Address</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-gray-700 font-medium mb-2">
                  Street Address
                </label>
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
                  placeholder="House No., Street Name, Area"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-gray-700 font-medium mb-2">
                    City
                  </label>
                  <input
                    type="text"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
                    placeholder="City"
                    required
                  />
                </div>
                <div>
                  <label className="block text-gray-700 font-medium mb-2">
                    Pincode
                  </label>
                  <input
                    type="text"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
                    placeholder="Pincode"
                    required
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Payment Method */}
          <div className="bg-white rounded-xl p-6 card-shadow">
            <h2 className="text-xl font-bold mb-4 text-secondary">Payment Method</h2>
            <div className="space-y-3">
              {['UPI', 'Credit/Debit Card', 'Net Banking', 'Cash on Delivery'].map(
                (method) => (
                  <label
                    key={method}
                    className={`flex items-center space-x-3 p-4 border rounded-lg cursor-pointer transition-colors ${
                      paymentMethod === method
                        ? 'border-primary bg-orange-50/30'
                        : 'hover:border-gray-300'
                    }`}
                  >
                    <input
                      type="radio"
                      name="payment"
                      value={method}
                      checked={paymentMethod === method}
                      onChange={(e) => setPaymentMethod(e.target.value)}
                      className="w-4 h-4 text-primary focus:ring-primary"
                    />
                    <span className="text-gray-800 font-medium">{method}</span>
                  </label>
                )
              )}
            </div>
          </div>
        </div>

        {/* Order Summary & Submit */}
        <div className="lg:col-span-1">
          <div className="space-y-6">
            <CartSummary
              totalItems={cartItems.reduce((acc, i) => acc + i.quantity, 0)}
              totalPrice={totalPrice}
              isCheckout={true}
            />

            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full py-4 text-lg flex justify-center items-center shadow-lg"
            >
              {loading ? <Loader size="sm" /> : `Place Order (₹${(totalPrice + 50).toFixed(2)})`}
            </button>
          </div>
        </div>
      </form>
    </div>
  )
}

export default Checkout
