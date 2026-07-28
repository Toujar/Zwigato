import RestaurantCard from './RestaurantCard'

const SkeletonCard = () => (
  <div className="glass-white overflow-hidden animate-pulse">
    <div className="w-full h-48 bg-sky-100/80 shimmer" />
    <div className="p-4 space-y-3">
      <div className="flex justify-between">
        <div className="h-5 bg-sky-100/80 rounded-lg w-3/5 shimmer" />
        <div className="h-5 bg-sky-100/80 rounded-lg w-12 shimmer" />
      </div>
      <div className="h-4 bg-sky-100/80 rounded-lg w-2/5 shimmer" />
      <div className="h-px bg-sky-100/60" />
      <div className="h-4 bg-sky-100/80 rounded-lg w-3/4 shimmer" />
    </div>
  </div>
)

const RestaurantList = ({ restaurants = [], loading = false, error = null }) => {
  if (loading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}
      </div>
    )
  }

  if (error) {
    return (
      <div className="glass-white border border-red-200/60 p-8 text-center">
        <p className="text-red-500 font-semibold mb-1">Failed to load restaurants</p>
        <p className="text-slate-500 text-sm">{error}</p>
      </div>
    )
  }

  if (!restaurants.length) {
    return (
      <div className="glass-white p-12 text-center">
        <div className="text-5xl mb-4">🍽️</div>
        <h3 className="text-xl font-bold text-slate-700 mb-2">No restaurants found</h3>
        <p className="text-slate-400 text-sm">Try adjusting your search or filter.</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      {restaurants.map(r => <RestaurantCard key={r.id} restaurant={r} />)}
    </div>
  )
}

export default RestaurantList
