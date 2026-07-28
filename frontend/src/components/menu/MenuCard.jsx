import { useCart } from '../../context/CartContext'
import { useToast } from '../../context/ToastContext'

const MenuCard = ({ item, restaurantId }) => {
  const { cartItems, addItem, updateQuantity } = useCart()
  const toast = useToast()

  const {
    id, name, description, price,
    image, imageUrl,
    isVeg = true, isBestSeller = false, isAvailable = true,
  } = item

  const src = imageUrl || image ||
    'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=200&fit=crop'

  const qty = cartItems.find(i => i.id === id)?.quantity ?? 0

  const handleAdd = () => {
    try {
      addItem({ id, name, price, imageUrl: src, restaurantId: restaurantId ?? item.restaurantId })
      toast.success(`${name} added to cart!`)
    } catch (err) {
      toast.error(err.message)
    }
  }

  return (
    <div className="glass-white hover:shadow-glass transition-all duration-300 flex flex-col sm:flex-row
                    items-start sm:items-center gap-4 p-4">
      {/* Image */}
      <div className="relative shrink-0">
        <img src={src} alt={name}
          className="w-24 h-24 object-cover rounded-2xl"
          loading="lazy" />
        {isBestSeller && (
          <span className="absolute -top-1.5 -left-1.5 badge badge-orange text-[10px] px-2 py-0.5">
            🔥 Best
          </span>
        )}
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-1">
          {/* Veg indicator */}
          <span title={isVeg ? 'Veg' : 'Non-Veg'}
            className={`w-3.5 h-3.5 rounded-sm border-2 flex items-center justify-center shrink-0 ${
              isVeg ? 'border-green-500' : 'border-red-500'
            }`}>
            <span className={`w-2 h-2 rounded-full ${isVeg ? 'bg-green-500' : 'bg-red-500'}`} />
          </span>
          <h4 className="font-bold text-slate-800 text-base leading-tight truncate">{name}</h4>
        </div>
        {description && (
          <p className="text-slate-400 text-sm line-clamp-2 mb-2">{description}</p>
        )}
        <p className="font-bold text-primary text-lg">₹{price}</p>
      </div>

      {/* Action */}
      <div className="shrink-0 self-end sm:self-center">
        {!isAvailable ? (
          <span className="px-5 py-2 rounded-2xl bg-slate-100 text-slate-400 text-sm font-semibold">
            Sold Out
          </span>
        ) : qty === 0 ? (
          <button onClick={handleAdd} className="btn-primary px-6 py-2 text-sm">Add</button>
        ) : (
          <div className="flex items-center rounded-2xl overflow-hidden border border-sky-200/60 bg-sky-50/60">
            <button onClick={() => updateQuantity(id, qty - 1)}
              className="px-3 py-2 text-primary font-black hover:bg-sky-100/60 transition-colors text-lg leading-none">−</button>
            <span className="px-3 font-bold text-slate-800 min-w-[28px] text-center">{qty}</span>
            <button onClick={() => updateQuantity(id, qty + 1)}
              className="px-3 py-2 text-primary font-black hover:bg-sky-100/60 transition-colors text-lg leading-none">+</button>
          </div>
        )}
      </div>
    </div>
  )
}

export default MenuCard
