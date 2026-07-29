/**
 * Category Service
 * GET /categories        → CategoryResponse[]
 * GET /categories/:id    → CategoryResponse
 */
import api from './api'

const categoryService = {
  getAll: async () => api.get('/categories'),
  getById: async (id) => api.get(`/categories/${id}`),
}

export default categoryService
