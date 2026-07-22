// Generic data-fetching hook
// Args:    url (string), options (object)
// Returns: { data, loading, error, refetch }
// Handles loading state, error state, and optional auto-fetch on mount

import { useState, useEffect, useCallback } from 'react'
import api from '../services/api'

export function useFetch(url, options = {}) {
  const { autoFetch = true, initialData = null } = options

  const [data, setData] = useState(initialData)
  const [loading, setLoading] = useState(autoFetch)
  const [error, setError] = useState(null)

  const fetchData = useCallback(async (fetchUrl) => {
    const targetUrl = fetchUrl || url
    if (!targetUrl) return

    setLoading(true)
    setError(null)

    try {
      const response = await api.get(targetUrl)
      setData(response.data)
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.message ||
        'Something went wrong'
      setError(message)
    } finally {
      setLoading(false)
    }
  }, [url])

  const refetch = useCallback(() => {
    return fetchData()
  }, [fetchData])

  useEffect(() => {
    if (autoFetch && url) {
      fetchData()
    }
  }, [url, autoFetch, fetchData])

  return { data, loading, error, refetch, setData }
}
