/**
 * Order Service
 *
 * Matches the backend OrderController:
 *   POST   /orders                    → OrderResponse  (CUSTOMER)
 *   GET    /orders                    → Page<OrderResponse>
 *   GET    /orders/:id                → OrderResponse
 *   PATCH  /orders/:id/status         → OrderResponse  (RESTAURANT_OWNER/DELIVERY_AGENT/ADMIN)
 *   PATCH  /orders/:id/cancel         → OrderResponse  (CUSTOMER)
 */
import api from './api'

const orderService = {
  /**
   * Place a new order.
   * @param {{
   *   restaurantId: number,
   *   deliveryAddress: string,
   *   items: Array<{ foodItemId: number, quantity: number }>,
   *   specialInstructions?: string
   * }} data
   * @returns {Promise<OrderResponse>}
   */
  placeOrder: async (data) => {
    return api.post('/orders', data)
  },

  /**
   * Get the current user's paginated order history.
   * @param {{ page?: number, size?: number }} params
   * @returns {Promise<Page<OrderResponse>>}  { content, totalPages, ... }
   */
  getOrders: async (params = { page: 0, size: 10 }) => {
    return api.get('/orders', { params })
  },

  /**
   * Get full details of a single order including items and payment.
   * @param {string|number} id
   * @returns {Promise<OrderResponse>}
   */
  getOrderById: async (id) => {
    return api.get(`/orders/${id}`)
  },

  /**
   * Advance an order's status (RESTAURANT_OWNER / DELIVERY_AGENT / ADMIN).
   * @param {string|number} id
   * @param {string} status  e.g. 'CONFIRMED', 'PREPARING', 'DELIVERED'
   * @returns {Promise<OrderResponse>}
   */
  updateStatus: async (id, status) => {
    return api.patch(`/orders/${id}/status`, null, { params: { status } })
  },

  /**
   * Cancel an order (CUSTOMER — only when PLACED or CONFIRMED).
   * @param {string|number} id
   * @returns {Promise<OrderResponse>}
   */
  cancelOrder: async (id) => {
    return api.patch(`/orders/${id}/cancel`)
  },
}

export default orderService
