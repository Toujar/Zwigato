import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useToast } from '../context/ToastContext'
import CartSummary from '../components/cart/CartSummary'
import orderService from '../services/orderService'
import Loader from '../components/common/Loader'

// Maps the frontend payment label to the backend PaymentMethod enum value
const PAYMENT_MAP = {
  'UPI':                  'UPI',
  'Credit/Debit Card':    'CREDIT_CARD',
  'Net Banking':          'NET_BANKING',
  'Cash on Delivery':     'CASH_ON_DELIVERY',
}

const Checkout = () => {
  const { cartItems, totalPrice, clearCart } = useCart()
  const toast    = useToast()
  const navigate = useNavigate()

  const [address, setAddress]             = useState('')
  const [city, setCity]                   = useState('')
  const [pincode, setPincode]             = useState('')
  const [paymentLabel, setPaymentLabel]   = useState('UPI')
  const [loading, setLoading]             = useState(false)

  const DELIVERY_FEE = 40
  const TAX_RATE     = 0.05
  const tax          = +(totalPrice * TAX_RATE).toFixed(2)
  const grandTotal   = +(totalPrice + DELIVERY_FEE + tax).toFixed(2)

  const handlePlaceOrder = async (e) => {
    e.preventDefault()
    if (!address || !city || !pincode) {
      toast.error('Please complete all delivery address fields')
      return
    }

    // Cart must have a restaurant reference
    const restaurantId = cartItems[0]?.restaurantId
    if (!restaurantId) {
      toast.error('Unable to determine restaurant. Please add items from the restaurant page.')
      return
    }

    setLoading(true)
    try {
      // Build the OrderRequest matching the backend DTO
      const orderRequest = {
        restaurantId,
        deliveryAddress: `${address}, ${city} - ${pincode}`,
        items: cartItems.map((item) => ({
          foodItemId: item.id,
          quantity:   item.quantity,
        })),
        specialInstructions: '',
      }

      const order = await orderService.placeOrder(orderRequest)
      clearCart()
      toast.success('Order placed successfully!')
      navigate(`/order-success/${order.id}`, { replace: true })
    } catch (err) {
      toast.error(err.message || 'Failed to place order. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  if (cartItems.length === 0) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center py-12 text-center">
        <h2 className="text-2xl font-bold text-gray-700 mb-3">Your cart is empty</h2>
        <Link to="/" className="btn-primary mt-2">Browse Restaurants</Link>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold mb-8 text-secondary">Checkout</h1>

      <form onSubmit={handlePlaceOrder} className="grid grid-cols-1 lg:grid-cols-3 gap-8">

        {/* ── Left column ── */}
        <div className="lg:col-span-2 space-y-8">

          {/* Delivery Address */}
          <div className="bg-white rounded-xl p-6 card-shadow">
            <h2 className="text-xl font-bold mb-5 text-secondary flex items-center gap-2">
              <span className="w-7 h-7 bg-primary text-white rounded-full flex items-center justify-center text-sm font-bold">1</span>
              Delivery Address
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-gray-700 font-medium mb-1 text-sm">Street Address *</label>
                <input
                  type="text" value={address} onChange={(e) => setAddress(e.target.value)}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
                  placeholder="House No., Street Name, Area" required
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-gray-700 font-medium mb-1 text-sm">City *</label>
                  <input
                    type="text" value={city} onChange={(e) => setCity(e.target.value)}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
                    placeholder="City" required
                  />
                </div>
                <div>
                  <label className="block text-gray-700 font-medium mb-1 text-sm">Pincode *</label>
                  <input
                    type="text" value={pincode} onChange={(e) => setPincode(e.target.value)}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
                    placeholder="560001" required pattern="[0-9]{5,6}"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Payment Method */}
          <div className="bg-white rounded-xl p-6 card-shadow">
            <h2 className="text-xl font-bold mb-5 text-secondary flex items-center gap-2">
              <span className="w-7 h-7 bg-primary text-white rounded-full flex items-center justify-center text-sm font-bold">2</span>
              Payment Method
            </h2>
            <div className="space-y-3">
              {Object.keys(PAYMENT_MAP).map((label) => (
                <label
                  key={label}
                  className={`flex items-center gap-3 p-4 border rounded-lg cursor-pointer transition-all ${
                    paymentLabel === label
                      ? 'border-primary bg-orange-50/40 shadow-sm'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <input
                    type="radio" name="payment" value={label}
                    checked={paymentLabel === label}
                    onChange={() => setPaymentLabel(label)}
                    className="w-4 h-4 text-primary focus:ring-primary accent-primary"
                  />
                  <span className="text-gray-800 font-medium">{label}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Items review */}
          <div className="bg-white rounded-xl p-6 card-shadow">
            <h2 className="text-xl font-bold mb-4 text-secondary flex items-center gap-2">
              <span className="w-7 h-7 bg-primary text-white rounded-full flex items-center justify-center text-sm font-bold">3</span>
              Review Items
            </h2>
            <div className="divide-y divide-gray-100">
              {cartItems.map((item) => (
                <div key={item.id} className="flex justify-between items-center py-3">
                  <div className="flex items-center gap-3">
                    <img
                      src={item.imageUrl || item.image || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=80&h=80&fit=crop'}
                      alt={item.name}
                      className="w-12 h-12 rounded-lg object-cover"
                    />
                    <div>
                      <p className="font-semibold text-secondary text-sm">{item.name}</p>
                      <p className="text-gray-500 text-xs">× {item.quantity}</p>
                    </div>
                  </div>
                  <p className="font-bold text-secondary">₹{(item.price * item.quantity).toFixed(2)}</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── Right column ── */}
        <div className="lg:col-span-1">
          <div className="sticky top-24 space-y-4">
            <CartSummary
              totalItems={cartItems.reduce((a, i) => a + i.quantity, 0)}
              totalPrice={totalPrice}
              deliveryFee={DELIVERY_FEE}
              serviceFee={+tax}
              isCheckout
            />
            <button
              type="submit" disabled={loading}
              className="btn-primary w-full py-4 text-lg flex justify-center items-center gap-2 shadow-lg"
            >
              {loading ? <Loader size="sm" /> : `Place Order • ₹${grandTotal}`}
            </button>
            <p className="text-xs text-gray-400 text-center">
              By placing this order you agree to our Terms of Service
            </p>
          </div>
        </div>
      </form>
    </div>
  )
}

export default Checkout
