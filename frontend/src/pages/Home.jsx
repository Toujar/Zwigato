import { useState, useEffect } from 'react'
import RestaurantList from '../components/restaurant/RestaurantList'
import restaurantService from '../services/restaurantService'

const FALLBACK = [
  { id:1, name:'The Spice Route', image:'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400&h=300&fit=crop', cuisine:'Indian, Asian',      rating:4.5, deliveryTime:'30-45 mins', deliveryFee:40, isOpen:true },
  { id:2, name:'Pizza Palace',    image:'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=400&h=300&fit=crop', cuisine:'Italian, Pizza',   rating:4.3, deliveryTime:'25-35 mins', deliveryFee:30, isOpen:true },
  { id:3, name:'Burger Haven',    image:'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&h=300&fit=crop', cuisine:'American',         rating:4.7, deliveryTime:'20-30 mins', deliveryFee:25, isOpen:true },
  { id:4, name:'Sushi Master',    image:'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=400&h=300&fit=crop', cuisine:'Japanese, Sushi',  rating:4.8, deliveryTime:'35-50 mins', deliveryFee:50, isOpen:true },
  { id:5, name:'Green Bowl',      image:'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop', cuisine:'Healthy, Salads',  rating:4.4, deliveryTime:'20-30 mins', deliveryFee:30, isOpen:true },
  { id:6, name:'Taco Fiesta',     image:'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=300&fit=crop', cuisine:'Mexican',          rating:4.6, deliveryTime:'25-40 mins', deliveryFee:35, isOpen:true },
]

const CATS = ['All','Indian','Italian','American','Japanese','Healthy','Mexican','Chinese']

const Home = () => {
  const [selectedCat, setSelectedCat] = useState('All')
  const [search, setSearch]           = useState('')
  const [restaurants, setRestaurants] = useState([])
  const [loading, setLoading]         = useState(true)
  const [error, setError]             = useState(null)

  useEffect(() => {
    const timer = setTimeout(async () => {
      setLoading(true); setError(null)
      try {
        const res = await restaurantService.getAll({ page:0, size:20, keyword: search.trim() || undefined })
        const list = res?.content ?? res
        setRestaurants(Array.isArray(list) && list.length ? list : FALLBACK)
      } catch { setRestaurants(FALLBACK) }
      finally { setLoading(false) }
    }, 400)
    return () => clearTimeout(timer)
  }, [search])

  const filtered = restaurants.filter(r =>
    selectedCat === 'All' || r.cuisine?.toLowerCase().includes(selectedCat.toLowerCase())
  )

  return (
    <div>
      {/* ── Hero ─────────────────────────────────────── */}
      <section className="hero-bg text-white py-20 px-4">
        <div className="max-w-4xl mx-auto text-center relative z-10">
          <span className="inline-block badge badge-blue mb-4 text-sm px-4 py-1.5">
            🚀 Fast delivery in 30 mins
          </span>
          <h1 className="text-5xl md:text-7xl font-black mb-5 leading-tight animate-float">
            Hungry? <br className="hidden sm:block" />
            <span style={{ textShadow: '0 2px 20px rgba(255,255,255,0.30)' }}>
              We've got you.
            </span>
          </h1>
          <p className="text-xl md:text-2xl mb-10 text-white/80 font-medium">
            Order from the best restaurants near you
          </p>

          {/* Search bar */}
          <div className="max-w-2xl mx-auto glass-elevated flex items-center p-1.5 gap-2">
            <input
              type="text"
              placeholder="Search restaurants or cuisines…"
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="flex-1 bg-transparent px-4 py-3 text-slate-800 placeholder-slate-400
                         outline-none text-base font-medium"
            />
            <button className="btn-primary px-5 py-3 text-sm rounded-xl shrink-0">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </button>
          </div>

          {/* Stats */}
          <div className="flex justify-center gap-8 mt-10">
            {[['500+','Restaurants'],['50k+','Happy Customers'],['30 min','Avg Delivery']].map(([v,l]) => (
              <div key={l} className="text-center">
                <p className="text-2xl font-black text-white">{v}</p>
                <p className="text-white/60 text-xs font-medium">{l}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Category chips ─────────────────────────── */}
      <section className="py-6 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="flex gap-2.5 overflow-x-auto pb-1 scrollbar-hide">
            {CATS.map(cat => (
              <button key={cat} onClick={() => setSelectedCat(cat)}
                className={`px-5 py-2.5 rounded-2xl font-semibold whitespace-nowrap text-sm
                            transition-all duration-200 ${
                  selectedCat === cat
                    ? 'btn-primary'
                    : 'glass-subtle text-slate-600 hover:bg-sky-100/60'
                }`}>
                {cat}
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* ── Restaurant grid ────────────────────────── */}
      <section className="pb-16 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="flex items-baseline justify-between mb-6">
            <h2 className="text-2xl font-black text-slate-800">
              {search ? `Results for "${search}"` : 'Popular Restaurants'}
            </h2>
            {!loading && (
              <span className="text-slate-400 text-sm">{filtered.length} found</span>
            )}
          </div>
          <RestaurantList restaurants={filtered} loading={loading} error={error} />
        </div>
      </section>

      {/* ── How it works ───────────────────────────── */}
      <section className="py-16 px-4">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-3xl font-black text-center text-slate-800 mb-12">How It Works</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[
              { icon:'🍽️', step:'01', title:'Choose Restaurant', desc:'Browse top-rated restaurants and explore their full menus.' },
              { icon:'🛒', step:'02', title:'Add to Cart',        desc:'Pick your favourites, adjust quantities, and checkout instantly.' },
              { icon:'🚀', step:'03', title:'Fast Delivery',      desc:'Track your order in real-time and get hot food at your door.' },
            ].map(({ icon, step, title, desc }) => (
              <div key={step} className="glass-white p-8 text-center hover:shadow-glass-lg
                                         transition-all duration-300 hover:-translate-y-1">
                <div className="text-5xl mb-4">{icon}</div>
                <span className="text-xs font-black text-primary tracking-widest">{step}</span>
                <h3 className="text-xl font-bold text-slate-800 mt-2 mb-2">{title}</h3>
                <p className="text-slate-500 text-sm leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}

export default Home
