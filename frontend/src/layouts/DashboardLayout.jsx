import React from 'react'
import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const DashboardLayout = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col md:flex-row">
      {/* Sidebar */}
      <aside className="w-full md:w-64 bg-secondary text-white shrink-0 p-6 flex flex-col justify-between">
        <div>
          <Link to="/" className="flex items-center space-x-2 mb-8">
            <div className="w-10 h-10 bg-primary rounded-full flex items-center justify-center">
              <span className="text-white font-bold text-xl">Z</span>
            </div>
            <span className="text-2xl font-bold text-white">Dashboard</span>
          </Link>

          <nav className="space-y-3">
            <Link
              to="/dashboard"
              className="block px-4 py-2.5 rounded-lg hover:bg-gray-700 transition-colors font-medium"
            >
              Overview
            </Link>
            <Link
              to="/dashboard/restaurants"
              className="block px-4 py-2.5 rounded-lg hover:bg-gray-700 transition-colors font-medium"
            >
              Restaurants
            </Link>
            <Link
              to="/dashboard/orders"
              className="block px-4 py-2.5 rounded-lg hover:bg-gray-700 transition-colors font-medium"
            >
              Orders
            </Link>
          </nav>
        </div>

        <div className="pt-6 border-t border-gray-700">
          <p className="text-sm text-gray-400 mb-2 truncate">
            {user?.name || user?.email}
          </p>
          <button
            onClick={handleLogout}
            className="w-full text-left text-red-400 hover:text-red-300 font-medium py-1"
          >
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 p-6 md:p-10 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}

export default DashboardLayout
