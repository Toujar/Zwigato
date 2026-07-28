import MenuCard from './MenuCard'
import Loader from '../common/Loader'

/**
 * Renders the full restaurant menu grouped by category.
 * Props:
 *   menuItems    — array of FoodItemResponse (or fallback shape)
 *   restaurantId — forwarded to MenuCard so it can lock the cart to this restaurant
 *   loading      — shows skeleton while data loads
 */
const MenuList = ({ menuItems = [], loading = false, restaurantId }) => {
  if (loading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <div key={i} className="bg-white rounded-xl p-4 food-card-shadow animate-pulse flex gap-4">
            <div className="w-24 h-24 bg-gray-200 rounded-lg shrink-0" />
            <div className="flex-1 space-y-3 pt-1">
              <div className="h-4 bg-gray-200 rounded w-2/3" />
              <div className="h-3 bg-gray-200 rounded w-full" />
              <div className="h-3 bg-gray-200 rounded w-1/3" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (!menuItems || menuItems.length === 0) {
    return (
      <div className="text-center py-12 bg-gray-50 rounded-xl">
        <p className="text-gray-500 text-lg">No menu items available at this time.</p>
      </div>
    )
  }

  // Group by category name — backend sends categoryName, fallback to category string
  const grouped = menuItems.reduce((acc, item) => {
    const cat = item.categoryName || item.category || 'Menu'
    if (!acc[cat]) acc[cat] = []
    acc[cat].push(item)
    return acc
  }, {})

  return (
    <div className="space-y-10">
      {Object.entries(grouped).map(([category, items]) => (
        <section key={category}>
          <h3 className="text-xl font-bold text-secondary mb-4 pb-2 border-b border-gray-100">
            {category}
            <span className="ml-2 text-sm font-normal text-gray-400">({items.length})</span>
          </h3>
          <div className="space-y-4">
            {items.map((item) => (
              <MenuCard key={item.id} item={item} restaurantId={restaurantId} />
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}

export default MenuList
