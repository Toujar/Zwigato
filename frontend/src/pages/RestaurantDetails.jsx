import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import restaurantService from '../services/restaurantService'
import menuService from '../services/menuService'
import MenuList from '../components/menu/MenuList'
import Loader from '../components/common/Loader'

const dummyMenuItems = [
  {
    id: 1,
    name: 'Butter Chicken',
    description: 'Creamy tomato-based curry with tender chicken',
    price: 299,
    image: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=200&h=150&fit=crop',
    category: 'Main Course',
    isVeg: false,
    isBestSeller: true,
    isAvailable: true,
  },
  {
    id: 2,
    name: 'Paneer Tikka Masala',
    description: 'Grilled paneer in rich spiced gravy',
    price: 269,
    image: 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=200&h=150&fit=crop',
    category: 'Main Course',
    isVeg: true,
    isBestSeller: true,
    isAvailable: true,
  },
  {
    id: 3,
    name: 'Naan',
    description: 'Soft, fluffy Indian bread',
    price: 49,
    image: 'https://images.unsplash.com/photo-1601050690597-df0568f70950?w=200&h=150&fit=crop',
    category: 'Breads',
    isVeg: true,
    isBestSeller: false,
    isAvailable: true,
  },
  {
    id: 4,
    name: 'Gulab Jamun',
    description: 'Deep-fried dumplings in sugar syrup',
    price: 129,
    image: 'https://images.unsplash.com/photo-1617424103328-765eda1a0852?w=200&h=150&fit=crop',
    category: 'Desserts',
    isVeg: true,
    isBestSeller: false,
    isAvailable: true,
  },
  {
    id: 5,
    name: 'Dal Makhani',
    description: 'Black lentils cooked with butter and cream',
    price: 249,
    image: 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=200&h=150&fit=crop',
    category: 'Main Course',
    isVeg: true,
    isBestSeller: true,
    isAvailable: true,
  },
  {
    id: 6,
    name: 'Vegetable Biryani',
    description: 'Fragrant rice with mixed vegetables',
    price: 279,
    image: 'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=200&h=150&fit=crop',
    category: 'Rice',
    isVeg: true,
    isBestSeller: false,
    isAvailable: true,
  },
]

const dummyRestaurant = {
  id: 1,
  name: 'The Spice Route',
  image: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&h=400&fit=crop',
  cuisine: 'Indian, Asian',
  rating: 4.5,
  deliveryTime: '30-45 mins',
  deliveryFee: 40,
  isOpen: true,
  address: '123 Food Street, Bangalore',
}

const RestaurantDetails = () => {
  const { id } = useParams()
  const [restaurant, setRestaurant] = useState(null)
  const [menuItems, setMenuItems] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      try {
        const [resData, menuData] = await Promise.allSettled([
          restaurantService.getById(id),
          menuService.getByRestaurant(id),
        ])

        if (resData.status === 'fulfilled' && resData.value) {
          setRestaurant(resData.value)
        } else {
          setRestaurant(dummyRestaurant)
        }

        if (menuData.status === 'fulfilled' && Array.isArray(menuData.value) && menuData.value.length > 0) {
          setMenuItems(menuData.value)
        } else {
          setMenuItems(dummyMenuItems)
        }
      } catch (err) {
        setRestaurant(dummyRestaurant)
        setMenuItems(dummyMenuItems)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [id])

  if (loading) {
    return <Loader fullPage />
  }

  const currentRest = restaurant || dummyRestaurant

  return (
    <div>
      {/* Restaurant Header */}
      <div className="relative">
        <img
          src={
            currentRest.imageUrl ||
            currentRest.image ||
            'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&h=400&fit=crop'
          }
          alt={currentRest.name}
          className="w-full h-64 md:h-80 object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent" />
        <div className="absolute bottom-0 left-0 right-0 p-6 text-white max-w-7xl mx-auto">
          <h1 className="text-3xl md:text-5xl font-bold mb-2">{currentRest.name}</h1>
          <p className="text-lg opacity-90 mb-3">{currentRest.cuisine}</p>
          <div className="flex flex-wrap items-center gap-3 text-sm">
            <div className="flex items-center bg-green-500 px-2.5 py-1 rounded font-bold">
              <svg
                className="w-4 h-4 mr-1 fill-current"
                viewBox="0 0 20 20"
              >
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
              {currentRest.rating || 4.5}
            </div>
            <span>•</span>
            <span>{currentRest.deliveryTime || '30-45 mins'}</span>
            <span>•</span>
            <span>₹{currentRest.deliveryFee || 40} delivery</span>
          </div>
          {currentRest.address && (
            <p className="mt-3 text-sm opacity-80">{currentRest.address}</p>
          )}
        </div>
      </div>

      {/* Menu Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h2 className="text-3xl font-bold mb-6 text-secondary">Menu</h2>
        <MenuList menuItems={menuItems} />
      </div>
    </div>
  )
}

export default RestaurantDetails
