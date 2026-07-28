/**
 * Restaurant Service
 *
 * GET  /restaurants             → Page<RestaurantResponse>
 * GET  /restaurants/:id         → RestaurantResponse
 * POST /restaurants             → RestaurantResponse  (RESTAURANT_OWNER / ADMIN)
 * PUT  /restaurants/:id         → RestaurantResponse  (owner / ADMIN)
 * DELETE /restaurants/:id       → void               (owner / ADMIN)
 * PATCH /restaurants/:id/toggle-open → RestaurantResponse
 */
import api from './api'

const restaurantService = {
  /**
   * Get paginated list of open restaurants.
   * Pass `keyword` to search by name or city.
   * @param {{ page?, size?, keyword?, sortBy? }} params
   * @returns {Promise<Page<RestaurantResponse>>}  { content, totalPages, ... }
   */
  getAll: async (params = {}) => {
    return api.get('/restaurants', { params })
  },

  /**
   * Get a single restaurant by ID.
   * @param {string|number} id
   * @returns {Promise<RestaurantResponse>}
   */
  getById: async (id) => {
    return api.get(`/restaurants/${id}`)
  },

  /**
   * Register a new restaurant (RESTAURANT_OWNER / ADMIN).
   * @param {RestaurantRequest} data
   * @returns {Promise<RestaurantResponse>}
   */
  create: async (data) => {
    return api.post('/restaurants', data)
  },

  /**
   * Update a restaurant (owner / ADMIN).
   * @param {string|number} id
   * @param {RestaurantRequest} data
   * @returns {Promise<RestaurantResponse>}
   */
  update: async (id, data) => {
    return api.put(`/restaurants/${id}`, data)
  },

  /**
   * Soft-delete a restaurant (owner / ADMIN).
   * @param {string|number} id
   */
  remove: async (id) => {
    return api.delete(`/restaurants/${id}`)
  },

  /**
   * Toggle open/closed status.
   * @param {string|number} id
   * @returns {Promise<RestaurantResponse>}
   */
  toggleOpen: async (id) => {
    return api.patch(`/restaurants/${id}/toggle-open`)
  },
}

export default restaurantService
