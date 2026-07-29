import React from 'react'
import { Outlet, Link } from 'react-router-dom'

const AuthLayout = () => {
  return (
    <div className="min-h-screen flex flex-col justify-center py-12 sm:px-6 lg:px-8"
      style={{
        background: '#F0F9FF',
        backgroundImage: `
          radial-gradient(ellipse at 20% 50%, rgba(14,165,233,0.18) 0%, transparent 60%),
          radial-gradient(ellipse at 80% 20%, rgba(56,189,248,0.15) 0%, transparent 55%),
          radial-gradient(ellipse at 50% 80%, rgba(125,211,252,0.12) 0%, transparent 55%)
        `,
        backgroundAttachment: 'fixed',
      }}
    >
      {/* Logo */}
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center mb-6">
        <Link to="/" className="inline-flex items-center gap-3 group">
          <div className="w-14 h-14 rounded-2xl flex items-center justify-center shadow-glass
                          transition-transform duration-300 group-hover:scale-105"
            style={{
              background: 'linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%)',
              boxShadow: '0 4px 16px rgba(14,165,233,0.40), inset 0 1px 0 rgba(255,255,255,0.30)',
            }}>
            <span className="text-white font-black text-2xl">Z</span>
          </div>
          <span className="text-3xl font-black text-gradient">Zwigato</span>
        </Link>
        <p className="mt-2 text-slate-500 text-sm">Your favourite food, delivered fast</p>
      </div>

      {/* Card */}
      <div className="sm:mx-auto sm:w-full sm:max-w-md px-4">
        <div className="glass-elevated py-8 px-6 sm:px-10 animate-scale-in">
          <Outlet />
        </div>
      </div>
    </div>
  )
}

export default AuthLayout
