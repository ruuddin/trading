import React, { useEffect, useState } from 'react'

export default function DeveloperApiPanel({ planTier = 'FREE' }) {
  const token = localStorage.getItem('token')
  const [usage, setUsage] = useState(null)
  const [apiKeyName, setApiKeyName] = useState('Default Key')
  const [newlyIssuedKey, setNewlyIssuedKey] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token || planTier !== 'PREMIUM') return
    loadUsage()
  }, [token, planTier])

  async function loadUsage() {
    setError('')
    try {
      const response = await fetch('/api/dev/usage', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!response.ok) throw new Error(`Failed to load usage (${response.status})`)
      const data = await response.json()
      setUsage(data)
    } catch (err) {
      setError(err?.message || 'Failed to load developer usage')
      setUsage(null)
    }
  }

  async function generateKey() {
    if (!apiKeyName.trim()) {
      setError('Key name is required')
      return
    }

    setLoading(true)
    setError('')

    try {
      const response = await fetch('/api/dev/keys', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ name: apiKeyName.trim() })
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || `Failed to create key (${response.status})`)
      }

      const data = await response.json()
      setNewlyIssuedKey(String(data?.apiKey || ''))
      await loadUsage()
    } catch (err) {
      setError(err?.message || 'Failed to create API key')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section style={{ marginTop: '16px', border: '1px solid #2a3a52', borderRadius: '10px', padding: '14px', background: '#0f1419' }}>
      <h3 style={{ marginTop: 0, color: '#e6eef6' }}>Developer API</h3>

      {!token ? (
        <p style={{ color: '#9aa4b2', margin: 0 }}>Sign in to manage developer API keys.</p>
      ) : planTier !== 'PREMIUM' ? (
        <p style={{ color: '#9aa4b2', margin: 0 }}>Upgrade to PREMIUM to create and manage developer API keys.</p>
      ) : (
        <>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
            <input
              value={apiKeyName}
              onChange={(event) => setApiKeyName(event.target.value)}
              placeholder="API key name"
              aria-label="API key name"
            />
            <button type="button" onClick={generateKey} disabled={loading}>
              {loading ? 'Creating...' : 'Generate API Key'}
            </button>
          </div>

          {newlyIssuedKey ? (
            <p style={{ color: '#00d19a', marginTop: '10px' }}>
              New key (copy now): <strong>{newlyIssuedKey}</strong>
            </p>
          ) : null}

          {usage?.summary ? (
            <div style={{ marginTop: '10px', color: '#9aa4b2' }}>
              <div>Active Keys: {usage.summary.activeKeys}</div>
              <div>Total Requests: {usage.summary.totalRequests}</div>
              <div>Requests Today: {usage.summary.requestsToday}</div>
            </div>
          ) : (
            <p style={{ color: '#9aa4b2', marginTop: '10px' }}>No usage data loaded yet.</p>
          )}

          {Array.isArray(usage?.keys) && usage.keys.length > 0 ? (
            <ul style={{ marginTop: '10px', color: '#9aa4b2', paddingLeft: '18px' }}>
              {usage.keys.map((key) => (
                <li key={key.id}>
                  <strong style={{ color: '#e6eef6' }}>{key.name}</strong>
                  {' · prefix='}{key.keyPrefix}
                  {' · status='}{key.status}
                </li>
              ))}
            </ul>
          ) : null}

          {error ? <p style={{ color: '#ff7f7f', marginTop: '10px' }}>{error}</p> : null}
        </>
      )}
    </section>
  )
}
