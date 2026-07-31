/**
 * AddressContext
 *
 * Persists a list of saved delivery addresses in localStorage.
 * Each address has: { id, label, fullAddress, lat, lng, isDefault }
 *
 * Used by:
 *  - Profile page   → manage saved addresses
 *  - Checkout page  → pick a saved address or enter a new one
 *  - OrderTracking  → show delivery pin on the map
 */
import { createContext, useContext, useCallback } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'

const AddressContext = createContext()

export const AddressProvider = ({ children }) => {
  const [addresses, setAddresses] = useLocalStorage('saved_addresses', [])

  const addAddress = useCallback((addr) => {
    const newAddr = { ...addr, id: Date.now() }
    setAddresses(prev => {
      // If first address, make it default
      if (prev.length === 0) newAddr.isDefault = true
      return [...prev, newAddr]
    })
    return newAddr
  }, [setAddresses])

  const updateAddress = useCallback((id, data) => {
    setAddresses(prev => prev.map(a => a.id === id ? { ...a, ...data } : a))
  }, [setAddresses])

  const removeAddress = useCallback((id) => {
    setAddresses(prev => {
      const filtered = prev.filter(a => a.id !== id)
      // If we removed the default, promote the first remaining one
      if (filtered.length > 0 && !filtered.some(a => a.isDefault)) {
        filtered[0].isDefault = true
      }
      return filtered
    })
  }, [setAddresses])

  const setDefault = useCallback((id) => {
    setAddresses(prev => prev.map(a => ({ ...a, isDefault: a.id === id })))
  }, [setAddresses])

  const defaultAddress = addresses.find(a => a.isDefault) || addresses[0] || null

  return (
    <AddressContext.Provider value={{
      addresses,
      addAddress,
      updateAddress,
      removeAddress,
      setDefault,
      defaultAddress,
    }}>
      {children}
    </AddressContext.Provider>
  )
}

export const useAddress = () => {
  const ctx = useContext(AddressContext)
  if (!ctx) throw new Error('useAddress must be used within AddressProvider')
  return ctx
}
