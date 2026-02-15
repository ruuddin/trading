import React, { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import Watchlist from './components/Watchlist'
import StockDetail from './components/StockDetail'
import Login from './components/Login'
import ApiUsageLegend from './components/ApiUsageLegend'

export default function App() {
  const [watchlists, setWatchlists] = useState([])
  const [selectedWatchlist, setSelectedWatchlist] = useState(null)
  const [selectedSymbol, setSelectedSymbol] = useState(null)
  const [delayedSymbol, setDelayedSymbol] = useState(null)
  const [username, setUsername] = useState(localStorage.getItem('username'))
  const token = localStorage.getItem('token')

  // Fetch user's watchlists
  useEffect(() => {
    if (!username) return

    const fetchWatchlists = async () => {
      try {
        const response = await fetch('/api/watchlists', {
          headers: token ? { 'Authorization': `Bearer ${token}` } : {}
        })
        if (response.ok) {
          const data = await response.json()
          setWatchlists(data)
          
          // Select first watchlist if available
          if (data.length > 0 && !selectedWatchlist) {
            setSelectedWatchlist(data[0])
            const firstSymbol = data[0].symbols?.[0] || null
            setSelectedSymbol(firstSymbol)
            setDelayedSymbol(firstSymbol)
          }
        }
      } catch (err) {
        console.error('Failed to fetch watchlists:', err)
      }
    }

    fetchWatchlists()
  }, [username, token])

  function onLogin(un) {
    setUsername(un)
    localStorage.setItem('username', un)
  }

  const handleWatchlistCreated = (newWatchlist) => {
    setWatchlists([...watchlists, newWatchlist])
    setSelectedWatchlist(newWatchlist)
    setSelectedSymbol(newWatchlist.symbols?.[0] || null)
  }

  const handleWatchlistUpdated = (updatedWatchlist) => {
    setWatchlists(watchlists.map(w => w.id === updatedWatchlist.id ? updatedWatchlist : w))
    if (selectedWatchlist?.id === updatedWatchlist.id) {
      setSelectedWatchlist(updatedWatchlist)
      if (!updatedWatchlist.symbols?.includes(selectedSymbol)) {
        setSelectedSymbol(updatedWatchlist.symbols?.[0] || null)
      }
    }
  }

  const handleWatchlistDeleted = (deletedId) => {
    const filtered = watchlists.filter(w => w.id !== deletedId)
    setWatchlists(filtered)
    if (selectedWatchlist?.id === deletedId) {
      setSelectedWatchlist(filtered.length > 0 ? filtered[0] : null)
      setSelectedSymbol(filtered.length > 0 ? (filtered[0].symbols?.[0] || null) : null)
    }
  }

  const handleWatchlistSelected = (watchlist) => {
    setSelectedWatchlist(watchlist)
    setSelectedSymbol(watchlist?.symbols?.[0] || null)
  }

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setDelayedSymbol(selectedSymbol)
    }, 1000)

    return () => clearTimeout(timeoutId)
  }, [selectedSymbol])

  return (
    <BrowserRouter>
      <div style={{ padding: 20, paddingBottom: 200 }}>
        <header style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <h1 style={{ margin: 0 }}>Trading</h1>
          <nav>
            <Link to="/">Watchlist + Stock Detail</Link>
          </nav>
          <div style={{ marginLeft: 'auto' }}>
            {username ? <span>Signed in as {username}</span> : <Link to="/login">Login</Link>}
          </div>
        </header>

        <Routes>
          <Route 
            path="/" 
            element={
              <div style={{ display: 'flex', gap: '16px', minHeight: 'calc(100vh - 220px)' }}>
                <div style={{ flex: '0 0 10%', minWidth: '220px' }}>
                  <Watchlist 
                    watchlists={watchlists}
                    selectedWatchlist={selectedWatchlist}
                    selectedSymbol={selectedSymbol}
                    onSelectWatchlist={handleWatchlistSelected}
                    onSelectSymbol={setSelectedSymbol}
                    onWatchlistCreated={handleWatchlistCreated}
                    onWatchlistUpdated={handleWatchlistUpdated}
                    onWatchlistDeleted={handleWatchlistDeleted}
                  />
                </div>
                <div style={{ flex: '1 1 90%', minWidth: 0 }}>
                  <StockDetail key={delayedSymbol || 'AAPL'} symbolOverride={delayedSymbol || 'AAPL'} />
                </div>
              </div>
            } 
          />
          <Route path="/stock/:symbol" element={<StockDetail />} />
          <Route path="/login" element={<Login onLogin={onLogin} />} />
        </Routes>

        <ApiUsageLegend />
      </div>
    </BrowserRouter>
  )
}
