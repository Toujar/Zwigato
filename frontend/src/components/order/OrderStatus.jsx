import React from 'react'

const OrderStatus = ({ currentStatus = 'PLACED' }) => {
  const steps = [
    { key: 'PLACED', title: 'Order Placed' },
    { key: 'CONFIRMED', title: 'Confirmed' },
    { key: 'PREPARING', title: 'Preparing' },
    { key: 'OUT_FOR_DELIVERY', title: 'Out for Delivery' },
    { key: 'DELIVERED', title: 'Delivered' },
  ]

  const statusOrder = ['PLACED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED']
  const currentIdx = statusOrder.indexOf(currentStatus?.toUpperCase())

  if (currentStatus?.toUpperCase() === 'CANCELLED') {
    return (
      <div className="bg-red-50 text-red-700 p-6 rounded-xl text-center border border-red-200">
        <h3 className="text-xl font-bold mb-2">Order Cancelled</h3>
        <p className="text-sm">This order has been cancelled.</p>
      </div>
    )
  }

  return (
    <div className="py-6">
      <div className="relative flex flex-col md:flex-row justify-between items-center gap-6 md:gap-0">
        {steps.map((step, index) => {
          const isCompleted = index <= (currentIdx >= 0 ? currentIdx : 0)
          const isCurrent = index === currentIdx

          return (
            <div
              key={step.key}
              className="flex md:flex-col items-center w-full md:w-auto z-10"
            >
              <div
                className={`w-12 h-12 rounded-full flex items-center justify-center font-bold transition-colors ${
                  isCurrent
                    ? 'bg-primary text-white ring-4 ring-orange-100 animate-pulse'
                    : isCompleted
                    ? 'bg-primary text-white'
                    : 'bg-gray-200 text-gray-500'
                }`}
              >
                {isCompleted ? (
                  <svg
                    className="w-6 h-6"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2.5}
                      d="M5 13l4 4L19 7"
                    />
                  </svg>
                ) : (
                  index + 1
                )}
              </div>
              <p
                className={`ml-4 md:ml-0 md:mt-3 text-sm font-semibold text-center ${
                  isCompleted ? 'text-secondary' : 'text-gray-400'
                }`}
              >
                {step.title}
              </p>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default OrderStatus
