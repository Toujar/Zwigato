import { useState, useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useCart } from '../../context/CartContext'

const Navbar = () => {
  const { user, isAuthenticated, logout } = useAuth()
  const { totalItems } = useCart()
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const fn = () => setScrolled(window.scrollY > 8)
    window.addEventListener('scroll', fn, { passive: true })
    return () => window.removeEventListener('scroll', fn)
  }, [])

  useEffect(() => setMobileOpen(false), [location.pathname])

  const handleLogout = () => { logout(); navigate('/') }

  const initials = user?.name
    ? user.name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
    : user?.email?.[0]?.toUpperCase() ?? 'U'

  const navLink = (path, label) => {
    const active = location.pathname === path
    return (
      <Link key={path} to={path}
        className={`px-4 py-2 rounded-xl text-sm font-semibold transition-all duration-200 ${
          active
            ? 'bg-primary/12 text-primary border border-primary/20'
            : 'text-slate-600 hover:text-primary hover:bg-sky-50/60'
        }`}>
        {label}
      </Link>
    )
  }

  return (
    <>
      <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
        scrolled
          ? 'backdrop-blur-xl border-b shadow-glass'
          : 'backdrop-blur-md border-b'
      }`}
        style={{
          background: scrolled
            ? 'rgba(240,249,255,0.82)'
            : 'rgba(224,242,254,0.55)',
          borderColor: 'rgba(186,230,253,0.55)',
          WebkitBackdropFilter: 'blur(20px) saturate(180%)',
        }}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">

            {/* Logo */}
            <Link to="/" className="flex items-center gap-2.5 group">
              <div className="w-9 h-9 rounded-2xl flex items-center justify-center shadow-float
                              transition-transform duration-300 group-hover:scale-110"
                style={{ background: 'linear-gradient(135deg,#0EA5E9,#38BDF8)' }}>
                <span className="text-white font-black text-base">Z</span>
              </div>
              <span className="text-xl font-black text-gradient hidden sm:block">Zwigato</span>
            </Link>

            {/* Desktop links */}
            <div className="hidden md:flex items-center gap-1">
              {navLink('/', 'Home')}
              {isAuthenticated && navLink('/orders', 'Orders')}
              {isAuthenticated && navLink('/profile', 'Profile')}
            </div>

            {/* Right side */}
            <div className="flex items-center gap-2">

              {/* Cart icon */}
              <Link to="/cart"
                className="relative p-2.5 rounded-xl glass-subtle hover:bg-sky-100/60 transition-all duration-200">
                <svg className="w-5 h-5 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                {totalItems > 0 && (
                  <span className="absolute -top-1 -right-1 w-5 h-5 rounded-full text-white text-[10px]
                                   font-black flex items-center justify-center animate-scale-in"
                    style={{ background: 'linear-gradient(135deg,#0EA5E9,#0284C7)' }}>
                    {totalItems > 9 ? '9+' : totalItems}
                  </span>
                )}
              </Link>

              {/* Auth — desktop */}
              {isAuthenticated ? (
                <div className="hidden md:flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold"
                    style={{ background: 'linear-gradient(135deg,#0EA5E9,#38BDF8)' }}>
                    {initials}
                  </div>
                  <span className="text-slate-600 text-sm font-medium hidden lg:block">
                    {user?.name?.split(' ')[0] || 'Hi!'}
                  </span>
                  <button onClick={handleLogout}
                    className="px-3 py-1.5 rounded-xl text-sm font-semibold text-slate-500
                               hover:text-red-500 hover:bg-red-50/60 transition-all duration-200">
                    Logout
                  </button>
                </div>
              ) : (
                <div className="hidden md:flex items-center gap-2">
                  <Link to="/login"
                    className="px-4 py-2 rounded-xl text-sm font-semibold text-slate-600
                               hover:text-primary hover:bg-sky-50/60 transition-all">
                    Login
                  </Link>
                  <Link to="/register" className="btn-primary px-4 py-2 text-sm">Sign Up</Link>
                </div>
              )}

              {/* Hamburger */}
              <button onClick={() => setMobileOpen(!mobileOpen)}
                className="md:hidden p-2.5 rounded-xl glass-subtle hover:bg-sky-100/60 transition-all">
                <div className="w-5 flex flex-col gap-1.5">
                  <span className={`block h-0.5 bg-slate-700 rounded-full transition-all duration-300
                    ${mobileOpen ? 'rotate-45 translate-y-2' : ''}`} />
                  <span className={`block h-0.5 bg-slate-700 rounded-full transition-all duration-300
                    ${mobileOpen ? 'opacity-0 w-0' : ''}`} />
                  <span className={`block h-0.5 bg-slate-700 rounded-full transition-all duration-300
                    ${mobileOpen ? '-rotate-45 -translate-y-2' : ''}`} />
                </div>
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Mobile menu */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 md:hidden" onClick={() => setMobileOpen(false)}>
          <div className="absolute inset-0 bg-sky-900/15 backdrop-blur-sm" />
          <div className="absolute top-[72px] left-3 right-3 glass-elevated p-4 animate-scale-in"
            onClick={e => e.stopPropagation()}>
            <div className="space-y-1">
              {[['/', '🏠', 'Home'], ['/orders','📋','Orders'], ['/profile','👤','Profile']].map(([path, icon, label]) =>
                (!isAuthenticated && path !== '/') ? null : (
                  <Link key={path} to={path}
                    className={`flex items-center gap-3 px-4 py-3 rounded-2xl transition-all font-semibold ${
                      location.pathname === path
                        ? 'bg-sky-100/80 text-primary'
                        : 'text-slate-700 hover:bg-sky-50/60'
                    }`}>
                    <span>{icon}</span>{label}
                  </Link>
                )
              )}
            </div>
            <div className="glass-divider my-3" />
            {isAuthenticated ? (
              <button onClick={handleLogout}
                className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl
                           text-red-500 hover:bg-red-50/60 transition-all font-semibold">
                🚪 Logout
              </button>
            ) : (
              <div className="flex gap-3 pt-1">
                <Link to="/login" className="flex-1 btn-glass py-3 text-center text-sm rounded-2xl">Login</Link>
                <Link to="/register" className="flex-1 btn-primary py-3 text-center text-sm rounded-2xl">Sign Up</Link>
              </div>
            )}
          </div>
        </div>
      )}
      <div className="h-16" />
    </>
  )
}

export default Navbar
