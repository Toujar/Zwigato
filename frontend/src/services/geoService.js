/**
 * geoService.js
 *
 * Free geocoding + routing using OpenStreetMap stack.
 * Zero billing — no Google, no credit card required.
 *
 * Geocoding : Nominatim  (free, no key)
 * Routing   : OpenRouteService free tier (optional key, falls back to haversine)
 */

// ── In-memory cache — avoids redundant Nominatim calls ───────────────
const geocodeCache = new Map()

// ── Nominatim base ────────────────────────────────────────────────────
const NOMINATIM = 'https://nominatim.openstreetmap.org/search'
const HEADERS   = {
  'User-Agent':      'Zwigato-FoodDelivery/1.0',
  'Accept-Language': 'en',
}

/**
 * Single Nominatim lookup — returns first result or null.
 */
async function nominatimQuery(q) {
  try {
    const url = `${NOMINATIM}?q=${encodeURIComponent(q)}&format=json&limit=1&countrycodes=in`
    const res  = await fetch(url, { headers: HEADERS })
    const data = await res.json()
    if (data?.length > 0) {
      return { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lon) }
    }
  } catch { /* network error */ }
  return null
}

/**
 * Build a list of progressively simpler queries from an address string.
 *
 * Strategy (stops as soon as one resolves):
 *   1. Full address as-is
 *   2. Strip leading house/door number (e.g. "12 Church St" → "Church St, MG Road, Bengaluru")
 *   3. From the second comma onward (drops street, keeps area + city)
 *   4. Last two comma-parts (area + city)
 *   5. Last part only (city/pincode)
 *
 * This handles short Indian addresses like "12 Church St, MG Road, Bengaluru"
 * that Nominatim won't resolve as a single query but will resolve
 * when simplified to "MG Road, Bengaluru".
 */
function buildFallbacks(address) {
  const parts = address.split(',').map(p => p.trim()).filter(Boolean)
  const queries = new Set()

  // 1. Full address
  queries.add(address.trim())

  // 2. Strip leading number (e.g. "12 Church St" → "Church St, MG Road, Bengaluru")
  const noLeadingNum = address.replace(/^\d+[\s-]*/,'').trim()
  if (noLeadingNum !== address.trim()) queries.add(noLeadingNum)

  // 3. Drop first part (usually the door/building number or street name)
  if (parts.length > 2) queries.add(parts.slice(1).join(', '))

  // 4. Drop first two parts (area + city)
  if (parts.length > 3) queries.add(parts.slice(2).join(', '))

  // 5. Last two parts (usually "Area, City")
  if (parts.length >= 2) queries.add(parts.slice(-2).join(', '))

  // 6. Last part only (city or pincode)
  if (parts.length >= 1) queries.add(parts[parts.length - 1])

  return [...queries]
}

/**
 * Geocode an address → { lat, lng }
 *
 * Tries progressively simplified fallback queries until one resolves.
 * Results are cached in memory to avoid redundant Nominatim calls.
 *
 * @param {string} address
 * @returns {Promise<{lat:number, lng:number} | null>}
 */
export async function geocodeAddress(address) {
  if (!address?.trim()) return null

  const cacheKey = address.trim().toLowerCase()
  if (geocodeCache.has(cacheKey)) return geocodeCache.get(cacheKey)

  const fallbacks = buildFallbacks(address)

  for (const query of fallbacks) {
    if (!query || query.length < 3) continue
    const result = await nominatimQuery(query)
    if (result) {
      // Cache under the original key so we don't re-try next time
      geocodeCache.set(cacheKey, result)
      return result
    }
    // Nominatim rate limit: 1 req/sec — small delay between retries
    await new Promise(r => setTimeout(r, 300))
  }

  return null
}

/**
 * Driving route between two lat/lng coords.
 * Uses OpenRouteService (free tier, 2000/day) if VITE_ORS_API_KEY is set.
 * Falls back to a straight-line haversine estimate otherwise.
 *
 * @param {{ lat:number, lng:number }} origin
 * @param {{ lat:number, lng:number }} destination
 * @returns {Promise<{polyline:[number,number][], distanceKm:string, durationMin:string, isStraightLine:boolean}>}
 */
export async function getDrivingRoute(origin, destination) {
  const orsKey = import.meta.env.VITE_ORS_API_KEY

  if (!orsKey || orsKey === 'YOUR_ORS_API_KEY_HERE') {
    return straightLineFallback(origin, destination)
  }

  try {
    const res = await fetch(
      'https://api.openrouteservice.org/v2/directions/driving-car/geojson',
      {
        method:  'POST',
        headers: {
          'Authorization': orsKey,
          'Content-Type':  'application/json',
          'Accept':        'application/json, application/geo+json',
        },
        body: JSON.stringify({
          coordinates: [
            [origin.lng,      origin.lat],       // ORS: [lng, lat]
            [destination.lng, destination.lat],
          ],
        }),
      }
    )

    if (!res.ok) return straightLineFallback(origin, destination)

    const geojson = await res.json()
    const feature = geojson.features?.[0]
    if (!feature)  return straightLineFallback(origin, destination)

    const summary  = feature.properties?.summary
    const coords   = feature.geometry?.coordinates        // [[lng,lat], ...]
    const polyline = coords.map(([lng, lat]) => [lat, lng]) // → Leaflet [lat,lng]

    const distKm = ((summary?.distance ?? 0) / 1000).toFixed(1)
    const durMin = Math.round((summary?.duration ?? 0) / 60)

    return {
      polyline,
      distanceKm:     `${distKm} km`,
      durationMin:    `${durMin} min${durMin !== 1 ? 's' : ''}`,
      isStraightLine: false,
    }
  } catch {
    return straightLineFallback(origin, destination)
  }
}

// ── Helpers ──────────────────────────────────────────────────────────

function straightLineFallback(origin, destination) {
  const km = haversineKm(origin, destination)
  return {
    polyline:       [[origin.lat, origin.lng], [destination.lat, destination.lng]],
    distanceKm:     `~${km.toFixed(1)} km`,
    durationMin:    `~${Math.round(km * 3)} mins`,
    isStraightLine: true,
  }
}

function haversineKm(a, b) {
  const R    = 6371
  const dLat = deg2rad(b.lat - a.lat)
  const dLng = deg2rad(b.lng - a.lng)
  const x    = Math.sin(dLat / 2) ** 2 +
               Math.cos(deg2rad(a.lat)) * Math.cos(deg2rad(b.lat)) *
               Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x))
}

function deg2rad(d) { return d * (Math.PI / 180) }
