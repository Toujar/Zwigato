import { Link } from 'react-router-dom'

const STATUS_STYLE = {
  DELIVERED:       'badge-green',
  CANCELLED:       'badge-red',
  OUT_FOR_DELIVERY:'badge-blue',
  PREPARING:       'badge-orange',
  CONFIRMED:       'badge-blue',
  PLACED:          'badge-gray',
}

const OrderCard = ({ order }) => {
  const {
    id, placedAt, items = [], orderItems = [],
    totalAmount = 0, status = 'PLACED', restaurantName,
  } = order

  const allItems = items.length ? items : orderItems
  const dateStr  = placedAt ? new Date(placedAt).toLocaleDateString('en-IN',
    { day:'numeric', month:'short', year:'numeric' }) : 'Recent'
  const itemText = allItems.slice(0, 2)
    .map(i => typeof i === 'string' ? i : i.foodItemName || i.name || 'Item')
    .join(', ') + (allItems.length > 2 ? ` +${allItems.length - 2} more` : '')

  return (
    <div className="glass-white hover:shadow-glass transition-all duration-200 p-5">
      <div className="flex flex-col sm:flex-row justify-between gap-3 mb-3">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h3 className="font-bold text-slate-800">Order #{String(id).slice(-6)}</h3>
            <span className={`badge ${STATUS_STYLE[status] || 'badge-gray'}`}>
              {status.replace(/_/g, ' ')}
            </span>
          </div>
          {restaurantName && <p className="text-primary text-sm font-medium">{restaurantName}</p>}
          <p className="text-slate-400 text-xs mt-0.5">{dateStr}</p>
        </div>
        <div className="text-right">
          <p className="font-black text-xl text-primary">₹{Number(totalAmount).toFixed(2)}</p>
          <p className="text-slate-400 text-xs">{allItems.length} item{allItems.length !== 1 ? 's' : ''}</p>
        </div>
      </div>

      <p className="text-slate-500 text-sm line-clamp-1 mb-4">{itemText || 'No items'}</p>

      <div className="flex gap-3 flex-wrap">
        <Link to={`/orders/${id}/tracking`}
          className="btn-primary px-4 py-2 text-sm">
          Track Order
        </Link>
        {(status === 'PLACED' || status === 'CONFIRMED') && (
          <span className="badge badge-orange self-center">Active</span>
        )}
      </div>
    </div>
  )
}

export default OrderCard
