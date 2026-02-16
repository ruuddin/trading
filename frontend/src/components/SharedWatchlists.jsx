import React, { useEffect, useState } from 'react'

export default function SharedWatchlists() {
  const token = localStorage.getItem('token')
  const [ownedWatchlists, setOwnedWatchlists] = useState([])
  const [sharedWatchlists, setSharedWatchlists] = useState([])
  const [selectedWatchlistId, setSelectedWatchlistId] = useState('')
  const [targetUsername, setTargetUsername] = useState('')
  const [revokeWatchlistId, setRevokeWatchlistId] = useState('')
  const [revokeUsername, setRevokeUsername] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!token) return
    loadOwnedWatchlists()
    loadSharedWatchlists()
  }, [token])

  async function loadOwnedWatchlists() {
    try {
      const response = await fetch('/api/watchlists', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!response.ok) throw new Error(`Failed to load watchlists (${response.status})`)
      const data = await response.json()
      const list = Array.isArray(data) ? data : []
      setOwnedWatchlists(list)
      if (!selectedWatchlistId && list.length > 0) {
        setSelectedWatchlistId(String(list[0].id))
      }
    } catch (err) {
      setError(err?.message || 'Failed to load watchlists')
    }
  }

  async function loadSharedWatchlists() {
    try {
      const response = await fetch('/api/watchlists/shared', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!response.ok) throw new Error(`Failed to load shared watchlists (${response.status})`)
      const data = await response.json()
      setSharedWatchlists(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err?.message || 'Failed to load shared watchlists')
      setSharedWatchlists([])
    }
  }

  async function shareSelectedWatchlist() {
    if (!selectedWatchlistId) {
      setError('Select a watchlist to share.')
      return
    }
    if (!targetUsername.trim()) {
      setError('Target username is required.')
      return
    }

    setLoading(true)
    setMessage('')
    setError('')

    try {
      const response = await fetch(`/api/watchlists/${selectedWatchlistId}/share`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ username: targetUsername.trim() })
      })

      if (!response.ok) {
        const body = await response.text()
        throw new Error(body || `Share failed (${response.status})`)
      }

      const payload = await response.json()
      setMessage(`Shared with ${payload.sharedWith}`)
      setTargetUsername('')
    } catch (err) {
      setError(err?.message || 'Failed to share watchlist')
    } finally {
      setLoading(false)
    }
  }

  async function revokeShare() {
    if (!revokeWatchlistId || !revokeUsername.trim()) {
      setError('Watchlist ID and username are required to revoke.')
      return
    }

    setLoading(true)
    setMessage('')
    setError('')

    try {
      const response = await fetch(`/api/watchlists/${revokeWatchlistId}/share/${encodeURIComponent(revokeUsername.trim())}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!response.ok) {
        const body = await response.text()
        throw new Error(body || `Revoke failed (${response.status})`)
      }

      setMessage('Share revoked')
      setRevokeUsername('')
      await loadSharedWatchlists()
    } catch (err) {
      setError(err?.message || 'Failed to revoke share')
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <div style={{ padding: '20px 0', color: '#9aa4b2' }}>
        Sign in to manage and view shared watchlists.
      </div>
    )
  }

  return (
    <div style={{ padding: '20px 0', color: '#e6eef6' }}>
      <h2 style={{ marginTop: 0 }}>Shared Watchlists</h2>

      <section style={{ border: '1px solid #2a3a52', borderRadius: '10px', padding: '12px', marginBottom: '14px', background: '#0f1419' }}>
        <h3 style={{ marginTop: 0, marginBottom: '10px' }}>Share Your Watchlist</h3>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <select
            value={selectedWatchlistId}
            onChange={(e) => setSelectedWatchlistId(e.target.value)}
            aria-label="Watchlist selector"
          >
            {ownedWatchlists.map((watchlist) => (
              <option key={watchlist.id} value={watchlist.id}>{watchlist.name} (#{watchlist.id})</option>
            ))}
          </select>
          <input
            value={targetUsername}
            onChange={(e) => setTargetUsername(e.target.value)}
            placeholder="Target username"
            aria-label="Target username"
          />
          <button type="button" onClick={shareSelectedWatchlist} disabled={loading}>Share</button>
        </div>
      </section>

      <section style={{ border: '1px solid #2a3a52', borderRadius: '10px', padding: '12px', marginBottom: '14px', background: '#0f1419' }}>
        <h3 style={{ marginTop: 0, marginBottom: '10px' }}>Watchlists Shared With You</h3>
        {sharedWatchlists.length === 0 ? (
          <p style={{ color: '#9aa4b2', margin: 0 }}>No shared watchlists available.</p>
        ) : (
          <ul style={{ margin: 0, paddingLeft: '18px', color: '#9aa4b2' }}>
            {sharedWatchlists.map((watchlist) => (
              <li key={watchlist.id}>
                <strong style={{ color: '#e6eef6' }}>{watchlist.name}</strong>
                {' · id='}{watchlist.id}
                {' · symbols='}{Array.isArray(watchlist.symbols) ? watchlist.symbols.join(', ') : '-'}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section style={{ border: '1px solid #2a3a52', borderRadius: '10px', padding: '12px', background: '#0f1419' }}>
        <h3 style={{ marginTop: 0, marginBottom: '10px' }}>Revoke Share (Owner)</h3>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <input
            value={revokeWatchlistId}
            onChange={(e) => setRevokeWatchlistId(e.target.value)}
            placeholder="Watchlist ID"
            aria-label="Revoke watchlist id"
          />
          <input
            value={revokeUsername}
            onChange={(e) => setRevokeUsername(e.target.value)}
            placeholder="Shared username"
            aria-label="Revoke username"
          />
          <button type="button" onClick={revokeShare} disabled={loading}>Revoke</button>
        </div>
      </section>

      {message ? <p style={{ color: '#00d19a', marginTop: '10px' }}>{message}</p> : null}
      {error ? <p style={{ color: '#ff7f7f', marginTop: '10px' }}>{error}</p> : null}
    </div>
  )
}
