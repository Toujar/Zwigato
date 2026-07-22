// getCart()              -> GET /api/cart
// addItem(data)          -> POST /api/cart/add
// updateItem(data)       -> PUT /api/cart/update
// removeItem(itemId)     -> DELETE /api/cart/remove/:itemId
// clearCart()            -> DELETE /api/cart/clear

import api from './api'

const cartService = {
  getCart: async () => {
    const response = await api.get('/cart')
    return response.data
  },

  addItem: async (data) => {
    const response = await api.post('/cart/add', data)
    return response.data
  },

  updateItem: async (data) => {
    const response = await api.put('/cart/update', data)
    return response.data
  },

  removeItem: async (itemId) => {
    const response = await api.delete(`/cart/remove/${itemId}`)
    return response.data
  },

  clearCart: async () => {
    const response = await api.delete('/cart/clear')
    return response.data
  },
}

export default cartService
