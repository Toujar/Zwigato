/**
 * AddressPicker
 *
 * Shown in Checkout. Lets the user:
 *  1. Pick a saved address from their list
 *  2. Type a new address manually
 *  3. Use Google Places Autocomplete for accurate lat/lng
 *
 * Props:
 *  - value        {string}   — current address string
 *  - onChange     {fn}       — (addressStr, { lat, lng }?) => void
 *  - savedAddresses {array}  — from AddressContext
 */
import { useRef, useState, useEffect } from 'react'

const AddressPicker = ({ value, onChange, savedAddresses = [] }) => {
  const inputRef     = useRef(null)
  const autocomplete = useRef(null)
  const [mode, setMode] = useState(savedAddresses.length > 0 ? 'saved' : 'manual')
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY
  const mapsReady = apiKey && apiKey !== 'YOUR_GOOGLE_MAPS_API_KEY_HERE'

  // Init Places Autocomplete when switching to manual mode
  useEffect(() => {
    if (mode !== 'manual' || !mapsReady) return
    if (!window.google?.maps?.places) return

    autocomplete.current = new window.google.maps.places.Autocomplete(
      inputRef.current,
      { componentRestrictions: { country: 'in' }, types: ['geocode'] }
    )
    autocomplete.current.addListener('place_changed', () => {
      const place = autocomplete.current.getPlace()
      if (place?.formatted_address) {
        const lat = place.geometry?.location?.lat()
        const lng = place.geometry?.location?.lng()
        onChange(place.formatted_address, lat && lng ? { lat, lng } : undefined)
      }
    })

    return () => {
      if (autocomplete.current) {
        window.google.maps.event.clearInstanceListeners(autocomplete.current)
      }
    }
  }, [mode, mapsReady]) // eslint-disable-line

  return (
    <div className="space-y-3">
      {/* Mode toggle */}
      {savedAddresses.length > 0 && (
        <div className="flex gap-2">
          {['saved', 'manual'].map(m => (
            <button key={m} type="button"
              onClick={() => setMode(m)}
              className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all ${
                mode === m ? 'btn-primary' : 'btn-glass'
              }`}>
              {m === 'saved' ? '📍 Saved Addresses' : '✏️ Enter Manually'}
            </button>
          ))}
        </div>
      )}

      {/* Saved address list */}
      {mode === 'saved' && (
        <div className="space-y-2">
          {savedAddresses.map(addr => (
            <label key={addr.id}
              className={`flex items-start gap-3 p-3 rounded-2xl cursor-pointer transition-all ${
                value === addr.fullAddress
                  ? 'bg-sky-50/80 border-primary'
                  : 'bg-white/40 border-sky-200/50 hover:bg-sky-50/40'
              }`}
              style={{ border: `1.5px solid ${value === addr.fullAddress ? '#0EA5E9' : 'rgba(186,230,253,0.55)'}` }}
            >
              <input type="radio" name="saved_address" className="mt-0.5 accent-primary"
                checked={value === addr.fullAddress}
                onChange={() => onChange(addr.fullAddress, addr.lat && addr.lng ? { lat: addr.lat, lng: addr.lng } : undefined)} />
              <div>
                <p className="font-semibold text-secondary text-sm flex items-center gap-1">
                  {addr.label}
                  {addr.isDefault && <span className="badge badge-blue text-xs ml-1">Default</span>}
                </p>
                <p className="text-slate-500 text-xs mt-0.5">{addr.fullAddress}</p>
              </div>
            </label>
          ))}
          <button type="button" onClick={() => setMode('manual')}
            className="text-primary text-sm font-semibold hover:underline flex items-center gap-1 mt-1">
            + Use a different address
          </button>
        </div>
      )}

      {/* Manual input with optional Places autocomplete */}
      {mode === 'manual' && (
        <div>
          <input
            ref={inputRef}
            type="text"
            value={value}
            onChange={e => onChange(e.target.value)}
            className="input-glass"
            placeholder={mapsReady
              ? 'Start typing your address…'
              : 'House No., Street, Area, City, Pincode'}
          />
          {mapsReady && (
            <p className="text-xs text-slate-400 mt-1.5 flex items-center gap-1">
              <span>🔍</span> Powered by Google Places — select a suggestion for accurate location
            </p>
          )}
        </div>
      )}
    </div>
  )
}

export default AddressPicker
