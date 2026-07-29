import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useToast } from '../../context/ToastContext'
import menuService from '../../services/menuService'
import restaurantService from '../../services/restaurantService'
import categoryService from '../../services/categoryService'
import Loader from '../../components/common/Loader'

const EMPTY_ITEM = { name:'', description:'', price:'', categoryId:'', isVegetarian:false, isAvailable:true, imageUrl:'' }

// ─── Food Item Form Modal ──────────────────────────────────────────────────
const ItemModal = ({ item, restaurantId, categories, onClose, onSaved }) => {
  const toast = useToast()
  const [form, setForm]     = useState(item
    ? { name:item.name, description:item.description||'', price:item.price,
        categoryId:item.categoryId||item.category?.id||'',
        isVegetarian:item.isVegetarian||false, isAvailable:item.isAvailable??true,
        imageUrl:item.imageUrl||'' }
    : EMPTY_ITEM)
  const [saving, setSaving] = useState(false)

  const handle = e => {
    const { name, value, type, checked } = e.target
    setForm(f => ({ ...f, [name]: type === 'checkbox' ? checked : value }))
  }

  const submit = async e => {
    e.preventDefault()
    if (!form.categoryId) { toast.error('Please select a category'); return }
    setSaving(true)
    try {
      const payload = { ...form, restaurantId: Number(restaurantId),
        categoryId: Number(form.categoryId), price: Number(form.price) }
      const saved = item
        ? await menuService.updateItem(item.id, payload)
        : await menuService.addItem(payload)
      toast.success(item ? 'Item updated!' : 'Item added!')
      onSaved(saved)
    } catch (err) { toast.error(err.message || 'Failed to save item') }
    finally { setSaving(false) }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background:'rgba(15,23,42,0.60)', backdropFilter:'blur(4px)' }}>
      <div className="glass-elevated w-full max-w-xl max-h-[92vh] overflow-y-auto p-8 animate-scale-in">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-black text-secondary">{item ? 'Edit Item' : 'Add Food Item'}</h2>
          <button onClick={onClose} className="w-9 h-9 rounded-xl glass flex items-center justify-center text-slate-500 hover:text-red-500 transition-colors">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>

        <form onSubmit={submit} className="space-y-4">
          {/* Name */}
          <div>
            <label className="block text-slate-700 font-semibold mb-1.5 text-sm">Item Name *</label>
            <input name="name" value={form.name} onChange={handle} required
              className="input-glass" placeholder="e.g. Chicken Biryani" />
          </div>

          {/* Description */}
          <div>
            <label className="block text-slate-700 font-semibold mb-1.5 text-sm">Description</label>
            <textarea name="description" value={form.description} onChange={handle}
              className="input-glass resize-none" rows={2} placeholder="Brief description of the dish" />
          </div>

          {/* Price + Category */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-slate-700 font-semibold mb-1.5 text-sm">Price (₹) *</label>
              <input type="number" name="price" value={form.price} onChange={handle}
                required min="0.01" step="0.01" className="input-glass" placeholder="199" />
            </div>
            <div>
              <label className="block text-slate-700 font-semibold mb-1.5 text-sm">Category *</label>
              <select name="categoryId" value={form.categoryId} onChange={handle}
                className="input-glass appearance-none">
                <option value="">Select category</option>
                {categories.map(c => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Image URL */}
          <div>
            <label className="block text-slate-700 font-semibold mb-1.5 text-sm">Image URL</label>
            <input name="imageUrl" value={form.imageUrl} onChange={handle}
              className="input-glass" placeholder="https://..." />
          </div>

          {/* Flags */}
          <div className="flex gap-6">
            <label className="flex items-center gap-2.5 cursor-pointer">
              <input type="checkbox" name="isVegetarian" checked={form.isVegetarian} onChange={handle}
                className="w-4 h-4 accent-green-500" />
              <span className="text-sm font-semibold text-slate-700">🌿 Vegetarian</span>
            </label>
            <label className="flex items-center gap-2.5 cursor-pointer">
              <input type="checkbox" name="isAvailable" checked={form.isAvailable} onChange={handle}
                className="w-4 h-4 accent-primary" />
              <span className="text-sm font-semibold text-slate-700">✅ Available</span>
            </label>
          </div>

          {/* Image preview */}
          {form.imageUrl && (
            <img src={form.imageUrl} alt="preview" className="w-full h-32 object-cover rounded-2xl" />
          )}

          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={saving} className="btn-primary flex-1 flex justify-center items-center gap-2">
              {saving ? <Loader size="sm" /> : (item ? 'Save Changes' : 'Add Item')}
            </button>
            <button type="button" onClick={onClose} className="btn-glass px-6">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ─── Main Page ─────────────────────────────────────────────────────────────
const OwnerMenuManager = () => {
  const { restaurantId }       = useParams()
  const toast                  = useToast()
  const [restaurant, setRestaurant] = useState(null)
  const [items, setItems]      = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading]  = useState(true)
  const [modal, setModal]      = useState(null) // null | 'add' | itemObj
  const [filter, setFilter]    = useState('all') // all | veg | nonveg

  useEffect(() => {
    const init = async () => {
      setLoading(true)
      try {
        const [rest, menu, cats] = await Promise.all([
          restaurantService.getById(restaurantId),
          menuService.getByRestaurant(restaurantId),
          categoryService.getAll(),
        ])
        setRestaurant(rest)
        setItems(Array.isArray(menu) ? menu : [])
        setCategories(Array.isArray(cats) ? cats : (cats?.content ?? []))
      } catch (err) { toast.error(err.message || 'Failed to load menu') }
      finally { setLoading(false) }
    }
    init()
  }, [restaurantId])

  const handleToggleAvail = async (id) => {
    try {
      const updated = await menuService.toggleAvailability(id)
      setItems(l => l.map(i => i.id === id ? { ...i, isAvailable: updated.isAvailable } : i))
    } catch (err) { toast.error(err.message) }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Remove this item from the menu?')) return
    try {
      await menuService.removeItem(id)
      setItems(l => l.filter(i => i.id !== id))
      toast.success('Item removed')
    } catch (err) { toast.error(err.message) }
  }

  const handleSaved = (saved) => {
    setItems(l => {
      const exists = l.find(i => i.id === saved.id)
      return exists ? l.map(i => i.id === saved.id ? saved : i) : [...l, saved]
    })
    setModal(null)
  }

  const filtered = items.filter(i => {
    if (filter === 'veg')    return i.isVegetarian
    if (filter === 'nonveg') return !i.isVegetarian
    return true
  })

  if (loading) return <Loader fullPage />

  return (
    <div>
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-slate-500 mb-6">
        <Link to="/dashboard/restaurants" className="hover:text-primary transition-colors">My Restaurants</Link>
        <span>›</span>
        <span className="text-secondary font-semibold">{restaurant?.name || 'Menu'}</span>
      </div>

      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-black text-secondary">Menu Manager</h1>
          <p className="text-slate-500 text-sm mt-1">{items.length} items · {restaurant?.name}</p>
        </div>
        <button onClick={() => setModal('add')} className="btn-primary flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4"/>
          </svg>
          Add Item
        </button>
      </div>

      {/* Filter pills */}
      <div className="flex gap-2 mb-6">
        {[['all','All'],['veg','🌿 Veg'],['nonveg','🍖 Non-Veg']].map(([v,l]) => (
          <button key={v} onClick={() => setFilter(v)}
            className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all ${
              filter === v ? 'btn-primary' : 'btn-glass'
            }`}>
            {l}
          </button>
        ))}
      </div>

      {/* Items grid */}
      {filtered.length === 0 ? (
        <div className="glass text-center py-16 px-8">
          <div className="text-5xl mb-4">🍽️</div>
          <h3 className="text-xl font-bold text-secondary mb-2">No items yet</h3>
          <p className="text-slate-500 mb-6">Add your first food item to this restaurant's menu</p>
          <button onClick={() => setModal('add')} className="btn-primary">Add Food Item</button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map(item => (
            <div key={item.id} className={`glass p-4 transition-all ${!item.isAvailable ? 'opacity-60' : ''}`}>
              <img
                src={item.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=220&fit=crop'}
                alt={item.name} className="w-full h-36 object-cover rounded-xl mb-3" />
              <div className="flex items-start justify-between gap-2 mb-1">
                <h3 className="font-bold text-secondary text-sm leading-tight">{item.name}</h3>
                <span className={`badge shrink-0 ${item.isVegetarian ? 'badge-green' : 'badge-orange'}`}>
                  {item.isVegetarian ? '🌿 Veg' : '🍖 Non'}
                </span>
              </div>
              <p className="text-slate-500 text-xs line-clamp-2 mb-2">{item.description || '—'}</p>
              <div className="flex items-center justify-between mb-3">
                <span className="text-primary font-black text-lg">₹{item.price}</span>
                <span className={`badge text-xs ${item.isAvailable ? 'badge-blue' : 'badge-gray'}`}>
                  {item.isAvailable ? 'Available' : 'Unavailable'}
                </span>
              </div>
              <div className="flex gap-2">
                <button onClick={() => handleToggleAvail(item.id)}
                  className="btn-glass flex-1 text-xs py-1.5 px-2">
                  {item.isAvailable ? 'Mark Unavailable' : 'Mark Available'}
                </button>
                <button onClick={() => setModal(item)}
                  className="btn-glass text-xs py-1.5 px-3">✏️</button>
                <button onClick={() => handleDelete(item.id)}
                  className="text-xs py-1.5 px-3 rounded-xl text-red-500 border border-red-200/50 hover:bg-red-50/60 transition-all">
                  🗑
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {modal && (
        <ItemModal
          item={modal === 'add' ? null : modal}
          restaurantId={restaurantId}
          categories={categories}
          onClose={() => setModal(null)}
          onSaved={handleSaved}
        />
      )}
    </div>
  )
}

export default OwnerMenuManager
