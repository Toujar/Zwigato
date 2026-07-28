/**
 * Cart Service
 *
 * Matches the backend CartController (CUSTOMER role required):
 *   GET    /cart                      → CartResponse
 *   POST   /cart/items                → CartResponse  (add item)
 *   PUT    /cart/items/:cartItemId    → CartResponse  (update qty)
 *   DELETE /cart/items/:cartItemId    → CartResponse  (remove item)
 *   DELETE /cart                      → void          (clear cart)
 */
import api from './api'

const cartService = {
  /**
   * Get the current user's full cart.
   * Creates an empty cart on first call.
   * @returns {Promise<CartResponse>}
   */
  getCart: async () => {
    return api.get('/cart')
  },

  /**
   * Add a food item to the cart.
   * Increments quantity if the item already exists.
   * @param {{ foodItemId: number, quantity: number }} data
   * @returns {Promise<CartResponse>}
   */
  addItem: async (data) => {
    return api.post('/cart/items', data)
  },

  /**
   * Set a specific quantity for a cart item.
   * @param {number} cartItemId
   * @param {number} quantity  Must be ≥ 1
   * @returns {Promise<CartResponse>}
   */
  updateQuantity: async (cartItemId, quantity) => {
    return api.put(`/cart/items/${cartItemId}`, null, {
      params: { quantity },
    })
  },

  /**
   * Remove a single item row from the cart.
   * @param {number} cartItemId
   * @returns {Promise<CartResponse>}
   */
  removeItem: async (cartItemId) => {
    return api.delete(`/cart/items/${cartItemId}`)
  },

  /**
   * Remove all items and release the restaurant lock.
   */
  clearCart: async () => {
    return api.delete('/cart')
  },
}

export default cartService
