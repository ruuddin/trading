import React, { useEffect, useState } from 'react'

export default function AlertsPanel({ symbol }) {
  const [alerts, setAlerts] = useState([])
  const [conditionType, setConditionType] = useState('ABOVE')
  const [targetPrice, setTargetPrice] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const token = localStorage.getItem('token')

  const fetchAlerts = async () => {
    if (!token || !symbol) return

    try {
      setLoading(true)
      setError('')
      const response = await fetch(`/api/alerts?symbol=${encodeURIComponent(symbol)}`, {
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!response.ok) {
        throw new Error(`Failed to load alerts (${response.status})`)
      }

      const data = await response.json()
      setAlerts(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err?.message || 'Failed to load alerts')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchAlerts()
  }, [symbol, token])

  const createAlert = async () => {
    if (!token || !symbol) return

    const numericTarget = Number(targetPrice)
    if (!Number.isFinite(numericTarget) || numericTarget <= 0) {
      setError('Target price must be greater than 0')
      return
    }

    try {
      setError('')
      const response = await fetch('/api/alerts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          symbol,
          conditionType,
          targetPrice: numericTarget
        })
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || `Failed to create alert (${response.status})`)
      }

      setTargetPrice('')
      await fetchAlerts()
    } catch (err) {
      setError(err?.message || 'Failed to create alert')
    }
  }

  const deleteAlert = async (id) => {
    if (!token) return

    try {
      setError('')
      const response = await fetch(`/api/alerts/${id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!response.ok) {
        throw new Error(`Failed to delete alert (${response.status})`)
      }

      await fetchAlerts()
    } catch (err) {
      setError(err?.message || 'Failed to delete alert')
    }
  }

  if (!token) {
    return (
      <div style={{ marginTop: '18px', color: '#9aa4b2' }}>
        Sign in to create price alerts.
      </div>
    )
  }

  return (
    <div style={{ marginTop: '18px', border: '1px solid #2a3a52', borderRadius: '10px', padding: '14px', background: '#0f1419' }}>
      <h3 style={{ marginTop: 0, marginBottom: '12px', color: '#e6eef6' }}>Price Alerts</h3>

      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center', marginBottom: '12px' }}>
        <select
          value={conditionType}
          onChange={(e) => setConditionType(e.target.value)}
          aria-label="Alert condition"
          style={{
            background: '#101926',
            border: '1px solid #2a3a52',
            borderRadius: '6px',
            color: '#e6eef6',
            padding: '8px'
          }}
        >
          <option value="ABOVE">Above</option>
          <option value="BELOW">Below</option>
        </select>

        <input
          aria-label="Target price"
          type="number"
          min="0"
          step="0.01"
          placeholder="Target price"
          value={targetPrice}
          onChange={(e) => setTargetPrice(e.target.value)}
          style={{
            background: '#101926',
            border: '1px solid #2a3a52',
            borderRadius: '6px',
            color: '#e6eef6',
            padding: '8px',
            minWidth: '160px'
          }}
        />

        <button
          type="button"
          onClick={createAlert}
          style={{
            background: '#00d19a',
            border: 'none',
            color: '#0f1419',
            fontWeight: 'bold',
            borderRadius: '8px',
            padding: '8px 12px',
            cursor: 'pointer'
          }}
        >
          Add Alert
        </button>
      </div>

      {error ? <p style={{ color: '#ff7f7f', margin: '8px 0' }}>{error}</p> : null}

      {loading ? (
        <p style={{ color: '#9aa4b2', margin: 0 }}>Loading alerts...</p>
      ) : alerts.length === 0 ? (
        <p style={{ color: '#9aa4b2', margin: 0 }}>No alerts for {symbol} yet.</p>
      ) : (
        <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'grid', gap: '8px' }}>
          {alerts.map((alert) => (
            <li
              key={alert.id}
              style={{
                border: '1px solid #2a3a52',
                borderRadius: '8px',
                padding: '10px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                gap: '10px'
              }}
            >
              <span style={{ color: '#e6eef6' }}>
                {alert.symbol} {String(alert.conditionType).toLowerCase()} ${Number(alert.targetPrice).toFixed(2)}
              </span>
              <button
                type="button"
                aria-label={`Delete alert ${alert.id}`}
                onClick={() => deleteAlert(alert.id)}
                style={{
                  background: 'transparent',
                  border: '1px solid #2a3a52',
                  color: '#ff7f7f',
                  borderRadius: '6px',
                  padding: '4px 8px',
                  cursor: 'pointer'
                }}
              >
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
