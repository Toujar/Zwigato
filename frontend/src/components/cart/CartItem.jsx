import React from 'react'

const CartItem = ({ item, onUpdateQuantity, onRemove }) => {
  const { id, name, price, quantity, image, imageUrl } = item
  const displayImage =
    imageUrl ||
    image ||
    'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=150&fit=crop'

  return (
    <div className="flex items-center justify-between p-4 bg-white rounded-xl food-card-shadow">
      <div className="flex items-center space-x-4">
        <img
          src={displayImage}
          alt={name}
          className="w-20 h-20 object-cover rounded-lg shrink-0"
        />
        <div>
          <h3 className="font-bold text-lg text-secondary">{name}</h3>
          <p className="font-semibold text-primary">₹{price}</p>
          <p className="text-xs text-gray-400 mt-1">
            Subtotal: ₹{(price * quantity).toFixed(2)}
          </p>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-3 border rounded-lg px-1">
          <button
            onClick={() => onUpdateQuantity(id, quantity - 1)}
            className="px-3 py-1 font-bold text-gray-600 hover:text-primary transition-colors"
          >
            -
          </button>
          <span className="font-bold text-base min-w-[20px] text-center">
            {quantity}
          </span>
          <button
            onClick={() => onUpdateQuantity(id, quantity + 1)}
            className="px-3 py-1 font-bold text-gray-600 hover:text-primary transition-colors"
          >
            +
          </button>
        </div>

        <button
          onClick={() => onRemove(id)}
          className="text-red-500 hover:text-red-700 transition-colors p-2"
          title="Remove item"
        >
          <svg
            className="w-5 h-5"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
            />
          </svg>
        </button>
      </div>
    </div>
  )
}

export default CartItem
