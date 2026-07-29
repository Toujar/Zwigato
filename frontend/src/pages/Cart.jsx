import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import CartItem from '../components/cart/CartItem'
import CartSummary from '../components/cart/CartSummary'

const Cart = () => {
  const { cartItems, totalItems, totalPrice, updateQuantity, removeItem } = useCart()

  if (cartItems.length === 0) {
    return (
      <div className="min-h-[65vh] flex flex-col items-center justify-center py-12 px-4 text-center">
        {/* Glass icon container */}
        <div className="w-32 h-32 glass rounded-full flex items-center justify-center mb-6 animate-float">
          <svg className="w-16 h-16 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
              d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
        </div>
        <h2 className="text-3xl font-black text-secondary mb-2">Your Cart is Empty</h2>
        <p className="text-slate-500 max-w-md mb-8">
          Looks like you haven&apos;t added anything yet. Explore our top restaurants!
        </p>
        <Link to="/" className="btn-primary">
          Browse Restaurants
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-black mb-2 text-secondary">Your Cart</h1>
      <p className="text-slate-500 text-sm mb-8">
        {totalItems} item{totalItems !== 1 ? 's' : ''} &middot; ₹{totalPrice.toFixed(2)}
      </p>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Cart Items */}
        <div className="lg:col-span-2 space-y-4">
          {cartItems.map((item) => (
            <CartItem
              key={item.id}
              item={item}
              onUpdateQuantity={updateQuantity}
              onRemove={removeItem}
            />
          ))}
        </div>

        {/* Summary */}
        <div className="lg:col-span-1">
          <CartSummary totalItems={totalItems} totalPrice={totalPrice} />
        </div>
      </div>
    </div>
  )
}

export default Cart
