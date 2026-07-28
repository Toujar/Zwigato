/**
 * Payment Service
 *
 * Matches the backend PaymentController:
 *   POST /payments/initiate              → PaymentResponse  (CUSTOMER)
 *   POST /payments/confirm/:transactionId→ PaymentResponse  (ADMIN/gateway)
 *   GET  /payments/order/:orderId        → PaymentResponse  (CUSTOMER/ADMIN)
 */
import api from './api'

const paymentService = {
  /**
   * Initiate a payment for an order (CUSTOMER only).
   * Creates a PENDING payment record.
   * @param {{ orderId: number, paymentMethod: string }} data
   * @returns {Promise<PaymentResponse>}
   */
  initiate: async (data) => {
    return api.post('/payments/initiate', data)
  },

  /**
   * Confirm a payment via gateway transaction ID (ADMIN / webhook).
   * @param {string} transactionId
   * @returns {Promise<PaymentResponse>}
   */
  confirm: async (transactionId) => {
    return api.post(`/payments/confirm/${transactionId}`)
  },

  /**
   * Get the payment record for a specific order.
   * @param {string|number} orderId
   * @returns {Promise<PaymentResponse>}
   */
  getByOrder: async (orderId) => {
    return api.get(`/payments/order/${orderId}`)
  },
}

export default paymentService
