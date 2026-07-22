import React from 'react'
import RestaurantCard from './RestaurantCard'
import Loader from '../common/Loader'

const RestaurantList = ({ restaurants = [], loading = false, error = null }) => {
  if (loading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        {[1, 2, 3, 4, 5, 6].map((i) => (
          <div
            key={i}
            className="bg-white rounded-xl overflow-hidden card-shadow animate-pulse h-80"
          >
            <div className="w-full h-48 bg-gray-200" />
            <div className="p-5 space-y-3">
              <div className="h-5 bg-gray-200 rounded w-3/4" />
              <div className="h-4 bg-gray-200 rounded w-1/2" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-12 bg-red-50 rounded-xl p-6">
        <p className="text-red-600 font-semibold mb-2">Error loading restaurants</p>
        <p className="text-gray-600 text-sm">{error}</p>
      </div>
    )
  }

  if (!restaurants || restaurants.length === 0) {
    return (
      <div className="text-center py-12 bg-gray-50 rounded-xl p-8">
        <h3 className="text-xl font-bold text-gray-700 mb-2">No restaurants found</h3>
        <p className="text-gray-500">Try adjusting your category or search query.</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      {restaurants.map((restaurant) => (
        <RestaurantCard key={restaurant.id} restaurant={restaurant} />
      ))}
    </div>
  )
}

export default RestaurantList
