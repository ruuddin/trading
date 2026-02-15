import React, { useState } from 'react'
import { trackActivationEvent } from '../services/analyticsService'

export default function Screener() {
  const token = localStorage.getItem('token')
  const [query, setQuery] = useState('')
  const [minPrice, setMinPrice] = useState('')
  const [maxPrice, setMaxPrice] = useState('')
  const [limit, setLimit] = useState(50)
  const [results, setResults] = useState([])
  const [count, setCount] = useState(0)
  const [savedScans, setSavedScans] = useState([])
  const [scanName, setScanName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const authHeaders = token ? { Authorization: `Bearer ${token}` } : {}

  async function runScreener(e) {
    e?.preventDefault?.()
    if (!token) {
      setError('Sign in to use screener.')
      return
    }

    setLoading(true)
    setError('')

    try {
      const params = new URLSearchParams()
      if (query.trim()) params.set('query', query.trim())
      if (minPrice !== '') params.set('minPrice', minPrice)
      if (maxPrice !== '') params.set('maxPrice', maxPrice)
      params.set('limit', String(limit || 50))

      const response = await fetch(`/api/screener?${params.toString()}`, {
        headers: authHeaders
      })
      if (!response.ok) throw new Error(`Failed to run screener (${response.status})`)
      const data = await response.json()
      setResults(Array.isArray(data?.results) ? data.results : [])
      setCount(Number(data?.count || 0))
      trackActivationEvent('screener_run', {
        query: query.trim() || null,
        minPrice: minPrice === '' ? null : Number(minPrice),
        maxPrice: maxPrice === '' ? null : Number(maxPrice),
        resultCount: Number(data?.count || 0)
      })
    } catch (err) {
      setError(err?.message || 'Failed to run screener')
      setResults([])
      setCount(0)
    } finally {
      setLoading(false)
    }
  }

  async function loadSavedScans() {
    if (!token) {
      setError('Sign in to view saved scans.')
      return
    }

    setError('')
    try {
      const response = await fetch('/api/screener/saved', {
        headers: authHeaders
      })
      if (!response.ok) throw new Error(`Failed to load saved scans (${response.status})`)
      const data = await response.json()
      setSavedScans(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err?.message || 'Failed to load saved scans')
      setSavedScans([])
    }
  }

  async function saveCurrentScan() {
    if (!token) {
      setError('Sign in to save scans.')
      return
    }
    if (!scanName.trim()) {
      setError('Scan name is required.')
      return
    }

    setError('')
    try {
      const payload = {
        name: scanName.trim(),
        query: query.trim() || null,
        minPrice: minPrice === '' ? null : Number(minPrice),
        maxPrice: maxPrice === '' ? null : Number(maxPrice)
      }

      const response = await fetch('/api/screener/saved', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeaders
        },
        body: JSON.stringify(payload)
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || `Failed to save scan (${response.status})`)
      }

      trackActivationEvent('screener_saved', {
        name: scanName.trim(),
        query: query.trim() || null
      }, { onceKey: `screener_saved_${scanName.trim().toLowerCase()}` })

      setScanName('')
      await loadSavedScans()
    } catch (err) {
      setError(err?.message || 'Failed to save scan')
    }
  }

  if (!token) {
    return (
      <div style={{ padding: '20px 0', color: '#9aa4b2' }}>
        Sign in to use screener and saved scans.
      </div>
    )
  }

  return (
    <div style={{ padding: '20px 0', color: '#e6eef6' }}>
      <h2 style={{ marginTop: 0 }}>Screener</h2>

      <form onSubmit={runScreener} style={{ display: 'grid', gap: '10px', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', marginBottom: '14px' }}>
        <label>
          Query
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="e.g. NVDA" style={{ width: '100%' }} />
        </label>
        <label>
          Min Price
          <input type="number" min="0" step="0.01" value={minPrice} onChange={(e) => setMinPrice(e.target.value)} style={{ width: '100%' }} />
        </label>
        <label>
          Max Price
          <input type="number" min="0" step="0.01" value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} style={{ width: '100%' }} />
        </label>
        <label>
          Limit
          <input type="number" min="1" max="200" value={limit} onChange={(e) => setLimit(Number(e.target.value || 50))} style={{ width: '100%' }} />
        </label>
        <div style={{ display: 'flex', alignItems: 'end', gap: '8px' }}>
          <button type="submit" disabled={loading}>{loading ? 'Running...' : 'Run Screener'}</button>
          <button type="button" onClick={loadSavedScans}>Load Saved</button>
        </div>
      </form>

      <div style={{ marginBottom: '12px', color: '#9aa4b2' }}>Results: {count}</div>

      {results.length > 0 ? (
        <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '14px' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'left' }}>Symbol</th>
              <th style={{ textAlign: 'left' }}>Name</th>
              <th style={{ textAlign: 'left' }}>Price</th>
            </tr>
          </thead>
          <tbody>
            {results.map((item) => (
              <tr key={String(item.symbol)}>
                <td>{item.symbol}</td>
                <td>{item.name}</td>
                <td>{item.price == null ? 'N/A' : `$${Number(item.price).toFixed(2)}`}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p style={{ color: '#9aa4b2' }}>No results yet. Run a screen to see matches.</p>
      )}

      <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
        <input
          value={scanName}
          onChange={(e) => setScanName(e.target.value)}
          placeholder="Saved scan name"
          style={{ maxWidth: 260 }}
        />
        <button type="button" onClick={saveCurrentScan}>Save Scan</button>
      </div>

      <h3 style={{ marginBottom: '8px' }}>Saved Scans</h3>
      {savedScans.length === 0 ? (
        <p style={{ color: '#9aa4b2' }}>No saved scans loaded.</p>
      ) : (
        <ul style={{ margin: 0, paddingLeft: '18px', color: '#9aa4b2' }}>
          {savedScans.map((scan) => (
            <li key={scan.id}>
              <strong style={{ color: '#e6eef6' }}>{scan.name}</strong>
              {' · query='}{scan.query || '-'}
              {' · min='}{scan.minPrice ?? '-'}
              {' · max='}{scan.maxPrice ?? '-'}
            </li>
          ))}
        </ul>
      )}

      {error ? <p style={{ color: '#ff7f7f', marginTop: '10px' }}>{error}</p> : null}
    </div>
  )
}
