import React, { useState } from 'react'
import { Outlet, Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const NAV_ITEMS = [
  {
    to: '/dashboard',
    label: 'Overview',
    roles: ['ADMIN', 'RESTAURANT_OWNER'],
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
      </svg>
    ),
  },
  {
    to: '/dashboard/restaurants',
    label: 'My Restaurants',
    roles: ['ADMIN', 'RESTAURANT_OWNER'],
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
      </svg>
    ),
  },
  {
    to: '/dashboard/orders',
    label: 'Orders',
    roles: ['ADMIN', 'RESTAURANT_OWNER'],
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
      </svg>
    ),
  },
]

const DashboardLayout = () => {
  const { user, logout } = useAuth()
  const navigate         = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const initial = user?.name?.charAt(0)?.toUpperCase() || 'A'

  const SidebarContent = () => (
    <>
      {/* Logo */}
      <Link to="/" className="flex items-center gap-3 mb-8 group">
        <div className="w-10 h-10 rounded-xl flex items-center justify-center transition-transform group-hover:scale-105"
          style={{ background: 'linear-gradient(135deg,#0EA5E9,#38BDF8)', boxShadow: '0 4px 12px rgba(14,165,233,0.40)' }}>
          <span className="text-white font-black text-lg">Z</span>
        </div>
        <span className="text-xl font-black text-white">Zwigato</span>
      </Link>

      {/* Nav */}
      <nav className="flex-1 space-y-1">
        {NAV_ITEMS.filter(item => !item.roles || item.roles.includes(user?.role)).map(({ to, label, icon }) => (
          <NavLink key={to} to={to} end={to === '/dashboard'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 rounded-2xl font-semibold text-sm transition-all duration-200 ${
                isActive
                  ? 'bg-white/15 text-white shadow-inner'
                  : 'text-sky-200 hover:bg-white/10 hover:text-white'
              }`
            }
          >
            {icon}
            {label}
          </NavLink>
        ))}
      </nav>

      {/* User info */}
      <div className="pt-6 mt-6" style={{ borderTop: '1px solid rgba(255,255,255,0.12)' }}>
        <div className="flex items-center gap-3 mb-4">
          <div className="w-9 h-9 rounded-xl flex items-center justify-center font-black text-sm"
            style={{ background: 'rgba(255,255,255,0.15)' }}>
            <span className="text-white">{initial}</span>
          </div>
          <div className="min-w-0">
            <p className="text-white text-sm font-semibold truncate">{user?.name || 'User'}</p>
            <p className="text-sky-300 text-xs truncate">{user?.role || 'ADMIN'}</p>
          </div>
        </div>
        <button onClick={handleLogout}
          className="w-full flex items-center gap-2 px-3 py-2 rounded-xl text-red-300 hover:bg-red-500/15
                     hover:text-red-200 transition-all duration-200 text-sm font-semibold">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
          Logout
        </button>
      </div>
    </>
  )

  return (
    <div className="min-h-screen flex"
      style={{
        background: '#F0F9FF',
        backgroundImage: `
          radial-gradient(ellipse at 20% 50%, rgba(14,165,233,0.12) 0%, transparent 60%),
          radial-gradient(ellipse at 80% 20%, rgba(56,189,248,0.10) 0%, transparent 55%)
        `,
      }}>

      {/* Desktop Sidebar */}
      <aside className="hidden md:flex md:w-64 shrink-0 flex-col p-6"
        style={{
          background: 'rgba(15,23,42,0.85)',
          backdropFilter: 'blur(20px)',
          borderRight: '1px solid rgba(255,255,255,0.08)',
        }}>
        <SidebarContent />
      </aside>

      {/* Mobile top bar */}
      <div className="md:hidden fixed top-0 left-0 right-0 z-40 flex items-center justify-between px-4 py-3"
        style={{
          background: 'rgba(15,23,42,0.90)',
          backdropFilter: 'blur(20px)',
          borderBottom: '1px solid rgba(255,255,255,0.08)',
        }}>
        <Link to="/" className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-xl flex items-center justify-center"
            style={{ background: 'linear-gradient(135deg,#0EA5E9,#38BDF8)' }}>
            <span className="text-white font-black text-sm">Z</span>
          </div>
          <span className="text-white font-black">Zwigato</span>
        </Link>
        <button onClick={() => setMobileOpen((v) => !v)}
          className="w-9 h-9 rounded-xl flex items-center justify-center text-sky-300"
          style={{ background: 'rgba(255,255,255,0.08)' }}>
          {mobileOpen
            ? <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
            : <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" /></svg>
          }
        </button>
      </div>

      {/* Mobile drawer */}
      {mobileOpen && (
        <div className="md:hidden fixed inset-0 z-30 pt-14">
          <div className="absolute inset-0 bg-black/40" onClick={() => setMobileOpen(false)} />
          <aside className="relative w-64 h-full flex flex-col p-6"
            style={{ background: 'rgba(15,23,42,0.95)', backdropFilter: 'blur(20px)' }}>
            <SidebarContent />
          </aside>
        </div>
      )}

      {/* Main */}
      <main className="flex-1 p-6 md:p-10 overflow-y-auto pt-20 md:pt-6">
        <Outlet />
      </main>
    </div>
  )
}

export default DashboardLayout
