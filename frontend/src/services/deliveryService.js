/**
 * Delivery Service
 * Matches DeliveryController endpoints at /api/delivery
 */
import api from './api'

const deliveryService = {
  /** Orders ready for pickup with no agent assigned yet */
  getAvailableOrders: () => api.get('/delivery/available'),

  /** My assigned deliveries (paginated) */
  getMyDeliveries: (params = { page: 0, size: 20 }) =>
    api.get('/delivery/my-deliveries', { params }),

  /** Self-assign to an available order */
  acceptDelivery: (orderId) => api.patch(`/delivery/${orderId}/accept`),

  /** Mark an assigned order as delivered */
  markDelivered: (orderId) => api.patch(`/delivery/${orderId}/delivered`),
}

export default deliveryService
