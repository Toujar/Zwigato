import React from 'react'

const Loader = ({ size = 'md', fullPage = false }) => {
  const sizeClasses = {
    sm: 'w-6 h-6 border-2',
    md: 'w-10 h-10 border-3',
    lg: 'w-16 h-16 border-4',
  }

  const spinner = (
    <div
      className={`${sizeClasses[size] || sizeClasses.md} border-primary border-t-transparent rounded-full animate-spin`}
      role="status"
    >
      <span className="sr-only">Loading...</span>
    </div>
  )

  if (fullPage) {
    return (
      <div className="fixed inset-0 bg-white/80 backdrop-blur-sm z-50 flex flex-col items-center justify-center space-y-3">
        {spinner}
        <p className="text-secondary font-medium animate-pulse">Loading, please wait...</p>
      </div>
    )
  }

  return <div className="flex justify-center items-center py-6">{spinner}</div>
}

export default Loader
