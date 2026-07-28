/**
 * Menu / Food Item Service
 *
 * Matches the backend FoodItemController:
 *   GET  /food-items/restaurant/:restaurantId  → List<FoodItemResponse>
 *   GET  /food-items/:id                       → FoodItemResponse
 *   POST /food-items                           → FoodItemResponse (owner/ADMIN)
 *   PUT  /food-items/:id                       → FoodItemResponse (owner/ADMIN)
 *   DELETE /food-items/:id                     → void
 *   PATCH /food-items/:id/toggle-availability  → FoodItemResponse
 */
import api from './api'

const menuService = {
  /**
   * Fetch the public menu for a restaurant (available items only).
   * @param {string|number} restaurantId
   * @returns {Promise<FoodItemResponse[]>}
   */
  getByRestaurant: async (restaurantId) => {
    return api.get(`/food-items/restaurant/${restaurantId}`)
  },

  /**
   * Get a single food item by ID.
   * @param {string|number} id
   * @returns {Promise<FoodItemResponse>}
   */
  getById: async (id) => {
    return api.get(`/food-items/${id}`)
  },

  /**
   * Add a new food item (RESTAURANT_OWNER / ADMIN).
   * @param {FoodItemRequest} data  { restaurantId, categoryId, name, price, ... }
   * @returns {Promise<FoodItemResponse>}
   */
  addItem: async (data) => {
    return api.post('/food-items', data)
  },

  /**
   * Update a food item (RESTAURANT_OWNER / ADMIN).
   * @param {string|number} id
   * @param {FoodItemRequest} data
   * @returns {Promise<FoodItemResponse>}
   */
  updateItem: async (id, data) => {
    return api.put(`/food-items/${id}`, data)
  },

  /**
   * Delete a food item (RESTAURANT_OWNER / ADMIN).
   * @param {string|number} id
   */
  removeItem: async (id) => {
    return api.delete(`/food-items/${id}`)
  },

  /**
   * Toggle availability flag (RESTAURANT_OWNER / ADMIN).
   * @param {string|number} id
   * @returns {Promise<FoodItemResponse>}
   */
  toggleAvailability: async (id) => {
    return api.patch(`/food-items/${id}/toggle-availability`)
  },
}

export default menuService
