// initiate(data)         -> POST /api/payments/initiate
// confirm(txnId)         -> POST /api/payments/confirm
// getByOrder(orderId)    -> GET /api/payments/:orderId

import api from './api'

const paymentService = {
  initiate: async (data) => {
    const response = await api.post('/payments/initiate', data)
    return response.data
  },

  confirm: async (txnId) => {
    const response = await api.post('/payments/confirm', { transactionId: txnId })
    return response.data
  },

  getByOrder: async (orderId) => {
    const response = await api.get(`/payments/${orderId}`)
    return response.data
  },
}

export default paymentService
