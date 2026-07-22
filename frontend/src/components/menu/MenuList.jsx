import React from 'react'
import MenuCard from './MenuCard'
import Loader from '../common/Loader'

const MenuList = ({ menuItems = [], loading = false }) => {
  if (loading) {
    return <Loader size="lg" />
  }

  if (!menuItems || menuItems.length === 0) {
    return (
      <div className="text-center py-8 bg-gray-50 rounded-xl">
        <p className="text-gray-500">No menu items available at this time.</p>
      </div>
    )
  }

  const groupedMenu = menuItems.reduce((acc, item) => {
    const category = item.category || 'Main Course'
    if (!acc[category]) {
      acc[category] = []
    }
    acc[category].push(item)
    return acc
  }, {})

  return (
    <div className="space-y-8">
      {Object.entries(groupedMenu).map(([category, items]) => (
        <div key={category} className="mb-8">
          <h3 className="text-2xl font-bold text-secondary mb-4 pb-2 border-b border-gray-100">
            {category}
          </h3>
          <div className="space-y-4">
            {items.map((item) => (
              <MenuCard key={item.id} item={item} />
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

export default MenuList
