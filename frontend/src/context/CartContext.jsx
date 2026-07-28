/**
 * CartContext — client-side shopping cart.
 *
 * Persists to localStorage so the cart survives page refresh.
 * Each item stored:  { id, name, price, quantity, restaurantId, imageUrl?, ... }
 *
 * IMPORTANT: restaurantId is required on every item so that Checkout
 * can build the OrderRequest for the backend.
 *
 * Business rule: all items must be from the same restaurant.
 * addItem() rejects an item whose restaurantId differs from the
 * current cart's locked restaurant.
 */
import { createContext, useContext, useCallback } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'

const CartContext = createContext()

export const CartProvider = ({ children }) => {
  const [cartItems, setCartItems] = useLocalStorage('cartItems', [])

  // ── addItem ───────────────────────────────────────────────────────
  const addItem = useCallback((item) => {
    setCartItems((prev) => {
      // Restaurant lock: reject items from a different restaurant
      const lockedRestaurantId = prev[0]?.restaurantId
      if (lockedRestaurantId && item.restaurantId && lockedRestaurantId !== item.restaurantId) {
        // Surface as an error — callers should catch this
        throw new Error(
          `Your cart contains items from another restaurant. ` +
          `Clear your cart before adding from a new restaurant.`
        )
      }

      const existing = prev.find((i) => i.id === item.id)
      if (existing) {
        return prev.map((i) =>
          i.id === item.id ? { ...i, quantity: i.quantity + 1 } : i
        )
      }
      return [...prev, { ...item, quantity: 1 }]
    })
  }, [setCartItems])

  // ── removeItem ────────────────────────────────────────────────────
  const removeItem = useCallback((itemId) => {
    setCartItems((prev) => prev.filter((i) => i.id !== itemId))
  }, [setCartItems])

  // ── updateQuantity ────────────────────────────────────────────────
  const updateQuantity = useCallback((itemId, qty) => {
    if (qty <= 0) { removeItem(itemId); return }
    setCartItems((prev) =>
      prev.map((i) => i.id === itemId ? { ...i, quantity: qty } : i)
    )
  }, [setCartItems, removeItem])

  // ── clearCart ─────────────────────────────────────────────────────
  const clearCart = useCallback(() => setCartItems([]), [setCartItems])

  // ── derived values ────────────────────────────────────────────────
  const totalItems = cartItems.reduce((sum, i) => sum + i.quantity, 0)
  const totalPrice = cartItems.reduce((sum, i) => sum + i.price * i.quantity, 0)
  const restaurantId = cartItems[0]?.restaurantId ?? null

  return (
    <CartContext.Provider value={{
      cartItems,
      totalItems,
      totalPrice,
      restaurantId,
      addItem,
      removeItem,
      updateQuantity,
      clearCart,
    }}>
      {children}
    </CartContext.Provider>
  )
}

export const useCart = () => {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within CartProvider')
  return ctx
}
