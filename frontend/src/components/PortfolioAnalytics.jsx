import React, { useEffect, useMemo, useState } from 'react'

const RANGE_OPTIONS = ['1M', '3M', '1Y', 'ALL']

export default function PortfolioAnalytics() {
  const [summary, setSummary] = useState(null)
  const [performance, setPerformance] = useState([])
  const [range, setRange] = useState('1M')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const token = localStorage.getItem('token')

  useEffect(() => {
    if (!token) return

    const fetchSummary = async () => {
      try {
        const response = await fetch('/api/analytics/portfolio-summary', {
          headers: { Authorization: `Bearer ${token}` }
        })
        if (!response.ok) throw new Error(`Failed to load summary (${response.status})`)
        const data = await response.json()
        setSummary(data)
      } catch (err) {
        setError(err?.message || 'Failed to load analytics summary')
      }
    }

    fetchSummary()
  }, [token])

  useEffect(() => {
    if (!token) return

    const fetchPerformance = async () => {
      setLoading(true)
      setError('')
      try {
        const response = await fetch(`/api/analytics/performance?range=${range}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        if (!response.ok) throw new Error(`Failed to load performance (${response.status})`)
        const data = await response.json()
        setPerformance(Array.isArray(data?.series) ? data.series : [])
      } catch (err) {
        setError(err?.message || 'Failed to load performance')
      } finally {
        setLoading(false)
      }
    }

    fetchPerformance()
  }, [token, range])

  const latestValue = useMemo(() => {
    if (performance.length === 0) return null
    const last = performance[performance.length - 1]
    return Number(last?.value)
  }, [performance])

  if (!token) {
    return (
      <div style={{ padding: '20px 0', color: '#9aa4b2' }}>
        Sign in to view portfolio analytics.
      </div>
    )
  }

  return (
    <div style={{ padding: '20px 0', color: '#e6eef6' }}>
      <h2 style={{ marginTop: 0 }}>Portfolio Analytics</h2>
      {error ? <p style={{ color: '#ff7f7f' }}>{error}</p> : null}

      {summary ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '10px', marginBottom: '14px' }}>
          <MetricCard label="Market Value" value={`$${Number(summary.marketValue).toFixed(2)}`} />
          <MetricCard label="Cost Basis" value={`$${Number(summary.costBasis).toFixed(2)}`} />
          <MetricCard label="Unrealized P/L" value={`$${Number(summary.unrealizedPnL).toFixed(2)}`} />
          <MetricCard label="Realized P/L" value={`$${Number(summary.realizedPnL).toFixed(2)}`} />
          <MetricCard label="Total Return" value={`${Number(summary.totalReturnPct).toFixed(2)}%`} />
          <MetricCard label="Positions" value={String(summary.positions)} />
        </div>
      ) : (
        <p style={{ color: '#9aa4b2' }}>Loading summary...</p>
      )}

      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '12px' }}>
        {RANGE_OPTIONS.map((option) => (
          <button
            key={option}
            type="button"
            onClick={() => setRange(option)}
            style={{
              padding: '8px 12px',
              borderRadius: '6px',
              border: range === option ? '2px solid #00d19a' : '1px solid #2a3a52',
              background: range === option ? 'rgba(0, 209, 154, 0.1)' : 'transparent',
              color: range === option ? '#00d19a' : '#9aa4b2',
              cursor: 'pointer',
              fontWeight: 'bold',
              fontSize: '12px'
            }}
          >
            {option}
          </button>
        ))}
      </div>

      <div style={{ border: '1px solid #2a3a52', borderRadius: '10px', padding: '14px', background: '#0f1419' }}>
        <h3 style={{ marginTop: 0, marginBottom: '10px', fontSize: '16px' }}>Performance ({range})</h3>
        {loading ? (
          <p style={{ color: '#9aa4b2', margin: 0 }}>Loading performance...</p>
        ) : performance.length === 0 ? (
          <p style={{ color: '#9aa4b2', margin: 0 }}>No performance data available.</p>
        ) : (
          <>
            <p style={{ color: '#00d19a', fontWeight: 'bold', marginTop: 0 }}>
              Latest value: ${latestValue?.toFixed(2)}
            </p>
            <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'grid', gap: '6px' }}>
              {performance.map((point) => (
                <li key={`${point.date}-${point.value}`} style={{ display: 'flex', justifyContent: 'space-between', color: '#9aa4b2' }}>
                  <span>{point.date}</span>
                  <span>${Number(point.value).toFixed(2)}</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
    </div>
  )
}

function MetricCard({ label, value }) {
  return (
    <div style={{ border: '1px solid #2a3a52', borderRadius: '8px', background: '#0f1419', padding: '12px' }}>
      <div style={{ color: '#9aa4b2', fontSize: '12px', marginBottom: '6px' }}>{label}</div>
      <div style={{ color: '#e6eef6', fontSize: '18px', fontWeight: 'bold' }}>{value}</div>
    </div>
  )
}
