import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import restaurantService from '../../services/restaurantService'
import Loader from '../../components/common/Loader'

const EMPTY = { name:'', description:'', address:'', city:'', phone:'', email:'', deliveryTime:30, minOrderAmount:0 }

const Field = ({ label, name, value, onChange, type='text', placeholder='', required=false, as='input' }) => (
  <div>
    <label className="block text-slate-700 font-semibold mb-1.5 text-sm">{label}{required && ' *'}</label>
    {as === 'textarea'
      ? <textarea name={name} value={value} onChange={onChange} placeholder={placeholder}
          rows={3} className="input-glass resize-none" />
      : <input type={type} name={name} value={value} onChange={onChange}
          placeholder={placeholder} required={required} className="input-glass" />
    }
  </div>
)

const RestaurantModal = ({ restaurant, onClose, onSaved }) => {
  const toast = useToast()
  const [form, setForm]     = useState(restaurant
    ? { name:restaurant.name, description:restaurant.description||'',
        address:restaurant.address, city:restaurant.city,
        phone:restaurant.phone, email:restaurant.email||'',
        deliveryTime:restaurant.deliveryTime||30,
        minOrderAmount:restaurant.minOrderAmount||0 }
    : EMPTY)
  const [saving, setSaving] = useState(false)

  const handle = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const submit = async e => {
    e.preventDefault()
    setSaving(true)
    try {
      const payload = { ...form, deliveryTime: Number(form.deliveryTime), minOrderAmount: Number(form.minOrderAmount) }
      const saved = restaurant
        ? await restaurantService.update(restaurant.id, payload)
        : await restaurantService.create(payload)
      toast.success(restaurant ? 'Restaurant updated!' : 'Restaurant created!')
      onSaved(saved)
    } catch (err) {
      toast.error(err.message || 'Failed to save restaurant')
    } finally { setSaving(false) }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(15,23,42,0.60)', backdropFilter: 'blur(4px)' }}>
      <div className="glass-elevated w-full max-w-2xl max-h-[90vh] overflow-y-auto p-8 animate-scale-in">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-black text-secondary">
            {restaurant ? 'Edit Restaurant' : 'Add New Restaurant'}
          </h2>
          <button onClick={onClose} className="w-9 h-9 rounded-xl glass flex items-center justify-center text-slate-500 hover:text-red-500 transition-colors">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Restaurant Name" name="name" value={form.name} onChange={handle} placeholder="e.g. Biryani House" required />
            <Field label="City" name="city" value={form.city} onChange={handle} placeholder="Bengaluru" required />
          </div>
          <Field label="Description" name="description" value={form.description} onChange={handle} placeholder="Short description of your restaurant" as="textarea" />
          <Field label="Full Address" name="address" value={form.address} onChange={handle} placeholder="Street, Area" required />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Phone" name="phone" value={form.phone} onChange={handle} placeholder="9811000001" required />
            <Field label="Email" name="email" type="email" value={form.email} onChange={handle} placeholder="info@restaurant.com" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Delivery Time (mins)" name="deliveryTime" type="number" value={form.deliveryTime} onChange={handle} />
            <Field label="Min Order Amount (₹)" name="minOrderAmount" type="number" value={form.minOrderAmount} onChange={handle} />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={saving} className="btn-primary flex-1 flex justify-center items-center gap-2">
              {saving ? <Loader size="sm" /> : (restaurant ? 'Save Changes' : 'Create Restaurant')}
            </button>
            <button type="button" onClick={onClose} className="btn-glass px-6">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  )
}

const OwnerRestaurants = () => {
  const { user }          = useAuth()
  const toast             = useToast()
  const [list, setList]   = useState([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(null) // null | 'add' | restaurantObj

  const load = async () => {
    setLoading(true)
    try {
      const res  = await restaurantService.getAll({ page:0, size:50 })
      const all  = res?.content ?? (Array.isArray(res) ? res : [])
      // Filter to only this owner's restaurants
      const mine = all.filter(r => r.ownerEmail === user?.email || r.ownerId === user?.id)
      setList(mine.length ? mine : all) // fallback show all if ownerEmail not in response
    } catch { setList([]) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const handleToggle = async (id) => {
    try {
      const updated = await restaurantService.toggleOpen(id)
      setList(l => l.map(r => r.id === id ? { ...r, open: updated.open } : r))
      toast.success('Status updated')
    } catch (err) { toast.error(err.message) }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this restaurant? This cannot be undone.')) return
    try {
      await restaurantService.remove(id)
      setList(l => l.filter(r => r.id !== id))
      toast.success('Restaurant removed')
    } catch (err) { toast.error(err.message) }
  }

  const handleSaved = (saved) => {
    setList(l => {
      const exists = l.find(r => r.id === saved.id)
      return exists ? l.map(r => r.id === saved.id ? saved : r) : [saved, ...l]
    })
    setModal(null)
  }

  if (loading) return <Loader fullPage />

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-black text-secondary">My Restaurants</h1>
          <p className="text-slate-500 text-sm mt-1">{list.length} restaurant{list.length !== 1 ? 's' : ''} registered</p>
        </div>
        <button onClick={() => setModal('add')} className="btn-primary flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4"/>
          </svg>
          Add Restaurant
        </button>
      </div>

      {list.length === 0 ? (
        <div className="glass text-center py-16 px-8">
          <div className="text-5xl mb-4">🏪</div>
          <h3 className="text-xl font-bold text-secondary mb-2">No restaurants yet</h3>
          <p className="text-slate-500 mb-6">Add your first restaurant to get started</p>
          <button onClick={() => setModal('add')} className="btn-primary">Add Restaurant</button>
        </div>
      ) : (
        <div className="grid gap-5">
          {list.map(r => (
            <div key={r.id} className="glass p-5 flex flex-col sm:flex-row gap-4 sm:items-center">
              <img src={r.imageUrl || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=120&h=80&fit=crop'}
                alt={r.name} className="w-full sm:w-24 h-20 sm:h-16 object-cover rounded-xl shrink-0" />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="font-bold text-secondary">{r.name}</h3>
                  <span className={`badge ${r.open || r.isOpen ? 'badge-green' : 'badge-red'}`}>
                    {r.open || r.isOpen ? 'Open' : 'Closed'}
                  </span>
                </div>
                <p className="text-slate-500 text-sm mt-0.5">{r.city} · ⏱ {r.deliveryTime} mins · ₹{r.minOrderAmount} min order</p>
                <p className="text-slate-400 text-xs mt-0.5 truncate">{r.address}</p>
              </div>
              <div className="flex flex-wrap gap-2 shrink-0">
                <Link to={`/dashboard/menu/${r.id}`}
                  className="btn-glass text-sm px-4 py-2">
                  🍽 Menu
                </Link>
                <button onClick={() => handleToggle(r.id)} className="btn-glass text-sm px-4 py-2">
                  {r.open || r.isOpen ? 'Close' : 'Open'}
                </button>
                <button onClick={() => setModal(r)} className="btn-glass text-sm px-4 py-2">
                  ✏️ Edit
                </button>
                <button onClick={() => handleDelete(r.id)}
                  className="px-4 py-2 rounded-2xl text-sm font-semibold text-red-500 hover:bg-red-50/60 border border-red-200/50 transition-all">
                  🗑 Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {modal && (
        <RestaurantModal
          restaurant={modal === 'add' ? null : modal}
          onClose={() => setModal(null)}
          onSaved={handleSaved}
        />
      )}
    </div>
  )
}

export default OwnerRestaurants
