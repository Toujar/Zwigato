/**
 * DeliveryMap
 *
 * Renders a free, no-billing delivery route map using:
 *   - Leaflet.js        — map rendering library
 *   - OpenStreetMap     — free map tiles (no key needed)
 *   - Nominatim         — free geocoding (address → lat/lng)
 *   - OpenRouteService  — free driving route (optional API key)
 *
 * Props:
 *   restaurantAddress {string}  — full address of restaurant
 *   deliveryAddress   {string}  — customer delivery address
 *   restaurantName    {string}  — label shown on restaurant marker
 *   height            {string}  — CSS height string (default "380px")
 */
import { useEffect, useRef, useState } from 'react'
import { MapContainer, TileLayer, Marker, Polyline, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import { geocodeAddress, getDrivingRoute } from '../../services/geoService'

// ── Fix Leaflet's default marker icons (broken in Vite/webpack) ──────
// Leaflet tries to auto-detect icon URLs via webpack magic comments
// that Vite doesn't support. Override them explicitly.
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

// Custom SVG marker factory — renders emoji inside a coloured circle
const makeIcon = (emoji, color) =>
  L.divIcon({
    className: '',
    html: `
      <div style="
        width:40px;height:40px;border-radius:50%;
        background:${color};border:3px solid white;
        display:flex;align-items:center;justify-content:center;
        font-size:18px;box-shadow:0 2px 8px rgba(0,0,0,0.25);
      ">${emoji}</div>`,
    iconSize:   [40, 40],
    iconAnchor: [20, 20],
    popupAnchor:[0, -22],
  })

const RESTAURANT_ICON = makeIcon('🏪', '#0EA5E9')
const DELIVERY_ICON   = makeIcon('🏠', '#22C55E')

// DEFAULT_CENTER = Bengaluru city centre
const DEFAULT_CENTER = [12.9716, 77.5946]

// Inner component that fits map bounds when the route changes
const FitBounds = ({ positions }) => {
  const map = useMap()
  useEffect(() => {
    if (positions && positions.length >= 2) {
      map.fitBounds(L.latLngBounds(positions), { padding: [50, 50] })
    }
  }, [positions, map])
  return null
}

const DeliveryMap = ({
  restaurantAddress,
  deliveryAddress,
  restaurantName   = 'Restaurant',
  orderStatus      = null,
  deliveryProgress = 0,
  onRouteCalculated = null,   // (durationMinutes: number) => void  — fires once when route is ready
  height           = '380px',
}) => {
  const isDelivered = orderStatus === 'DELIVERED'
  const isCancelled = orderStatus === 'CANCELLED'
  const isOnWay     = orderStatus === 'OUT_FOR_DELIVERY'
  const [state, setState] = useState({
    loading:      true,
    error:        null,
    originCoords: null,
    destCoords:   null,
    partial:      false,  // true if one address fell back to city centre
    route:        null,
  })

  useEffect(() => {
    if (!restaurantAddress || !deliveryAddress) {
      setState(s => ({ ...s, loading: false }))
      return
    }

    let cancelled = false

    const load = async () => {
      setState(s => ({ ...s, loading: true, error: null }))

      // Step 1: geocode both addresses in parallel
      const [origin, dest] = await Promise.all([
        geocodeAddress(restaurantAddress),
        geocodeAddress(deliveryAddress),
      ])

      if (cancelled) return

      if (!origin && !dest) {
        setState(s => ({
          ...s, loading: false,
          error: 'Could not locate either address. Check that the city name is included.',
        }))
        return
      }

      // If only one resolved, use city centre of Bengaluru as fallback for the other
      const BENGALURU = { lat: 12.9716, lng: 77.5946 }
      const resolvedOrigin = origin || BENGALURU
      const resolvedDest   = dest   || BENGALURU
      const partial        = !origin || !dest

      // Step 2: fetch driving route
      const route = await getDrivingRoute(resolvedOrigin, resolvedDest)
      if (cancelled) return

      setState({
        loading: false,
        error:   null,
        originCoords: resolvedOrigin,
        destCoords:   resolvedDest,
        partial,
        route,
      })

      // Fire callback with the real driving duration so the parent
      // countdown uses actual route time instead of the DB estimate.
      if (route && onRouteCalculated) {
        // durationMin is e.g. "16 mins" or "~18 mins" — parse the number
        const mins = parseInt(route.durationMin, 10)
        if (!isNaN(mins) && mins > 0) onRouteCalculated(mins)
      }
    }

    load()
    return () => { cancelled = true }
  }, [restaurantAddress, deliveryAddress])

  // ── Loading state ───────────────────────────────────────────
  if (state.loading) {
    return (
      <div className="glass-subtle rounded-2xl flex flex-col items-center justify-center gap-3"
        style={{ height }}>
        <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
        <p className="text-slate-500 text-sm">Calculating route…</p>
        <p className="text-slate-400 text-xs">Powered by OpenStreetMap</p>
      </div>
    )
  }

  // ── Error state ─────────────────────────────────────────────
  if (state.error) {
    return (
      <div className="glass-subtle rounded-2xl p-5" style={{ height }}>
        <div className="flex flex-col items-center justify-center h-full gap-3 text-center">
          <div className="text-3xl">⚠️</div>
          <p className="text-slate-600 text-sm font-semibold">{state.error}</p>
          <div className="space-y-2 w-full mt-2">
            {restaurantAddress && (
              <div className="glass rounded-xl p-3 flex items-center gap-2 text-left">
                <span>🏪</span>
                <div>
                  <p className="text-xs font-bold text-secondary">{restaurantName}</p>
                  <p className="text-xs text-slate-500">{restaurantAddress}</p>
                </div>
              </div>
            )}
            {deliveryAddress && (
              <div className="glass rounded-xl p-3 flex items-center gap-2 text-left">
                <span>🏠</span>
                <div>
                  <p className="text-xs font-bold text-secondary">Delivery</p>
                  <p className="text-xs text-slate-500">{deliveryAddress}</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    )
  }

  // ── No coords yet ───────────────────────────────────────────
  if (!state.originCoords || !state.destCoords) {
    return (
      <div className="glass-subtle rounded-2xl p-5 text-center" style={{ height }}>
        <p className="text-slate-400 text-sm mt-4">Enter a delivery address to see the route</p>
      </div>
    )
  }

  const { originCoords, destCoords, route, partial } = state
  const polylinePositions = route?.polyline ?? [
    [originCoords.lat, originCoords.lng],
    [destCoords.lat,   destCoords.lng],
  ]

  return (
    <div className="rounded-2xl overflow-hidden relative" style={{ height }}>

      {/* ── Route info bar (floats above map) ── */}
      {route && (
        <div className="absolute top-3 left-1/2 -translate-x-1/2 z-[1000] pointer-events-none">
          <div className="glass-elevated px-4 py-2 rounded-full flex items-center gap-3 shadow-glass text-sm">
            <span>🏪</span>
            {isDelivered ? (
              <span className="font-black text-green-600">Delivered ✓</span>
            ) : isOnWay && deliveryProgress > 0 ? (
              /* Shrink distance + time as scooter gets closer */
              <>
                <span className="font-black text-primary">
                  ~{Math.max(0, (1 - deliveryProgress) * parseFloat(route.distanceKm)).toFixed(1)} km
                </span>
                <span className="text-slate-300">·</span>
                <span className="font-semibold text-secondary">
                  ~{Math.max(0, Math.round((1 - deliveryProgress) * parseInt(route.durationMin, 10)))} mins
                </span>
                <span className="text-base animate-bounce">🛵</span>
              </>
            ) : (
              <>
                <span className="font-black text-primary">{route.distanceKm}</span>
                <span className="text-slate-300">·</span>
                <span className="font-semibold text-secondary">{route.durationMin}</span>
              </>
            )}
            <span>🏠</span>
            {route.isStraightLine && !isDelivered && (
              <span className="text-slate-400 text-xs">(est.)</span>
            )}
          </div>
        </div>
      )}

      {/* ── Partial geocode warning ── */}
      {partial && (
        <div className="absolute top-14 left-3 right-3 z-[1000] pointer-events-none">
          <div className="glass-elevated px-3 py-2 rounded-xl flex items-center gap-2">
            <span className="text-sm">⚠️</span>
            <p className="text-xs text-slate-500">
              One address could not be precisely located — showing approximate position.
            </p>
          </div>
        </div>
      )}

      {/* ── Leaflet map ── */}
      <MapContainer
        center={DEFAULT_CENTER}
        zoom={12}
        style={{ width: '100%', height: '100%' }}
        zoomControl={true}
        attributionControl={true}
      >
        {/* OpenStreetMap tiles — completely free */}
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors'
          maxZoom={19}
        />

        {/* Auto-fit map to route */}
        <FitBounds positions={polylinePositions} />

        {/* Driving route polyline — green when delivered, blue when active */}
        <Polyline
          positions={polylinePositions}
          pathOptions={{
            color:     isDelivered ? '#22C55E' : '#0EA5E9',
            weight:    5,
            opacity:   isDelivered ? 0.7 : 0.85,
            dashArray: route?.isStraightLine ? '8 6' : undefined,
          }}
        />

        {/* Restaurant marker — hidden after delivery (customer only sees destination) */}
        {!isDelivered && (
          <Marker position={[originCoords.lat, originCoords.lng]} icon={RESTAURANT_ICON}>
            <Popup>
              <div style={{ minWidth: 140 }}>
                <p style={{ fontWeight: 700, marginBottom: 2 }}>{restaurantName}</p>
                <p style={{ fontSize: 11, color: '#64748b' }}>{restaurantAddress}</p>
              </div>
            </Popup>
          </Marker>
        )}

        {/* Delivery marker */}
        <Marker position={[destCoords.lat, destCoords.lng]} icon={DELIVERY_ICON}>
          <Popup>
            <div style={{ minWidth: 140 }}>
              <p style={{ fontWeight: 700, marginBottom: 2 }}>
                {isDelivered ? '✓ Delivered Here' : 'Delivery Location'}
              </p>
              <p style={{ fontSize: 11, color: '#64748b' }}>{deliveryAddress}</p>
            </div>
          </Popup>
        </Marker>
      </MapContainer>

      {/* ── Bottom info bar ── */}
      {route && (
        <div className="absolute bottom-3 left-3 right-3 z-[1000] pointer-events-none">
          <div className="glass-elevated px-4 py-3 rounded-2xl flex items-center gap-3">
            {isDelivered ? (
              /* Delivered — show green confirmation bar */
              <div className="flex-1 flex items-center justify-center gap-2">
                <span className="text-lg">🏠</span>
                <span className="text-sm font-black text-green-600">Delivered ✓</span>
                <span className="text-slate-300 text-xs">·</span>
                <span className="text-xs text-slate-400">{route.distanceKm}</span>
              </div>
            ) : (
              /* In transit — scooter animation */
              <>
                <div className="flex items-center gap-1.5 min-w-0">
                  <span className="text-lg shrink-0">🏪</span>
                  <span className="text-xs text-slate-500 truncate">{restaurantName}</span>
                </div>
                <div className="flex-1 flex items-center justify-center">
                  <div className="h-px flex-1 bg-primary/20 relative">
                    <span className="absolute -top-3 left-1/2 -translate-x-1/2 text-sm animate-bounce">
                      🛵
                    </span>
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-xs font-black text-primary">{route.distanceKm}</p>
                  <p className="text-xs text-slate-400">{route.durationMin}</p>
                </div>
                <span className="text-lg shrink-0">🏠</span>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default DeliveryMap
