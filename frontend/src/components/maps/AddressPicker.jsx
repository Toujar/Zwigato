/**
 * AddressPicker
 *
 * Address input with Nominatim (OpenStreetMap) autocomplete suggestions.
 * Completely free — no API key, no billing.
 *
 * Props:
 *   value          {string}  — current address value
 *   onChange       {fn}      — (addressStr, {lat,lng}?) => void
 *   savedAddresses {array}   — from AddressContext
 */
import { useRef, useState, useEffect } from 'react'

// Debounce helper — prevents hammering Nominatim on every keystroke
function useDebounce(value, delay) {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(t)
  }, [value, delay])
  return debounced
}

const AddressPicker = ({ value, onChange, savedAddresses = [] }) => {
  const [mode, setMode]             = useState(savedAddresses.length > 0 ? 'saved' : 'manual')
  const [inputText, setInputText]   = useState(value || '')
  const [suggestions, setSuggestions] = useState([])
  const [showSugg, setShowSugg]     = useState(false)
  const [loading, setLoading]       = useState(false)
  const debouncedInput              = useDebounce(inputText, 450) // 450ms after typing stops
  const wrapperRef                  = useRef(null)

  // Fetch Nominatim suggestions when debounced input changes
  useEffect(() => {
    if (!debouncedInput || debouncedInput.length < 4 || mode !== 'manual') {
      setSuggestions([])
      return
    }

    let cancelled = false
    setLoading(true)

    fetch(
      `https://nominatim.openstreetmap.org/search?` +
      `q=${encodeURIComponent(debouncedInput)}&format=json&limit=5&countrycodes=in&addressdetails=1`,
      { headers: { 'User-Agent': 'Zwigato-FoodDelivery/1.0', 'Accept-Language': 'en' } }
    )
      .then(r => r.json())
      .then(data => {
        if (!cancelled) {
          setSuggestions(data || [])
          setShowSugg(true)
          setLoading(false)
        }
      })
      .catch(() => { if (!cancelled) setLoading(false) })

    return () => { cancelled = true }
  }, [debouncedInput, mode])

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        setShowSugg(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const handleSelect = (place) => {
    const address = place.display_name
    const coords  = { lat: parseFloat(place.lat), lng: parseFloat(place.lon) }
    setInputText(address)
    setSuggestions([])
    setShowSugg(false)
    onChange(address, coords)
  }

  const handleManualInput = (e) => {
    const text = e.target.value
    setInputText(text)
    onChange(text) // pass raw text without coords until suggestion is picked
  }

  // Sync external value into local state
  useEffect(() => {
    if (value !== inputText) setInputText(value || '')
  }, [value]) // eslint-disable-line

  return (
    <div className="space-y-3">

      {/* Mode toggle — only shown if there are saved addresses */}
      {savedAddresses.length > 0 && (
        <div className="flex gap-2">
          {['saved', 'manual'].map(m => (
            <button key={m} type="button"
              onClick={() => { setMode(m); setShowSugg(false) }}
              className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-all ${
                mode === m ? 'btn-primary' : 'btn-glass'
              }`}>
              {m === 'saved' ? '📍 Saved' : '✏️ New Address'}
            </button>
          ))}
        </div>
      )}

      {/* Saved address list */}
      {mode === 'saved' && (
        <div className="space-y-2">
          {savedAddresses.map(addr => (
            <label key={addr.id}
              style={{ border: `1.5px solid ${value === addr.fullAddress ? '#0EA5E9' : 'rgba(186,230,253,0.55)'}` }}
              className={`flex items-start gap-3 p-3 rounded-2xl cursor-pointer transition-all ${
                value === addr.fullAddress ? 'bg-sky-50/80' : 'bg-white/40 hover:bg-sky-50/40'
              }`}>
              <input type="radio" name="saved_address" className="mt-0.5 accent-primary"
                checked={value === addr.fullAddress}
                onChange={() => onChange(
                  addr.fullAddress,
                  addr.lat && addr.lng ? { lat: addr.lat, lng: addr.lng } : undefined
                )} />
              <div>
                <p className="font-semibold text-secondary text-sm flex items-center gap-1">
                  {addr.label}
                  {addr.isDefault && <span className="badge badge-blue text-xs ml-1">Default</span>}
                  {addr.lat && <span className="badge badge-green text-xs ml-1">📍</span>}
                </p>
                <p className="text-slate-500 text-xs mt-0.5">{addr.fullAddress}</p>
              </div>
            </label>
          ))}
          <button type="button" onClick={() => setMode('manual')}
            className="text-primary text-sm font-semibold hover:underline mt-1">
            + Use a different address
          </button>
        </div>
      )}

      {/* Manual input with Nominatim suggestions */}
      {mode === 'manual' && (
        <div ref={wrapperRef} className="relative">
          <div className="relative">
            <input
              type="text"
              value={inputText}
              onChange={handleManualInput}
              onFocus={() => suggestions.length > 0 && setShowSugg(true)}
              className="input-glass pr-8"
              placeholder="Start typing your address…"
              autoComplete="off"
            />
            {loading && (
              <div className="absolute right-3 top-1/2 -translate-y-1/2">
                <div className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin" />
              </div>
            )}
          </div>

          <p className="text-xs text-slate-400 mt-1.5 flex items-center gap-1">
            🔍 Powered by{' '}
            <a href="https://www.openstreetmap.org" target="_blank" rel="noreferrer"
              className="text-primary hover:underline">OpenStreetMap</a>
            {' '}— type 4+ chars to see suggestions
          </p>

          {/* Dropdown suggestions */}
          {showSugg && suggestions.length > 0 && (
            <ul className="absolute z-50 w-full mt-1 glass-elevated rounded-2xl overflow-hidden shadow-glass-lg max-h-60 overflow-y-auto">
              {suggestions.map((place) => (
                <li key={place.place_id}>
                  <button
                    type="button"
                    onMouseDown={(e) => { e.preventDefault(); handleSelect(place) }}
                    className="w-full text-left px-4 py-3 text-sm hover:bg-sky-50/60 transition-colors
                               border-b last:border-0 flex items-start gap-2"
                    style={{ borderColor: 'rgba(186,230,253,0.35)' }}
                  >
                    <span className="text-primary mt-0.5 shrink-0">📍</span>
                    <span className="text-slate-700 line-clamp-2">{place.display_name}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}

export default AddressPicker
