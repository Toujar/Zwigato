import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import restaurantService from '../services/restaurantService'
import menuService from '../services/menuService'
import MenuList from '../components/menu/MenuList'
import Loader from '../components/common/Loader'

// Fallback data when backend is offline
const FALLBACK_RESTAURANT = {
  id: 1, name: 'The Spice Route',
  image: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&h=400&fit=crop',
  cuisine: 'Indian, Asian', rating: 4.5, deliveryTime: '30-45 mins',
  deliveryFee: 40, isOpen: true, address: '123 Food Street, Bangalore',
}

const FALLBACK_MENU = [
  { id: 1, name: 'Butter Chicken',       description: 'Creamy tomato-based curry with tender chicken', price: 299, image: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=200&h=150&fit=crop', category: 'Main Course', isVeg: false, isBestSeller: true,  isAvailable: true },
  { id: 2, name: 'Paneer Tikka Masala',  description: 'Grilled paneer in rich spiced gravy',           price: 269, image: 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=200&h=150&fit=crop', category: 'Main Course', isVeg: true,  isBestSeller: true,  isAvailable: true },
  { id: 3, name: 'Naan',                 description: 'Soft, fluffy Indian bread',                     price: 49,  image: 'https://images.unsplash.com/photo-1601050690597-df0568f70950?w=200&h=150&fit=crop', category: 'Breads',      isVeg: true,  isBestSeller: false, isAvailable: true },
  { id: 4, name: 'Dal Makhani',          description: 'Black lentils cooked with butter and cream',    price: 249, image: 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=200&h=150&fit=crop', category: 'Main Course', isVeg: true,  isBestSeller: true,  isAvailable: true },
  { id: 5, name: 'Gulab Jamun',          description: 'Deep-fried dumplings in sugar syrup',           price: 129, image: 'https://images.unsplash.com/photo-1617424103328-765eda1a0852?w=200&h=150&fit=crop', category: 'Desserts',    isVeg: true,  isBestSeller: false, isAvailable: true },
  { id: 6, name: 'Vegetable Biryani',    description: 'Fragrant basmati rice with fresh vegetables',   price: 279, image: 'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=200&h=150&fit=crop', category: 'Rice',        isVeg: true,  isBestSeller: false, isAvailable: true },
]

/**
 * Normalize a FoodItem from the backend (camelCase fields) into the
 * shape MenuCard expects (isVeg, isBestSeller, image, category string).
 */
const normalizeItem = (item) => ({
  ...item,
  image:       item.imageUrl || item.image,
  isVeg:       item.isVegetarian ?? item.isVeg ?? true,
  isBestSeller: item.isBestSeller ?? false,
  isAvailable: item.isAvailable ?? true,
  category:    item.categoryName || item.category || 'Menu',
})

const RestaurantDetails = () => {
  const { id } = useParams()
  const [restaurant, setRestaurant] = useState(null)
  const [menuItems, setMenuItems]   = useState([])
  const [loading, setLoading]       = useState(true)
  const [vegOnly, setVegOnly]       = useState(false)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      const [resResult, menuResult] = await Promise.allSettled([
        restaurantService.getById(id),
        menuService.getByRestaurant(id),
      ])

      setRestaurant(
        resResult.status === 'fulfilled' && resResult.value
          ? resResult.value
          : FALLBACK_RESTAURANT
      )

      const rawMenu =
        menuResult.status === 'fulfilled' && Array.isArray(menuResult.value) && menuResult.value.length > 0
          ? menuResult.value
          : FALLBACK_MENU

      setMenuItems(rawMenu.map(normalizeItem))
      setLoading(false)
    }
    load()
  }, [id])

  if (loading) return <Loader fullPage />

  const r = restaurant || FALLBACK_RESTAURANT
  const displayImage = r.imageUrl || r.image ||
    'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&h=400&fit=crop'

  const visibleMenu = vegOnly ? menuItems.filter((i) => i.isVeg) : menuItems

  return (
    <div>
      {/* ── Hero Banner ─────────────────────────────────── */}
      <div className="relative">
        <img src={displayImage} alt={r.name} className="w-full h-64 md:h-80 object-cover" />
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent" />

        <div className="absolute bottom-0 left-0 right-0 p-6 text-white max-w-7xl mx-auto">
          <div className="flex items-center gap-2 mb-1">
            <Link to="/" className="text-white/70 hover:text-white text-sm">Home</Link>
            <span className="text-white/50 text-sm">›</span>
            <span className="text-white text-sm">{r.name}</span>
          </div>
          <h1 className="text-3xl md:text-5xl font-bold mb-2">{r.name}</h1>
          <p className="text-lg opacity-90 mb-3">{r.cuisine}</p>

          <div className="flex flex-wrap items-center gap-3 text-sm">
            <span className="flex items-center bg-green-500 px-2.5 py-1 rounded font-bold">
              ★ {r.rating || 4.5}
            </span>
            <span>• {r.deliveryTime || `${r.deliveryTime ?? 30} mins`}</span>
            <span>• ₹{r.deliveryFee ?? 40} delivery</span>
            <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${r.isOpen ? 'bg-green-500' : 'bg-red-500'}`}>
              {r.isOpen ? 'Open Now' : 'Closed'}
            </span>
          </div>

          {r.address && <p className="mt-2 text-sm opacity-70">{r.address}</p>}
        </div>
      </div>

      {/* ── Menu Controls ────────────────────────────────── */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-secondary">Menu</h2>

          <button
            onClick={() => setVegOnly(!vegOnly)}
            className={`flex items-center gap-2 px-4 py-2 rounded-full border font-medium text-sm transition-colors ${
              vegOnly
                ? 'bg-green-500 text-white border-green-500'
                : 'bg-white text-gray-700 border-gray-300 hover:border-green-400'
            }`}
          >
            <span className="w-3 h-3 border-2 border-current rounded-sm inline-flex items-center justify-center">
              <span className={`block w-1.5 h-1.5 rounded-full ${vegOnly ? 'bg-white' : 'bg-green-500'}`} />
            </span>
            Veg Only
          </button>
        </div>

        <MenuList menuItems={visibleMenu} restaurantId={id} />
      </div>
    </div>
  )
}

export default RestaurantDetails
