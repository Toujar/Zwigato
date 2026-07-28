import { Link } from 'react-router-dom'

const RestaurantCard = ({ restaurant }) => {
  const { id, name, cuisine, rating = 4.5, deliveryTime = '30-45 mins',
          deliveryFee = 40, isOpen = true, image, imageUrl } = restaurant

  const src = imageUrl || image ||
    'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400&h=300&fit=crop'

  return (
    <Link to={`/restaurant/${id}`}
      className="group block glass-white hover:shadow-glass-lg transition-all duration-300
                 hover:-translate-y-1 overflow-hidden h-full flex flex-col">

      {/* Image */}
      <div className="relative overflow-hidden rounded-t-[20px]">
        <img src={src} alt={name}
          className="w-full h-48 object-cover transition-transform duration-500 group-hover:scale-105"
          loading="lazy" />

        {/* Gradient overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/30 to-transparent" />

        {/* Open/closed badge */}
        <span className={`absolute top-3 right-3 badge ${isOpen ? 'badge-green' : 'badge-red'}`}>
          <span className={`w-1.5 h-1.5 rounded-full ${isOpen ? 'bg-green-500' : 'bg-red-500'}`} />
          {isOpen ? 'Open' : 'Closed'}
        </span>
      </div>

      {/* Info */}
      <div className="p-4 flex-1 flex flex-col justify-between">
        <div>
          <div className="flex justify-between items-start gap-2 mb-1">
            <h3 className="text-lg font-bold text-slate-800 leading-tight line-clamp-1">{name}</h3>
            <span className="flex items-center gap-1 badge badge-green shrink-0">
              ★ {rating}
            </span>
          </div>
          <p className="text-slate-500 text-sm line-clamp-1">{cuisine || 'Multi-Cuisine'}</p>
        </div>

        <div className="flex items-center gap-3 mt-3 pt-3 text-sm text-slate-500"
          style={{ borderTop: '1px solid rgba(186,230,253,0.45)' }}>
          <span className="flex items-center gap-1">
            <svg className="w-3.5 h-3.5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {deliveryTime}
          </span>
          <span className="text-sky-200">•</span>
          <span>₹{deliveryFee} delivery</span>
        </div>
      </div>
    </Link>
  )
}

export default RestaurantCard
