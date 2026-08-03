import { Routes, Route } from 'react-router-dom'
import MainLayout     from '../layouts/MainLayout'
import AuthLayout     from '../layouts/AuthLayout'
import DashboardLayout from '../layouts/DashboardLayout'
import AgentLayout    from '../layouts/AgentLayout'
import ProtectedRoute from '../components/common/ProtectedRoute'

import Home             from '../pages/Home'
import Login            from '../pages/Login'
import Register         from '../pages/Register'
import RestaurantDetails from '../pages/RestaurantDetails'
import Cart             from '../pages/Cart'
import Checkout         from '../pages/Checkout'
import OrderHistory     from '../pages/OrderHistory'
import OrderTracking    from '../pages/OrderTracking'
import OrderSuccess     from '../pages/OrderSuccess'
import Profile          from '../pages/Profile'
import NotFound         from '../pages/NotFound'

// Owner pages
import OwnerOverview     from '../pages/owner/OwnerOverview'
import OwnerRestaurants  from '../pages/owner/OwnerRestaurants'
import OwnerMenuManager  from '../pages/owner/OwnerMenuManager'
import OwnerOrders       from '../pages/owner/OwnerOrders'

// Agent pages
import AgentOverview     from '../pages/agent/AgentOverview'
import AgentAvailable    from '../pages/agent/AgentAvailable'
import AgentMyDeliveries from '../pages/agent/AgentMyDeliveries'
import AgentEarnings     from '../pages/agent/AgentEarnings'

const AppRoutes = () => (
  <Routes>

    {/* ── Auth ── */}
    <Route element={<AuthLayout />}>
      <Route path="login"    element={<Login />} />
      <Route path="register" element={<Register />} />
    </Route>

    {/* ── Customer / Public ── */}
    <Route path="/" element={<MainLayout />}>
      <Route index                element={<Home />} />
      <Route path="restaurant/:id" element={<RestaurantDetails />} />
      <Route path="cart"          element={<Cart />} />

      <Route path="checkout"
        element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
      <Route path="order-success/:id"
        element={<ProtectedRoute><OrderSuccess /></ProtectedRoute>} />
      <Route path="orders"
        element={<ProtectedRoute><OrderHistory /></ProtectedRoute>} />
      <Route path="orders/:id/tracking"
        element={<ProtectedRoute><OrderTracking /></ProtectedRoute>} />
      <Route path="profile"
        element={<ProtectedRoute><Profile /></ProtectedRoute>} />

      <Route path="*" element={<NotFound />} />
    </Route>

    {/* ── Owner / Admin Dashboard ── */}
    <Route path="dashboard"
      element={
        <ProtectedRoute roles={['ADMIN', 'RESTAURANT_OWNER']}>
          <DashboardLayout />
        </ProtectedRoute>
      }>
      <Route index                          element={<OwnerOverview />} />
      <Route path="restaurants"             element={<OwnerRestaurants />} />
      <Route path="menu/:restaurantId"      element={<OwnerMenuManager />} />
      <Route path="orders"                  element={<OwnerOrders />} />
    </Route>

    {/* ── Delivery Agent Portal ── */}
    <Route path="agent"
      element={
        <ProtectedRoute roles={['DELIVERY_AGENT']}>
          <AgentLayout />
        </ProtectedRoute>
      }>
      <Route index                element={<AgentOverview />} />
      <Route path="available"     element={<AgentAvailable />} />
      <Route path="my-deliveries" element={<AgentMyDeliveries />} />
      <Route path="earnings"      element={<AgentEarnings />} />
    </Route>

  </Routes>
)

export default AppRoutes
