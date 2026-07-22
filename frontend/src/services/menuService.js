// getByRestaurant(restaurantId) -> GET /api/restaurants/:id/menu
// addItem(restaurantId, data)   -> POST /api/restaurants/:id/menu
// updateItem(itemId, data)      -> PUT /api/menu/:itemId
// removeItem(itemId)            -> DELETE /api/menu/:itemId

import api from './api'

const menuService = {
  getByRestaurant: async (restaurantId) => {
    const response = await api.get(`/restaurants/${restaurantId}/menu`)
    return response.data
  },

  addItem: async (restaurantId, data) => {
    const response = await api.post(`/restaurants/${restaurantId}/menu`, data)
    return response.data
  },

  updateItem: async (itemId, data) => {
    const response = await api.put(`/menu/${itemId}`, data)
    return response.data
  },

  removeItem: async (itemId) => {
    const response = await api.delete(`/menu/${itemId}`)
    return response.data
  },
}

export default menuService
