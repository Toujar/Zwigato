import { Routes, Route } from 'react-router-dom'
import MainLayout from '../layouts/MainLayout'
import AuthLayout from '../layouts/AuthLayout'
import DashboardLayout from '../layouts/DashboardLayout'
import ProtectedRoute from '../components/common/ProtectedRoute'

import Home from '../pages/Home'
import Login from '../pages/Login'
import Register from '../pages/Register'
import RestaurantDetails from '../pages/RestaurantDetails'
import Cart from '../pages/Cart'
import Checkout from '../pages/Checkout'
import OrderHistory from '../pages/OrderHistory'
import OrderTracking from '../pages/OrderTracking'
import OrderSuccess from '../pages/OrderSuccess'
import Profile from '../pages/Profile'
import NotFound from '../pages/NotFound'

const AppRoutes = () => {
  return (
    <Routes>
      {/* Auth Routes */}
      <Route element={<AuthLayout />}>
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
      </Route>

      {/* Main Public & Customer Routes */}
      <Route path="/" element={<MainLayout />}>
        <Route index element={<Home />} />
        <Route path="restaurant/:id" element={<RestaurantDetails />} />
        <Route path="cart" element={<Cart />} />

        {/* Protected Customer Routes */}
        <Route
          path="checkout"
          element={
            <ProtectedRoute>
              <Checkout />
            </ProtectedRoute>
          }
        />
        <Route
          path="order-success/:id"
          element={
            <ProtectedRoute>
              <OrderSuccess />
            </ProtectedRoute>
          }
        />
        <Route
          path="orders"
          element={
            <ProtectedRoute>
              <OrderHistory />
            </ProtectedRoute>
          }
        />
        <Route
          path="orders/:id/tracking"
          element={
            <ProtectedRoute>
              <OrderTracking />
            </ProtectedRoute>
          }
        />
        <Route
          path="profile"
          element={
            <ProtectedRoute>
              <Profile />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<NotFound />} />
      </Route>

      {/* Admin / Owner Dashboard Shell */}
      <Route
        path="dashboard"
        element={
          <ProtectedRoute roles={['ADMIN', 'RESTAURANT_OWNER']}>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<div className="text-2xl font-bold text-secondary">Dashboard Overview</div>} />
        <Route path="restaurants" element={<div className="text-2xl font-bold text-secondary">Manage Restaurants</div>} />
        <Route path="orders" element={<div className="text-2xl font-bold text-secondary">Manage Orders</div>} />
      </Route>
    </Routes>
  )
}

export default AppRoutes
