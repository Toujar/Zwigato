/**
 * AddressManager
 *
 * Full CRUD UI for saved delivery addresses.
 * Uses Nominatim (OpenStreetMap) for geocoding — free, no key needed.
 */
import { useState, useEffect, useRef } from 'react'
import { useAddress } from '../../context/AddressContext'
import { useToast } from '../../context/ToastContext'

const LABELS = ['Home', 'Work', "Partner's place", 'Other']
const EMPTY_FORM = { label: 'Home', fullAddress: '', lat: null, lng: null }

function useDebounce(value, delay) {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(t)
  }, [value, delay])
  return debounced
}

const AddressForm = ({ initial, onSave, onCancel }) => {
  const [form, setForm]         = useState(initial || EMPTY_FORM)
  const [suggestions, setSuggestions] = useState([])
  const [showSugg, setShowSugg] = useState(false)
  const [fetching, setFetching] = useState(false)
  const wrapperRef              = useRef(null)
  const debouncedAddress        = useDebounce(form.fullAddress, 450)

  // Nominatim lookup
  useEffect(() => {
    if (!debouncedAddress || debouncedAddress.length < 4) {
      setSuggestions([])
      return
    }
    let cancelled = false
    setFetching(true)
    fetch(
      `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(debouncedAddress)}&format=json&limit=5&countrycodes=in`,
      { headers: { 'User-Agent': 'Zwigato-FoodDelivery/1.0', 'Accept-Language': 'en' } }
    )
      .then(r => r.json())
      .then(data => { if (!cancelled) { setSuggestions(data || []); setShowSugg(true); setFetching(false) } })
      .catch(() => { if (!cancelled) setFetching(false) })
    return () => { cancelled = true }
  }, [debouncedAddress])

  // Close on outside click
  useEffect(() => {
    const h = (e) => { if (wrapperRef.current && !wrapperRef.current.contains(e.target)) setShowSugg(false) }
    document.addEventListener('mousedown', h)
    return () => document.removeEventListener('mousedown', h)
  }, [])

  const handleSelect = (place) => {
    setForm(f => ({ ...f, fullAddress: place.display_name, lat: parseFloat(place.lat), lng: parseFloat(place.lon) }))
    setSuggestions([])
    setShowSugg(false)
  }

  const handleSubmit = e => {
    e.preventDefault()
    if (!form.fullAddress.trim()) return
    onSave(form)
  }

  return (
    <form onSubmit={handleSubmit} className="glass p-5 space-y-4 animate-scale-in">
      {/* Label buttons */}
      <div>
        <label className="block text-slate-700 font-semibold mb-2 text-sm">Address Label</label>
        <div className="flex gap-2 flex-wrap">
          {LABELS.map(l => (
            <button key={l} type="button"
              onClick={() => setForm(f => ({ ...f, label: l }))}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${form.label === l ? 'btn-primary' : 'btn-glass'}`}>
              {l === 'Home' ? '🏠' : l === 'Work' ? '💼' : l === "Partner's place" ? '❤️' : '📍'} {l}
            </button>
          ))}
        </div>
      </div>

      {/* Address input with suggestions */}
      <div ref={wrapperRef} className="relative">
        <label className="block text-slate-700 font-semibold mb-1.5 text-sm">Full Address *</label>
        <div className="relative">
          <input
            type="text" value={form.fullAddress} required autoComplete="off"
            onChange={e => setForm(f => ({ ...f, fullAddress: e.target.value, lat: null, lng: null }))}
            onFocus={() => suggestions.length > 0 && setShowSugg(true)}
            className="input-glass pr-8"
            placeholder="Start typing — OpenStreetMap suggestions will appear"
          />
          {fetching && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2">
              <div className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin" />
            </div>
          )}
        </div>

        {form.lat && form.lng ? (
          <p className="text-xs text-green-600 mt-1 flex items-center gap-1">
            ✅ Location confirmed ({form.lat.toFixed(4)}, {form.lng.toFixed(4)})
          </p>
        ) : (
          <p className="text-xs text-slate-400 mt-1">🔍 Powered by OpenStreetMap · type 4+ chars</p>
        )}

        {showSugg && suggestions.length > 0 && (
          <ul className="absolute z-50 w-full mt-1 glass-elevated rounded-2xl overflow-hidden shadow-glass max-h-52 overflow-y-auto">
            {suggestions.map(place => (
              <li key={place.place_id}>
                <button type="button"
                  onMouseDown={e => { e.preventDefault(); handleSelect(place) }}
                  className="w-full text-left px-4 py-3 text-xs hover:bg-sky-50/60 transition-colors
                             border-b last:border-0 flex items-start gap-2"
                  style={{ borderColor: 'rgba(186,230,253,0.35)' }}>
                  <span className="text-primary shrink-0 mt-0.5">📍</span>
                  <span className="text-slate-700 line-clamp-2">{place.display_name}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="flex gap-2 pt-1">
        <button type="submit" className="btn-primary flex-1">
          {initial ? 'Update Address' : 'Save Address'}
        </button>
        <button type="button" onClick={onCancel} className="btn-glass px-5">Cancel</button>
      </div>
    </form>
  )
}

const AddressManager = () => {
  const { addresses, addAddress, updateAddress, removeAddress, setDefault } = useAddress()
  const toast = useToast()
  const [adding, setAdding]   = useState(false)
  const [editing, setEditing] = useState(null) // address id being edited

  const handleAdd = (form) => {
    addAddress(form)
    setAdding(false)
    toast.success('Address saved!')
  }

  const handleUpdate = (form) => {
    updateAddress(editing, form)
    setEditing(null)
    toast.success('Address updated!')
  }

  const handleRemove = (id) => {
    if (!window.confirm('Remove this address?')) return
    removeAddress(id)
    toast.success('Address removed')
  }

  const LABEL_ICON = { Home: '🏠', Work: '💼', "Partner's place": '❤️', Other: '📍' }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <h3 className="font-black text-secondary text-lg">Saved Addresses</h3>
        {!adding && (
          <button onClick={() => setAdding(true)} className="btn-primary text-sm px-4 py-2 flex items-center gap-1.5">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4"/>
            </svg>
            Add Address
          </button>
        )}
      </div>

      {/* Add form */}
      {adding && (
        <div className="mb-4">
          <AddressForm onSave={handleAdd} onCancel={() => setAdding(false)} />
        </div>
      )}

      {/* Address list */}
      {addresses.length === 0 && !adding ? (
        <div className="glass-subtle rounded-2xl p-8 text-center">
          <div className="text-4xl mb-3">📍</div>
          <p className="font-semibold text-secondary mb-1">No saved addresses</p>
          <p className="text-slate-500 text-sm mb-4">Save your home or work address for faster checkout</p>
          <button onClick={() => setAdding(true)} className="btn-primary text-sm px-5 py-2">
            Add Your First Address
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {addresses.map(addr => (
            <div key={addr.id}>
              {editing === addr.id ? (
                <AddressForm
                  initial={addr}
                  onSave={handleUpdate}
                  onCancel={() => setEditing(null)}
                />
              ) : (
                <div className={`glass-white p-4 flex items-start gap-3 transition-all ${
                  addr.isDefault ? 'ring-1 ring-primary/30' : ''
                }`}>
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center text-xl shrink-0"
                    style={{ background: 'rgba(14,165,233,0.10)', border: '1px solid rgba(14,165,233,0.20)' }}>
                    {LABEL_ICON[addr.label] || '📍'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-0.5">
                      <span className="font-bold text-secondary text-sm">{addr.label}</span>
                      {addr.isDefault && <span className="badge badge-blue text-xs">Default</span>}
                      {addr.lat && addr.lng && (
                        <span className="badge badge-green text-xs">📍 Pinned</span>
                      )}
                    </div>
                    <p className="text-slate-500 text-sm leading-snug">{addr.fullAddress}</p>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    {!addr.isDefault && (
                      <button onClick={() => setDefault(addr.id)}
                        className="text-xs text-primary hover:underline font-semibold px-2 py-1">
                        Set Default
                      </button>
                    )}
                    <button onClick={() => setEditing(addr.id)}
                      className="w-8 h-8 rounded-xl flex items-center justify-center text-slate-400 hover:text-primary hover:bg-sky-50 transition-all">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                          d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                      </svg>
                    </button>
                    <button onClick={() => handleRemove(addr.id)}
                      className="w-8 h-8 rounded-xl flex items-center justify-center text-slate-400 hover:text-red-500 hover:bg-red-50 transition-all">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                      </svg>
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default AddressManager
