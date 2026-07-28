const CartItem = ({ item, onUpdateQuantity, onRemove }) => {
  const { id, name, price, quantity, image, imageUrl } = item
  const src = imageUrl || image ||
    'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=120&h=120&fit=crop'

  return (
    <div className="glass-white flex items-center gap-4 p-4 hover:shadow-glass transition-all duration-200">
      <img src={src} alt={name}
        className="w-20 h-20 object-cover rounded-2xl shrink-0"
        loading="lazy" />

      <div className="flex-1 min-w-0">
        <h3 className="font-bold text-slate-800 text-base truncate">{name}</h3>
        <p className="text-primary font-bold mt-0.5">₹{price}</p>
        <p className="text-slate-400 text-xs mt-0.5">
          Subtotal: <span className="text-slate-600 font-semibold">₹{(price * quantity).toFixed(2)}</span>
        </p>
      </div>

      <div className="flex items-center gap-3 shrink-0">
        {/* Stepper */}
        <div className="flex items-center rounded-2xl overflow-hidden border border-sky-200/60 bg-sky-50/60">
          <button onClick={() => onUpdateQuantity(id, quantity - 1)}
            className="px-3 py-2 text-primary font-black hover:bg-sky-100 transition-colors text-lg leading-none">−</button>
          <span className="px-3 font-bold text-slate-800 min-w-[28px] text-center">{quantity}</span>
          <button onClick={() => onUpdateQuantity(id, quantity + 1)}
            className="px-3 py-2 text-primary font-black hover:bg-sky-100 transition-colors text-lg leading-none">+</button>
        </div>

        {/* Delete */}
        <button onClick={() => onRemove(id)}
          className="p-2 rounded-xl text-slate-400 hover:text-red-500 hover:bg-red-50/60 transition-all duration-200">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>
      </div>
    </div>
  )
}

export default CartItem
