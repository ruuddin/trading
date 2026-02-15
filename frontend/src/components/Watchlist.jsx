import React, { useState, useEffect } from 'react'
import { getLogoUrl, getInitialsBadge } from '../services/logoService'

export default function Watchlist({ 
  watchlists, 
  selectedWatchlist, 
  selectedSymbol,
  onSelectWatchlist,
  onSelectSymbol,
  onWatchlistCreated,
  onWatchlistUpdated,
  onWatchlistDeleted 
}) {
  const token = localStorage.getItem('token')
  const [prices, setPrices] = useState({})
  const [loading, setLoading] = useState(false)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showAddSymbolModal, setShowAddSymbolModal] = useState(false)
  const [newWatchlistName, setNewWatchlistName] = useState('')
  const [newSymbol, setNewSymbol] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [sortConfig, setSortConfig] = useState({ key: 'symbol', direction: 'asc' })
  const [hoveredSymbol, setHoveredSymbol] = useState(null)

  const formatPrice = (price) => {
    if (price === null || price === undefined || Number.isNaN(Number(price))) {
      return 'N/A'
    }
    return `$${Number(price).toFixed(2)}`
  }

  // Fetch live prices for symbols in current watchlist
  useEffect(() => {
    if (!selectedWatchlist?.symbols || selectedWatchlist.symbols.length === 0) {
      setPrices({})
      return
    }

    const fetchPrices = async () => {
      setLoading(true)
      const results = await Promise.all(
        selectedWatchlist.symbols.map(async (symbol) => {
          try {
            const response = await fetch(`/api/stocks/${symbol}/price`)
            if (!response.ok) {
              return [symbol, null]
            }
            const data = await response.json()
            const parsedPrice = Number(data?.price)
            return [symbol, Number.isNaN(parsedPrice) ? null : parsedPrice]
          } catch (err) {
            console.error(`Failed to fetch price for ${symbol}:`, err)
            return [symbol, null]
          }
        })
      )

      setPrices(Object.fromEntries(results))
      setLoading(false)
    }

    fetchPrices()
    // Refresh prices every 30 seconds
    const interval = setInterval(fetchPrices, 30000)
    return () => clearInterval(interval)
  }, [selectedWatchlist])

  const handleCreateWatchlist = async (e) => {
    e.preventDefault()
    if (!newWatchlistName.trim()) return

    try {
      const response = await fetch('/api/watchlists', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ name: newWatchlistName.trim() })
      })

      if (response.ok) {
        const data = await response.json()
        onWatchlistCreated(data)
        setNewWatchlistName('')
        setShowCreateModal(false)
      } else {
        const error = await response.text()
        setErrorMessage(error)
        alert('Error: ' + error)
      }
    } catch (err) {
      alert('Failed to create watchlist: ' + err.message)
    }
  }

  const handleRenameWatchlist = async (newName) => {
    if (!newName?.trim() || !selectedWatchlist) return

    try {
      const response = await fetch(`/api/watchlists/${selectedWatchlist.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ name: newName.trim() })
      })

      if (response.ok) {
        const data = await response.json()
        onWatchlistUpdated(data)
      } else {
        const error = await response.text()
        setErrorMessage(error)
        alert('Error: ' + error)
      }
    } catch (err) {
      alert('Failed to rename watchlist: ' + err.message)
    }
  }

  const handleAddSymbol = async (e) => {
    e.preventDefault()
    if (!newSymbol.trim() || !selectedWatchlist) return

    try {
      const response = await fetch(`/api/watchlists/${selectedWatchlist.id}/symbols`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ symbol: newSymbol.trim().toUpperCase() })
      })

      if (response.ok) {
        const data = await response.json()
        onWatchlistUpdated(data)
        setNewSymbol('')
        setShowAddSymbolModal(false)
        setErrorMessage('')
      } else {
        const error = await response.text()
        setErrorMessage(error)
        alert('Error: ' + error)
      }
    } catch (err) {
      setErrorMessage(err.message)
      alert('Failed to add symbol: ' + err.message)
    }
  }

  const handleRemoveSymbol = async (symbol) => {
    if (!selectedWatchlist) return

    try {
      const response = await fetch(`/api/watchlists/${selectedWatchlist.id}/symbols/${symbol}`, {
        method: 'DELETE',
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      })

      if (response.ok) {
        const data = await response.json()
        onWatchlistUpdated(data)
        if (selectedSymbol === symbol) {
          onSelectSymbol(data.symbols?.[0] || null)
        }
      }
    } catch (err) {
      alert('Failed to remove symbol: ' + err.message)
    }
  }

  const getSortedSymbols = () => {
    const symbols = [...(selectedWatchlist?.symbols || [])]

    const sorted = symbols.sort((left, right) => {
      if (sortConfig.key === 'price') {
        const leftPrice = Number(prices[left])
        const rightPrice = Number(prices[right])
        const leftValid = Number.isFinite(leftPrice)
        const rightValid = Number.isFinite(rightPrice)

        if (!leftValid && !rightValid) {
          return left.localeCompare(right)
        }
        if (!leftValid) return 1
        if (!rightValid) return -1
        return leftPrice - rightPrice
      }

      return left.localeCompare(right)
    })

    return sortConfig.direction === 'asc' ? sorted : sorted.reverse()
  }

  const sortedSymbols = getSortedSymbols()

  const toggleSort = (key) => {
    setSortConfig((current) => {
      if (current.key === key) {
        return {
          key,
          direction: current.direction === 'asc' ? 'desc' : 'asc'
        }
      }

      return { key, direction: key === 'symbol' ? 'asc' : 'desc' }
    })
  }

  const sortIndicator = (key) => {
    if (sortConfig.key !== key) return ''
    return sortConfig.direction === 'asc' ? ' ↑' : ' ↓'
  }

  const handleDeleteWatchlist = async () => {
    if (!selectedWatchlist || !window.confirm('Are you sure you want to delete this watchlist?')) return

    try {
      const response = await fetch(`/api/watchlists/${selectedWatchlist.id}`, {
        method: 'DELETE',
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
      })

      if (response.ok) {
        onWatchlistDeleted(selectedWatchlist.id)
      } else {
        const error = await response.text()
        setErrorMessage(error)
        alert('Error: ' + error)
      }
    } catch (err) {
      setErrorMessage(err.message)
      alert('Failed to delete watchlist: ' + err.message)
    }
  }

  if (!selectedWatchlist) {
    return (
      <div style={{ textAlign: 'center', padding: '40px', color: '#9aa4b2' }}>
        <p>No watchlists yet.</p>
        <button
          onClick={() => setShowCreateModal(true)}
          style={{
            padding: '10px 20px',
            background: '#00d19a',
            color: '#0f1419',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontWeight: 'bold'
          }}
        >
          Create Your First Watchlist
        </button>
        {showCreateModal && <CreateWatchlistModal onClose={() => setShowCreateModal(false)} onSubmit={handleCreateWatchlist} value={newWatchlistName} onChange={setNewWatchlistName} />}
      </div>
    )
  }

  return (
    <div style={{ height: '100%', fontSize: '13px' }}>
      {/* Watchlist Header and Controls */}
      <div style={{ marginBottom: '16px', background: '#0f1419', padding: '12px', borderRadius: '8px', border: '1px solid #2a3a52' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px', flexWrap: 'wrap' }}>
          {/* Watchlist Selector */}
          <select
            value={selectedWatchlist?.id || ''}
            onChange={(e) => {
              const w = watchlists.find(x => x.id == e.target.value)
              onSelectWatchlist(w)
            }}
            style={{
              padding: '8px',
              background: '#1a2332',
              color: '#e6eef6',
              border: '1px solid #2a3a52',
              borderRadius: '6px',
              cursor: 'pointer'
            }}
          >
            {watchlists.map(w => (
              <option key={w.id} value={w.id}>{w.name}</option>
            ))}
          </select>

          <div style={{ flex: 1 }} />

          <button
            onClick={() => {
              const nextName = window.prompt('Rename watchlist', selectedWatchlist.name)
              if (nextName !== null) {
                handleRenameWatchlist(nextName)
              }
            }}
            style={{
              padding: '6px 8px',
              background: '#2a3a52',
              color: '#e6eef6',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer'
            }}
            title="Rename watchlist"
            aria-label="Rename watchlist"
          >
            ✎
          </button>
          <button
            onClick={() => setShowCreateModal(true)}
            style={{
              padding: '6px 8px',
              background: '#2a3a52',
              color: '#e6eef6',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer'
            }}
            title="Create new watchlist"
            aria-label="Create new watchlist"
          >
            ≡+
          </button>

          {/* Control Buttons */}
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            <button
              onClick={() => setShowAddSymbolModal(true)}
              style={{
                width: '30px',
                height: '30px',
                background: '#00d19a',
                color: '#0f1419',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
              title="Add symbol"
              aria-label="Add symbol"
            >
              +
            </button>
            <button
              onClick={handleDeleteWatchlist}
              style={{
                width: '30px',
                height: '30px',
                background: '#8b3d3d',
                color: '#e6eef6',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer'
              }}
              title="Delete watchlist"
              aria-label="Delete watchlist"
            >
              🗑
            </button>
          </div>
        </div>

        {/* Count info */}
        <div style={{ color: '#9aa4b2', fontSize: '12px', marginBottom: '8px' }}>
          {selectedWatchlist.symbols?.length || 0} / 30 symbols • {watchlists.length} / 20 watchlists
        </div>

      </div>

      {/* Stocks Table */}
      {selectedWatchlist.symbols && selectedWatchlist.symbols.length > 0 ? (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #2a3a52' }}>
              <th
                onClick={() => toggleSort('symbol')}
                style={{ textAlign: 'left', padding: '12px 0', cursor: 'pointer', userSelect: 'none' }}
              >
                Symbol{sortIndicator('symbol')}
              </th>
              <th
                onClick={() => toggleSort('price')}
                style={{ textAlign: 'right', padding: '12px 0', cursor: 'pointer', userSelect: 'none' }}
              >
                Price{sortIndicator('price')}
              </th>
              <th style={{ textAlign: 'right', padding: '12px 0', width: '30px' }}></th>
            </tr>
          </thead>
          <tbody>
            {sortedSymbols.map(symbol => (
              <tr
                key={symbol}
                style={{
                  borderBottom: '1px solid #2a3a52',
                  backgroundColor: selectedSymbol === symbol ? '#1a2332' : 'transparent',
                  transition: 'background-color 0.2s'
                }}
                onMouseEnter={() => setHoveredSymbol(symbol)}
                onMouseLeave={() => setHoveredSymbol(null)}
                onClick={() => onSelectSymbol(symbol)}
              >
                <td
                  style={{
                    padding: '12px 0',
                    cursor: 'pointer',
                    color: '#00d19a',
                    fontWeight: 'bold',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px'
                  }}
                >
                  <img 
                    src={getLogoUrl(symbol)} 
                    alt={symbol}
                    style={{
                      width: '32px',
                      height: '32px',
                      borderRadius: '4px',
                      objectFit: 'contain',
                      background: '#1a2332',
                      padding: '2px'
                    }}
                    onError={(e) => {
                      const fallback = getInitialsBadge(symbol)
                      if (fallback) {
                        e.target.src = fallback
                      } else {
                        e.target.style.display = 'none'
                      }
                    }}
                  />
                  {symbol}
                </td>
                <td
                  style={{
                    textAlign: 'right',
                    padding: '12px 0',
                    cursor: 'pointer',
                    color: '#e6eef6'
                  }}
                >
                  {loading ? '-' : formatPrice(prices[symbol])}
                </td>
                <td
                  style={{
                    textAlign: 'right',
                    padding: '12px 0'
                  }}
                >
                  {hoveredSymbol === symbol && (
                    <button
                      onClick={(event) => {
                        event.stopPropagation()
                        handleRemoveSymbol(symbol)
                      }}
                      style={{
                        width: '22px',
                        height: '22px',
                        background: 'transparent',
                        color: '#ff8a8a',
                        border: '1px solid #8b3d3d',
                        borderRadius: '50%',
                        cursor: 'pointer',
                        fontSize: '14px',
                        lineHeight: '18px',
                        padding: 0
                      }}
                      aria-label={`Remove ${symbol}`}
                    >
                      ×
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <div style={{ textAlign: 'center', padding: '40px', color: '#9aa4b2' }}>
          <p>No symbols in this watchlist yet.</p>
          <button
            onClick={() => setShowAddSymbolModal(true)}
            style={{
              padding: '10px 20px',
              background: '#00d19a',
              color: '#0f1419',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              fontWeight: 'bold'
            }}
          >
            Add Your First Symbol
          </button>
        </div>
      )}

      {/* Modals */}
      {showCreateModal && (
        <CreateWatchlistModal 
          onClose={() => setShowCreateModal(false)} 
          onSubmit={handleCreateWatchlist}
          value={newWatchlistName}
          onChange={setNewWatchlistName}
        />
      )}

      {showAddSymbolModal && (
        <AddSymbolModal
          onClose={() => setShowAddSymbolModal(false)}
          onSubmit={handleAddSymbol}
          value={newSymbol}
          onChange={setNewSymbol}
          errorMessage={errorMessage}
        />
      )}
    </div>
  )
}

function CreateWatchlistModal({ onClose, onSubmit, value, onChange }) {
  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0, 0, 0, 0.7)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000
    }}>
      <div style={{
        background: '#0f1419',
        border: '1px solid #2a3a52',
        borderRadius: '8px',
        padding: '30px',
        width: '400px'
      }}>
        <h3 style={{ color: '#e6eef6', marginTop: 0 }}>Create New Watchlist</h3>
        <form onSubmit={onSubmit}>
          <input
            type="text"
            placeholder="Watchlist name"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            autoFocus
            style={{
              width: '100%',
              padding: '12px',
              background: '#1a2332',
              color: '#e6eef6',
              border: '1px solid #2a3a52',
              borderRadius: '6px',
              marginBottom: '20px',
              boxSizing: 'border-box'
            }}
          />
          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              type="submit"
              style={{
                flex: 1,
                padding: '10px',
                background: '#00d19a',
                color: '#0f1419',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              Create
            </button>
            <button
              type="button"
              onClick={onClose}
              style={{
                flex: 1,
                padding: '10px',
                background: '#2a3a52',
                color: '#e6eef6',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer'
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function AddSymbolModal({ onClose, onSubmit, value, onChange, errorMessage }) {
  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0, 0, 0, 0.7)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000
    }}>
      <div style={{
        background: '#0f1419',
        border: '1px solid #2a3a52',
        borderRadius: '8px',
        padding: '30px',
        width: '400px'
      }}>
        <h3 style={{ color: '#e6eef6', marginTop: 0 }}>Add Symbol to Watchlist</h3>
        <form onSubmit={onSubmit}>
          {/** show API error messages inline */}
          {value && errorMessage && (
            <div style={{ color: '#ff6b6b', marginBottom: '12px' }}>{errorMessage}</div>
          )}
          <input
            type="text"
            placeholder="Stock symbol (e.g., AAPL, MSFT)"
            value={value}
            onChange={(e) => onChange(e.target.value.toUpperCase())}
            autoFocus
            style={{
              width: '100%',
              padding: '12px',
              background: '#1a2332',
              color: '#e6eef6',
              border: '1px solid #2a3a52',
              borderRadius: '6px',
              marginBottom: '20px',
              boxSizing: 'border-box'
            }}
          />
          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              type="submit"
              style={{
                flex: 1,
                padding: '10px',
                background: '#00d19a',
                color: '#0f1419',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              Add
            </button>
            <button
              type="button"
              onClick={onClose}
              style={{
                flex: 1,
                padding: '10px',
                background: '#2a3a52',
                color: '#e6eef6',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer'
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
