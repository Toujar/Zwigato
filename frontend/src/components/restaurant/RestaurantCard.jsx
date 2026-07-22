import React from 'react'
import { Link } from 'react-router-dom'

const RestaurantCard = ({ restaurant }) => {
  const {
    id,
    name,
    cuisine,
    rating = 4.5,
    deliveryTime = '30-45 mins',
    deliveryFee = 40,
    isOpen = true,
    image,
    imageUrl,
  } = restaurant

  const displayImage =
    imageUrl ||
    image ||
    'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400&h=300&fit=crop'

  return (
    <Link
      to={`/restaurant/${id}`}
      className="bg-white rounded-xl overflow-hidden card-shadow card-hover block h-full flex flex-col justify-between"
    >
      <div>
        <div className="relative">
          <img
            src={displayImage}
            alt={name}
            className="w-full h-48 object-cover"
          />
          <div
            className={`absolute top-4 right-4 text-white px-3 py-1 rounded-full text-xs font-semibold ${
              isOpen ? 'bg-green-500' : 'bg-red-500'
            }`}
          >
            {isOpen ? 'Open' : 'Closed'}
          </div>
        </div>

        <div className="p-5">
          <div className="flex justify-between items-start mb-2">
            <h3 className="text-xl font-bold text-secondary truncate">{name}</h3>
            <div className="flex items-center bg-green-100 text-green-700 px-2 py-1 rounded text-sm font-semibold ml-2 shrink-0">
              <svg
                className="w-4 h-4 mr-1 fill-current text-green-600"
                viewBox="0 0 20 20"
              >
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
              {rating}
            </div>
          </div>

          <p className="text-gray-500 text-sm mb-3 truncate">
            {cuisine || 'Multi-Cuisine'}
          </p>
        </div>
      </div>

      <div className="px-5 pb-5 pt-0 flex items-center space-x-4 text-gray-600 text-sm border-t border-gray-50 mt-auto">
        <span className="flex items-center">
          <svg
            className="w-4 h-4 mr-1"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          {deliveryTime}
        </span>
        <span>•</span>
        <span>₹{deliveryFee} delivery</span>
      </div>
    </Link>
  )
}

export default RestaurantCard
