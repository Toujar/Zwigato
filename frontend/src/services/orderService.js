// placeOrder(data)       -> POST /api/orders
// getOrders()            -> GET /api/orders
// getOrderById(id)       -> GET /api/orders/:id
// updateStatus(id, status) -> PUT /api/orders/:id/status

import api from './api'

const orderService = {
  placeOrder: async (data) => {
    const response = await api.post('/orders', data)
    return response.data
  },

  getOrders: async () => {
    const response = await api.get('/orders')
    return response.data
  },

  getOrderById: async (id) => {
    const response = await api.get(`/orders/${id}`)
    return response.data
  },

  updateStatus: async (id, status) => {
    const response = await api.put(`/orders/${id}/status`, { status })
    return response.data
  },
}

export default orderService
