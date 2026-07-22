// getAll(params)     -> GET /api/restaurants
// getById(id)        -> GET /api/restaurants/:id
// create(data)       -> POST /api/restaurants
// update(id, data)   -> PUT /api/restaurants/:id
// remove(id)         -> DELETE /api/restaurants/:id

import api from './api'

const restaurantService = {
  getAll: async (params = {}) => {
    const response = await api.get('/restaurants', { params })
    return response.data
  },

  getById: async (id) => {
    const response = await api.get(`/restaurants/${id}`)
    return response.data
  },

  create: async (data) => {
    const response = await api.post('/restaurants', data)
    return response.data
  },

  update: async (id, data) => {
    const response = await api.put(`/restaurants/${id}`, data)
    return response.data
  },

  remove: async (id) => {
    const response = await api.delete(`/restaurants/${id}`)
    return response.data
  },
}

export default restaurantService
