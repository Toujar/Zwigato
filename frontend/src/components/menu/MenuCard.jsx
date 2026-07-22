import React from 'react'
import { useCart } from '../../context/CartContext'
import { useToast } from '../../context/ToastContext'

const MenuCard = ({ item }) => {
  const { addItem } = useCart()
  const { success } = useToast()

  const {
    id,
    name,
    description,
    price,
    image,
    imageUrl,
    isVeg = true,
    isBestSeller = false,
    isAvailable = true,
  } = item

  const displayImage =
    imageUrl ||
    image ||
    'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=150&fit=crop'

  const handleAddToCart = () => {
    addItem(item)
    success(`Added ${name} to cart!`)
  }

  return (
    <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center p-4 bg-white rounded-xl food-card-shadow hover:shadow-md transition-shadow gap-4">
      <div className="flex items-start space-x-4">
        <img
          src={displayImage}
          alt={name}
          className="w-24 h-24 object-cover rounded-lg shrink-0"
        />
        <div>
          <div className="flex items-center space-x-2 mb-1">
            {isBestSeller && (
              <span className="text-xs bg-orange-100 text-orange-700 px-2 py-0.5 rounded font-semibold">
                Best Seller
              </span>
            )}
            <span
              className={`w-3 h-3 border-2 rounded-sm flex items-center justify-center ${
                isVeg ? 'border-green-600' : 'border-red-600'
              }`}
            >
              <span
                className={`block w-1.5 h-1.5 rounded-full ${
                  isVeg ? 'bg-green-600' : 'bg-red-600'
                }`}
              />
            </span>
          </div>

          <h4 className="font-bold text-lg text-secondary">{name}</h4>
          <p className="text-gray-500 text-sm mb-2 line-clamp-2">{description}</p>
          <p className="font-bold text-primary">₹{price}</p>
        </div>
      </div>

      <button
        onClick={handleAddToCart}
        disabled={!isAvailable}
        className={`px-6 py-2.5 rounded-lg font-semibold transition-all duration-300 shrink-0 self-end sm:self-center ${
          isAvailable
            ? 'btn-primary'
            : 'bg-gray-200 text-gray-400 cursor-not-allowed'
        }`}
      >
        {isAvailable ? 'Add' : 'Sold Out'}
      </button>
    </div>
  )
}

export default MenuCard
