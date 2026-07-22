import { useState, useEffect } from 'react'
import RestaurantList from '../components/restaurant/RestaurantList'
import restaurantService from '../services/restaurantService'

const dummyRestaurants = [
  {
    id: 1,
    name: 'The Spice Route',
    image: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400&h=300&fit=crop',
    cuisine: 'Indian, Asian',
    rating: 4.5,
    deliveryTime: '30-45 mins',
    deliveryFee: 40,
    isOpen: true,
  },
  {
    id: 2,
    name: 'Pizza Palace',
    image: 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=400&h=300&fit=crop',
    cuisine: 'Italian, Pizza',
    rating: 4.3,
    deliveryTime: '25-35 mins',
    deliveryFee: 30,
    isOpen: true,
  },
  {
    id: 3,
    name: 'Burger Haven',
    image: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&h=300&fit=crop',
    cuisine: 'American, Fast Food',
    rating: 4.7,
    deliveryTime: '20-30 mins',
    deliveryFee: 25,
    isOpen: true,
  },
  {
    id: 4,
    name: 'Sushi Master',
    image: 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=400&h=300&fit=crop',
    cuisine: 'Japanese, Sushi',
    rating: 4.8,
    deliveryTime: '35-50 mins',
    deliveryFee: 50,
    isOpen: true,
  },
  {
    id: 5,
    name: 'Green Bowl',
    image: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop',
    cuisine: 'Healthy, Salad',
    rating: 4.4,
    deliveryTime: '20-30 mins',
    deliveryFee: 30,
    isOpen: true,
  },
  {
    id: 6,
    name: 'Taco Fiesta',
    image: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=300&fit=crop',
    cuisine: 'Mexican',
    rating: 4.6,
    deliveryTime: '25-40 mins',
    deliveryFee: 35,
    isOpen: true,
  },
]

const categories = [
  'All',
  'Indian',
  'Italian',
  'American',
  'Japanese',
  'Healthy',
  'Mexican',
]

const Home = () => {
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [searchQuery, setSearchQuery] = useState('')
  const [restaurants, setRestaurants] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const fetchRestaurants = async () => {
      setLoading(true)
      try {
        const data = await restaurantService.getAll()
        if (Array.isArray(data) && data.length > 0) {
          setRestaurants(data)
        } else {
          setRestaurants(dummyRestaurants)
        }
      } catch (err) {
        // Graceful fallback to initial seed data when backend API is offline
        setRestaurants(dummyRestaurants)
      } finally {
        setLoading(false)
      }
    }

    fetchRestaurants()
  }, [])

  const filteredRestaurants = restaurants.filter((restaurant) => {
    const matchesCategory =
      selectedCategory === 'All'
        ? true
        : restaurant.cuisine?.toLowerCase().includes(selectedCategory.toLowerCase())
    const matchesSearch = restaurant.name
      ?.toLowerCase()
      .includes(searchQuery.toLowerCase())
    return matchesCategory && matchesSearch
  })

  return (
    <div>
      {/* Hero Section */}
      <section className="gradient-bg text-white py-16 px-4">
        <div className="max-w-7xl mx-auto text-center">
          <h1 className="text-4xl md:text-6xl font-bold mb-4 animate-float">
            Craving Something Delicious?
          </h1>
          <p className="text-xl md:text-2xl mb-8 opacity-90">
            Order food from the best restaurants in your city
          </p>
          <div className="max-w-2xl mx-auto">
            <div className="bg-white rounded-xl shadow-lg flex items-center p-2">
              <input
                type="text"
                placeholder="Search for restaurants or dishes..."
                className="flex-1 px-4 py-3 text-gray-800 outline-none rounded-l-xl"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <button className="btn-primary m-0 px-6 py-3 shrink-0">
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
                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                  />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Categories */}
      <section className="py-8 bg-light border-b border-gray-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex space-x-4 overflow-x-auto pb-2 scrollbar-none">
            {categories.map((category) => (
              <button
                key={category}
                onClick={() => setSelectedCategory(category)}
                className={`px-6 py-2.5 rounded-full font-medium whitespace-nowrap transition-all duration-200 ${
                  selectedCategory === category
                    ? 'bg-primary text-white shadow-md'
                    : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-200'
                }`}
              >
                {category}
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* Restaurants List */}
      <section className="py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold mb-8 text-secondary">
            Popular Restaurants Near You
          </h2>

          <RestaurantList
            restaurants={filteredRestaurants}
            loading={loading}
            error={error}
          />
        </div>
      </section>
    </div>
  )
}

export default Home
