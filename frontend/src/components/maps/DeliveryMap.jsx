/**
 * DeliveryMap
 *
 * Shows a Google Map with:
 *  - 📍 Restaurant marker (origin)
 *  - 🏠 Customer marker (destination)
 *  - Blue driving route between them (Google Directions API)
 *  - Distance and duration info bar
 *
 * Props:
 *  - restaurantAddress {string}  — full address of the restaurant
 *  - deliveryAddress   {string}  — full address of the customer
 *  - restaurantName    {string}  — label for the restaurant pin
 *  - height            {string}  — CSS height (default "400px")
 */
import { useState, useCallback, useRef } from 'react'
import {
  GoogleMap,
  useJsApiLoader,
  Marker,
  DirectionsRenderer,
  InfoWindow,
} from '@react-google-maps/api'

const LIBRARIES = ['places', 'geometry']

const MAP_STYLE = [
  { featureType: 'poi.business', stylers: [{ visibility: 'off' }] },
  { featureType: 'transit',      stylers: [{ visibility: 'simplified' }] },
  {
    featureType: 'water',
    elementType: 'geometry',
    stylers: [{ color: '#bae6fd' }],
  },
  {
    featureType: 'road.highway',
    elementType: 'geometry',
    stylers: [{ color: '#e0f2fe' }],
  },
]

const DEFAULT_CENTER = { lat: 12.9716, lng: 77.5946 } // Bengaluru

const DeliveryMap = ({
  restaurantAddress,
  deliveryAddress,
  restaurantName = 'Restaurant',
  height = '400px',
}) => {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY

  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey: apiKey || '',
    libraries: LIBRARIES,
  })

  const [map, setMap]                   = useState(null)
  const [directions, setDirections]     = useState(null)
  const [routeInfo, setRouteInfo]       = useState(null) // { distance, duration }
  const [routeError, setRouteError]     = useState(null)
  const [activeMarker, setActiveMarker] = useState(null) // 'origin' | 'dest'
  const [calculating, setCalculating]   = useState(false)
  const directionsService               = useRef(null)

  const onMapLoad = useCallback((mapInstance) => {
    setMap(mapInstance)
    directionsService.current = new window.google.maps.DirectionsService()

    if (restaurantAddress && deliveryAddress) {
      calculateRoute(mapInstance)
    }
  }, [restaurantAddress, deliveryAddress]) // eslint-disable-line

  const calculateRoute = useCallback((mapInstance) => {
    if (!restaurantAddress || !deliveryAddress) return
    if (!window.google) return

    setCalculating(true)
    setRouteError(null)

    const svc = directionsService.current ||
      new window.google.maps.DirectionsService()

    svc.route(
      {
        origin:      restaurantAddress,
        destination: deliveryAddress,
        travelMode:  window.google.maps.TravelMode.DRIVING,
        region:      'IN',
      },
      (result, status) => {
        setCalculating(false)
        if (status === 'OK' && result) {
          setDirections(result)
          const leg = result.routes[0]?.legs[0]
          if (leg) {
            setRouteInfo({
              distance: leg.distance?.text,
              duration: leg.duration?.text,
            })
          }
          // Fit map to route bounds
          if (mapInstance && result.routes[0]?.bounds) {
            mapInstance.fitBounds(result.routes[0].bounds, 60)
          }
        } else {
          setRouteError(`Could not calculate route: ${status}`)
          // Fall back to showing both addresses as text
        }
      }
    )
  }, [restaurantAddress, deliveryAddress])

  // ── No API key ──────────────────────────────────────────────
  if (!apiKey || apiKey === 'YOUR_GOOGLE_MAPS_API_KEY_HERE') {
    return (
      <div className="glass-subtle rounded-2xl p-6 text-center" style={{ height }}>
        <div className="flex flex-col items-center justify-center h-full gap-3">
          <div className="text-4xl">🗺️</div>
          <p className="font-bold text-secondary">Map not configured</p>
          <p className="text-slate-500 text-sm max-w-xs">
            Add your Google Maps API key to <code className="bg-sky-50 px-1.5 py-0.5 rounded text-xs">.env</code>:
          </p>
          <code className="text-xs bg-slate-100 px-3 py-2 rounded-xl text-slate-600 block max-w-full overflow-x-auto">
            VITE_GOOGLE_MAPS_API_KEY=AIza...
          </code>
          <a href="https://console.cloud.google.com/" target="_blank" rel="noreferrer"
            className="btn-glass text-xs px-4 py-2 mt-1">
            Get API Key →
          </a>

          {/* Fallback address display */}
          {(restaurantAddress || deliveryAddress) && (
            <div className="w-full mt-4 space-y-2 text-left">
              {restaurantAddress && (
                <div className="glass-subtle rounded-xl p-3 flex items-start gap-2">
                  <span className="text-lg shrink-0">🏪</span>
                  <div>
                    <p className="text-xs font-bold text-secondary">{restaurantName}</p>
                    <p className="text-xs text-slate-500">{restaurantAddress}</p>
                  </div>
                </div>
              )}
              {deliveryAddress && (
                <div className="glass-subtle rounded-xl p-3 flex items-start gap-2">
                  <span className="text-lg shrink-0">🏠</span>
                  <div>
                    <p className="text-xs font-bold text-secondary">Your Location</p>
                    <p className="text-xs text-slate-500">{deliveryAddress}</p>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    )
  }

  if (loadError) {
    return (
      <div className="glass-subtle rounded-2xl p-6 text-center" style={{ height }}>
        <p className="text-red-500 font-semibold">Failed to load Google Maps</p>
        <p className="text-slate-500 text-sm mt-1">{loadError.message}</p>
      </div>
    )
  }

  if (!isLoaded) {
    return (
      <div className="glass-subtle rounded-2xl flex items-center justify-center" style={{ height }}>
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          <p className="text-slate-500 text-sm">Loading map…</p>
        </div>
      </div>
    )
  }

  return (
    <div className="rounded-2xl overflow-hidden" style={{ height }}>
      {/* Route info bar */}
      {(routeInfo || calculating || routeError) && (
        <div className="absolute z-10 top-3 left-1/2 -translate-x-1/2"
          style={{ pointerEvents: 'none' }}>
          <div className="glass-elevated px-4 py-2 flex items-center gap-3 rounded-full shadow-glass">
            {calculating ? (
              <>
                <div className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                <span className="text-xs font-semibold text-slate-600">Calculating route…</span>
              </>
            ) : routeError ? (
              <span className="text-xs font-semibold text-red-500">{routeError}</span>
            ) : (
              <>
                <span className="text-sm">🛵</span>
                <span className="text-xs font-bold text-secondary">{routeInfo.distance}</span>
                <span className="text-slate-300 text-xs">·</span>
                <span className="text-xs font-semibold text-primary">{routeInfo.duration}</span>
              </>
            )}
          </div>
        </div>
      )}

      <div className="relative w-full h-full">
        <GoogleMap
          mapContainerStyle={{ width: '100%', height: '100%' }}
          center={DEFAULT_CENTER}
          zoom={13}
          onLoad={onMapLoad}
          options={{
            styles:            MAP_STYLE,
            disableDefaultUI:  false,
            zoomControl:       true,
            streetViewControl: false,
            mapTypeControl:    false,
            fullscreenControl: true,
          }}
        >
          {/* Route polyline */}
          {directions && (
            <DirectionsRenderer
              directions={directions}
              options={{
                suppressMarkers:   true, // we draw our own markers
                polylineOptions: {
                  strokeColor:   '#0EA5E9',
                  strokeWeight:  5,
                  strokeOpacity: 0.85,
                },
              }}
            />
          )}

          {/* Restaurant marker */}
          {directions?.routes?.[0]?.legs?.[0]?.start_location && (
            <>
              <Marker
                position={directions.routes[0].legs[0].start_location}
                icon={{
                  url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                    <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40">
                      <circle cx="20" cy="20" r="18" fill="#0EA5E9" stroke="white" stroke-width="3"/>
                      <text x="20" y="26" text-anchor="middle" font-size="18">🏪</text>
                    </svg>
                  `),
                  scaledSize: new window.google.maps.Size(40, 40),
                  anchor:     new window.google.maps.Point(20, 20),
                }}
                onClick={() => setActiveMarker('origin')}
              />
              {activeMarker === 'origin' && (
                <InfoWindow
                  position={directions.routes[0].legs[0].start_location}
                  onCloseClick={() => setActiveMarker(null)}
                >
                  <div className="text-sm">
                    <p className="font-bold text-slate-800">{restaurantName}</p>
                    <p className="text-slate-500 text-xs mt-0.5">{restaurantAddress}</p>
                  </div>
                </InfoWindow>
              )}
            </>
          )}

          {/* Delivery destination marker */}
          {directions?.routes?.[0]?.legs?.[0]?.end_location && (
            <>
              <Marker
                position={directions.routes[0].legs[0].end_location}
                icon={{
                  url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                    <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40">
                      <circle cx="20" cy="20" r="18" fill="#22C55E" stroke="white" stroke-width="3"/>
                      <text x="20" y="26" text-anchor="middle" font-size="18">🏠</text>
                    </svg>
                  `),
                  scaledSize: new window.google.maps.Size(40, 40),
                  anchor:     new window.google.maps.Point(20, 20),
                }}
                onClick={() => setActiveMarker('dest')}
              />
              {activeMarker === 'dest' && (
                <InfoWindow
                  position={directions.routes[0].legs[0].end_location}
                  onCloseClick={() => setActiveMarker(null)}
                >
                  <div className="text-sm">
                    <p className="font-bold text-slate-800">Delivery Location</p>
                    <p className="text-slate-500 text-xs mt-0.5">{deliveryAddress}</p>
                  </div>
                </InfoWindow>
              )}
            </>
          )}
        </GoogleMap>

        {/* Floating route info overlay */}
        {routeInfo && (
          <div className="absolute bottom-4 left-4 right-4 pointer-events-none">
            <div className="glass-elevated px-4 py-3 flex items-center gap-4 rounded-2xl">
              <div className="flex items-center gap-2">
                <span className="text-xl">🏪</span>
                <span className="text-xs text-slate-500 max-w-[120px] truncate">{restaurantName}</span>
              </div>
              <div className="flex-1 flex items-center gap-1 justify-center">
                <div className="h-0.5 flex-1 bg-primary/30 relative">
                  <div className="absolute -top-1.5 left-1/2 -translate-x-1/2 text-xs animate-bounce">🛵</div>
                </div>
              </div>
              <div className="flex flex-col items-end">
                <span className="text-xs font-black text-primary">{routeInfo.distance}</span>
                <span className="text-xs text-slate-400">{routeInfo.duration}</span>
              </div>
              <span className="text-xl">🏠</span>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default DeliveryMap
