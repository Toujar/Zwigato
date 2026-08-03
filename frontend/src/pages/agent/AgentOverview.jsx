import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import deliveryService from '../../services/deliveryService'
import Loader from '../../components/common/Loader'

const StatCard = ({ icon, label, value, sub, color = '#22C55E' }) => (
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

const AgentOverview = () => {
  const { user }              = useAuth()
  const [myOrders, setMyOrders]       = useState([])
  const [available, setAvailable]     = useState([])
  const [loading, setLoading]         = useState(true)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [myRes, avRes] = await Promise.all([
          deliveryService.getMyDeliveries({ page: 0, size: 100 }),
          deliveryService.getAvailableOrders(),
        ])
        const my = myRes?.content ?? (Array.isArray(myRes) ? myRes : [])
        setMyOrders(my)
        setAvailable(Array.isArray(avRes) ? avRes : [])
      } catch { /* keep empty */ }
      finally { setLoading(false) }
    }
    load()
  }, [])

  if (loading) return <Loader fullPage />

  const delivered    = myOrders.filter(o => o.status === 'DELIVERED')
  const active       = myOrders.filter(o => o.status === 'OUT_FOR_DELIVERY')
  const totalEarned  = delivered.reduce((s, o) => s + Number(o.deliveryFee ?? 40), 0)
  const todayStr     = new Date().toDateString()
  const todayEarned  = delivered
    .filter(o => o.placedAt && new Date(o.placedAt).toDateString() === todayStr)
    .reduce((s, o) => s + Number(o.deliveryFee ?? 40), 0)

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-black text-secondary">
          Hey {user?.name?.split(' ')[0]} 🛵
        </h1>
        <p className="text-slate-500 text-sm mt-1">Here's your delivery overview</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
        <StatCard icon="✅" label="Delivered"     value={delivered.length}   sub="All time" />
        <StatCard icon="🛵" label="Active"        value={active.length}      sub="In progress" color="#0EA5E9" />
        <StatCard icon="📦" label="Available Now" value={available.length}   sub="Ready for pickup" color="#F97316" />
        <StatCard icon="💰" label="Total Earned"  value={`₹${totalEarned}`} sub={`₹${todayEarned} today`} color="#8B5CF6" />
      </div>

      {/* Quick links */}
      <div className="grid sm:grid-cols-2 gap-4 mb-10">
        <Link to="/agent/available"
          className="glass p-6 hover:shadow-glass-lg transition-all duration-300 group">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl flex items-center justify-center text-3xl"
              style={{ background:'rgba(249,115,22,0.12)', border:'1px solid rgba(249,115,22,0.25)' }}>
              📦
            </div>
            <div>
              <h3 className="font-black text-secondary group-hover:text-primary transition-colors">
                Available Deliveries
              </h3>
              <p className="text-slate-500 text-sm">
                {available.length} order{available.length !== 1 ? 's' : ''} waiting for pickup
              </p>
            </div>
          </div>
        </Link>

        <Link to="/agent/earnings"
          className="glass p-6 hover:shadow-glass-lg transition-all duration-300 group">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl flex items-center justify-center text-3xl"
              style={{ background:'rgba(139,92,246,0.12)', border:'1px solid rgba(139,92,246,0.25)' }}>
              💰
            </div>
            <div>
              <h3 className="font-black text-secondary group-hover:text-primary transition-colors">
                My Earnings
              </h3>
              <p className="text-slate-500 text-sm">₹{totalEarned} total · ₹{todayEarned} today</p>
            </div>
          </div>
        </Link>
      </div>

      {/* Active deliveries */}
      {active.length > 0 && (
        <div>
          <h2 className="font-bold text-secondary mb-4">🟢 Active Deliveries</h2>
          <div className="space-y-3">
            {active.map(o => (
              <div key={o.id} className="glass-white p-4 flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="font-bold text-secondary">Order #{String(o.id).slice(-6)}</p>
                  <p className="text-slate-500 text-sm">📍 {o.deliveryAddress?.slice(0,50)}…</p>
                  <p className="text-green-600 text-xs font-semibold mt-0.5">🏪 {o.restaurantName}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="font-black text-primary">₹{Number(o.deliveryFee ?? 40).toFixed(0)}</span>
                  <Link to="/agent/my-deliveries" className="btn-primary text-xs px-4 py-2">
                    Manage →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default AgentOverview
