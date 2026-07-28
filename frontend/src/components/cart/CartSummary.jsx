import { Link } from 'react-router-dom'

const Row = ({ label, value, bold }) => (
  <div className={`flex justify-between items-center ${bold ? 'font-bold text-slate-800 text-base' : 'text-slate-500 text-sm'}`}>
    <span>{label}</span>
    <span className={bold ? 'text-primary' : ''}>{value}</span>
  </div>
)

const CartSummary = ({
  totalItems = 0,
  totalPrice = 0,
  deliveryFee = 40,
  serviceFee = 10,
  isCheckout = false,
}) => {
  const hasFees = totalItems > 0
  const subtotal = totalPrice
  const dFee     = hasFees ? deliveryFee : 0
  const sFee     = hasFees ? serviceFee : 0
  const total    = subtotal + dFee + sFee

  return (
    <div className="glass-white p-6 sticky top-24">
      <h3 className="text-lg font-bold text-slate-800 mb-5 pb-4"
        style={{ borderBottom: '1px solid rgba(186,230,253,0.50)' }}>
        Order Summary
      </h3>

      <div className="space-y-3 mb-5">
        <Row label={`Items (${totalItems})`} value={`₹${subtotal.toFixed(2)}`} />
        <Row label="Delivery fee"             value={`₹${dFee}`} />
        <Row label="Platform fee"             value={`₹${sFee}`} />
        <div className="glass-divider" />
        <Row label="Total" value={`₹${total.toFixed(2)}`} bold />
      </div>

      {!isCheckout && (
        <Link to="/checkout"
          className={`btn-primary w-full block text-center ${totalItems === 0 ? 'pointer-events-none opacity-50' : ''}`}>
          Proceed to Checkout →
        </Link>
      )}

      {/* Savings badge */}
      {hasFees && (
        <p className="text-center text-xs text-green-600 mt-3 font-medium">
          🎉 You're saving on platform fee today!
        </p>
      )}
    </div>
  )
}

export default CartSummary
