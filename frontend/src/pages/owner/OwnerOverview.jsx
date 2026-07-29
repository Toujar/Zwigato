import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import restaurantService from '../../services/restaurantService'
import orderService from '../../services/orderService'
import Loader from '../../components/common/Loader'

const StatCard = ({ icon, label, value, sub, color = '#0EA5E9' }) => (
  <div className="glass p-5">
    <div className="flex items-center gap-3 mb-3">
      <div className="w-10 h-10 rounded-xl flex items-center justify-center text-xl"
        style={{ background: `${color}22`, border: `1px solid ${color}33` }}>
        {icon}
      </div>
      <span className="text-slate-500 text-sm font-medium">{label}</span>
    </div>
    <p className="text-3xl font-black text-secondary">{value}</p>
    {sub && <p className="text-slate-400 text-xs mt-1">{sub}</p>}
  </div>
)

const OwnerOverview = () => {
  const { user }              = useAuth()
  const [restaurants, setRestaurants] = useState([])
  const [orders, setOrders]   = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [rRes, oRes] = await Promise.all([
          restaurantService.getAll({ page:0, size:50 }),
          orderService.getOrders({ page:0, size:100 }),
        ])
        setRestaurants(rRes?.content ?? (Array.isArray(rRes) ? rRes : []))
        const ol = oRes?.content ?? (Array.isArray(oRes) ? oRes : [])
        setOrders(ol)
      } catch { /* keep empty */ }
      finally { setLoading(false) }
    }
    load()
  }, [])

  if (loading) return <Loader fullPage />

  const activeOrders   = orders.filter(o => !['DELIVERED','CANCELLED'].includes(o.status))
  const revenue        = orders.filter(o => o.status === 'DELIVERED').reduce((s, o) => s + Number(o.totalAmount), 0)
  const openRestaurants= restaurants.filter(r => r.open || r.isOpen).length

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-black text-secondary">
          Welcome back, {user?.name?.split(' ')[0]} 👋
        </h1>
        <p className="text-slate-500 text-sm mt-1">Here's your restaurant overview</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
        <StatCard icon="🏪" label="Restaurants"   value={restaurants.length} sub={`${openRestaurants} open now`} />
        <StatCard icon="📋" label="Active Orders"  value={activeOrders.length} sub="Need attention" color="#F97316" />
        <StatCard icon="✅" label="Total Orders"   value={orders.length} sub="All time" color="#22C55E" />
        <StatCard icon="💰" label="Revenue"         value={`₹${revenue.toFixed(0)}`} sub="Delivered orders" color="#8B5CF6" />
      </div>

      {/* Quick links */}
      <div className="grid sm:grid-cols-2 gap-4 mb-10">
        <Link to="/dashboard/restaurants" className="glass p-6 hover:shadow-glass-lg transition-all duration-300 group">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl flex items-center justify-center text-3xl"
              style={{ background:'rgba(14,165,233,0.12)', border:'1px solid rgba(14,165,233,0.25)' }}>
              🏪
            </div>
            <div>
              <h3 className="font-black text-secondary group-hover:text-primary transition-colors">Manage Restaurants</h3>
              <p className="text-slate-500 text-sm">Add, edit, or toggle open/closed</p>
            </div>
          </div>
        </Link>
        <Link to="/dashboard/orders" className="glass p-6 hover:shadow-glass-lg transition-all duration-300 group">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl flex items-center justify-center text-3xl"
              style={{ background:'rgba(249,115,22,0.12)', border:'1px solid rgba(249,115,22,0.25)' }}>
              📋
            </div>
            <div>
              <h3 className="font-black text-secondary group-hover:text-primary transition-colors">View Orders</h3>
              <p className="text-slate-500 text-sm">Track and update order status</p>
            </div>
          </div>
        </Link>
      </div>

      {/* Active orders preview */}
      {activeOrders.length > 0 && (
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-bold text-secondary">Active Orders</h2>
            <Link to="/dashboard/orders" className="text-primary text-sm font-semibold hover:underline">View all →</Link>
          </div>
          <div className="space-y-3">
            {activeOrders.slice(0, 5).map(o => (
              <div key={o.id} className="glass-subtle flex items-center justify-between p-4 rounded-2xl">
                <div>
                  <span className="font-bold text-secondary text-sm">Order #{String(o.id).slice(-6)}</span>
                  <p className="text-slate-400 text-xs mt-0.5">{o.deliveryAddress?.slice(0,40)}...</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="font-black text-primary">₹{Number(o.totalAmount).toFixed(0)}</span>
                  <span className={`badge badge-orange text-xs`}>{o.status?.replace(/_/g,' ')}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default OwnerOverview
