import React, { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Link, useLocation } from 'react-router-dom'
import Watchlist from './components/Watchlist'
import StockDetail from './components/StockDetail'
import Login from './components/Login'
import ApiUsageLegend from './components/ApiUsageLegend'
import Pricing from './components/Pricing'
import PortfolioAnalytics from './components/PortfolioAnalytics'
import Screener from './components/Screener'
import SharedWatchlists from './components/SharedWatchlists'
import { trackActivationEvent } from './services/analyticsService'

export default function App() {
  const [watchlists, setWatchlists] = useState([])
  const [selectedWatchlist, setSelectedWatchlist] = useState(null)
  const [selectedSymbol, setSelectedSymbol] = useState(null)
  const [delayedSymbol, setDelayedSymbol] = useState(null)
  const [username, setUsername] = useState(localStorage.getItem('username'))
  const [entitlement, setEntitlement] = useState({ planTier: 'FREE', billingStatus: 'TRIAL', trialActive: true })
  const [visitedAnalytics, setVisitedAnalytics] = useState(localStorage.getItem('activation_visited_analytics') === '1')
  const [visitedScreener, setVisitedScreener] = useState(localStorage.getItem('activation_visited_screener') === '1')
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

  useEffect(() => {
    if (!username || !token) {
      setEntitlement({ planTier: 'FREE', billingStatus: 'TRIAL', trialActive: true })
      return
    }

    const fetchEntitlement = async () => {
      try {
        const response = await fetch('/api/billing/me', {
          headers: { 'Authorization': `Bearer ${token}` }
        })

        if (!response.ok) return
        const data = await response.json()
        const nextTier = typeof data?.planTier === 'string' ? data.planTier : 'FREE'
        const nextStatus = typeof data?.billingStatus === 'string' ? data.billingStatus : 'TRIAL'
        const nextTrialActive = typeof data?.trialActive === 'boolean' ? data.trialActive : true

        setEntitlement({
          planTier: nextTier,
          billingStatus: nextStatus,
          trialActive: nextTrialActive
        })
      } catch (err) {
        console.error('Failed to fetch entitlement:', err)
      }
    }

    fetchEntitlement()
  }, [username, token])

  function onLogin(un) {
    setUsername(un)
    localStorage.setItem('username', un)
    trackActivationEvent('user_signed_in', { username: un }, { onceKey: `signed_in_${un}` })
  }

  const handleWatchlistCreated = (newWatchlist) => {
    setWatchlists([...watchlists, newWatchlist])
    setSelectedWatchlist(newWatchlist)
    setSelectedSymbol(newWatchlist.symbols?.[0] || null)
    trackActivationEvent('watchlist_created', { watchlistId: newWatchlist?.id, symbolCount: newWatchlist?.symbols?.length || 0 }, { onceKey: 'watchlist_created' })
  }

  const handleWatchlistUpdated = (updatedWatchlist) => {
    setWatchlists(watchlists.map(w => w.id === updatedWatchlist.id ? updatedWatchlist : w))
    if ((updatedWatchlist?.symbols?.length || 0) > 0) {
      trackActivationEvent('symbol_added_to_watchlist', {
        watchlistId: updatedWatchlist?.id,
        symbolCount: updatedWatchlist?.symbols?.length || 0
      }, { onceKey: 'first_symbol_added' })
    }
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

  const hasWatchlist = watchlists.length > 0
  const hasSymbol = watchlists.some((watchlist) => (watchlist?.symbols?.length || 0) > 0)

  useEffect(() => {
    if (hasWatchlist) {
      trackActivationEvent('onboarding_watchlist_complete', { count: watchlists.length }, { onceKey: 'onboarding_watchlist_complete' })
    }
  }, [hasWatchlist, watchlists.length])

  useEffect(() => {
    if (hasSymbol) {
      trackActivationEvent('onboarding_symbol_complete', {}, { onceKey: 'onboarding_symbol_complete' })
    }
  }, [hasSymbol])

  return (
    <BrowserRouter>
      <div style={{ padding: 20, paddingBottom: 200 }}>
        <header style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <h1 style={{ margin: 0 }}>Trading</h1>
          <nav>
            <Link to="/">Watchlist + Stock Detail</Link>
            {' · '}
            <Link to="/pricing">Pricing</Link>
            {' · '}
            <Link to="/analytics">Analytics</Link>
            {' · '}
            <Link to="/screener">Screener</Link>
            {' · '}
            <Link to="/shared-watchlists">Shared Watchlists</Link>
          </nav>
          <div style={{ marginLeft: 'auto' }}>
            {username ? <span>Signed in as {username}</span> : <Link to="/login">Login</Link>}
          </div>
        </header>

        <ActivationTracker
          username={username}
          hasWatchlist={hasWatchlist}
          hasSymbol={hasSymbol}
          onVisitedAnalytics={() => setVisitedAnalytics(true)}
          onVisitedScreener={() => setVisitedScreener(true)}
        />

        <OnboardingChecklist
          signedIn={Boolean(username && token)}
          hasWatchlist={hasWatchlist}
          hasSymbol={hasSymbol}
          visitedAnalytics={visitedAnalytics}
          visitedScreener={visitedScreener}
        />

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
                  <StockDetail
                    key={delayedSymbol || 'AAPL'}
                    symbolOverride={delayedSymbol || 'AAPL'}
                    planTier={entitlement.planTier}
                  />
                </div>
              </div>
            } 
          />
          <Route path="/stock/:symbol" element={<StockDetail planTier={entitlement.planTier} />} />
          <Route path="/login" element={<Login onLogin={onLogin} />} />
          <Route path="/pricing" element={<Pricing entitlement={entitlement} />} />
          <Route path="/analytics" element={<PortfolioAnalytics />} />
          <Route path="/screener" element={<Screener />} />
          <Route path="/shared-watchlists" element={<SharedWatchlists />} />
        </Routes>

        <ApiUsageLegend />
      </div>
    </BrowserRouter>
  )
}

function ActivationTracker({ username, hasWatchlist, hasSymbol, onVisitedAnalytics, onVisitedScreener }) {
  const location = useLocation()

  useEffect(() => {
    trackActivationEvent('route_visited', { path: location.pathname })

    if (location.pathname === '/analytics') {
      localStorage.setItem('activation_visited_analytics', '1')
      trackActivationEvent('activation_analytics_visited', {}, { onceKey: 'activation_analytics_visited' })
      onVisitedAnalytics()
    }

    if (location.pathname === '/screener') {
      localStorage.setItem('activation_visited_screener', '1')
      trackActivationEvent('activation_screener_visited', {}, { onceKey: 'activation_screener_visited' })
      onVisitedScreener()
    }
  }, [location.pathname, onVisitedAnalytics, onVisitedScreener])

  useEffect(() => {
    if (username) {
      trackActivationEvent('activation_signed_in_seen', { username }, { onceKey: `activation_signed_in_seen_${username}` })
    }
  }, [username])

  useEffect(() => {
    if (hasWatchlist) {
      trackActivationEvent('activation_has_watchlist_seen', {}, { onceKey: 'activation_has_watchlist_seen' })
    }
  }, [hasWatchlist])

  useEffect(() => {
    if (hasSymbol) {
      trackActivationEvent('activation_has_symbol_seen', {}, { onceKey: 'activation_has_symbol_seen' })
    }
  }, [hasSymbol])

  return null
}

function OnboardingChecklist({ signedIn, hasWatchlist, hasSymbol, visitedAnalytics, visitedScreener }) {
  const steps = [
    { label: 'Sign in', done: signedIn },
    { label: 'Create first watchlist', done: hasWatchlist },
    { label: 'Add first symbol', done: hasSymbol },
    { label: 'Visit Analytics', done: visitedAnalytics },
    { label: 'Visit Screener', done: visitedScreener }
  ]

  const completed = steps.filter((step) => step.done).length

  return (
    <section style={{ marginTop: 14, marginBottom: 14, border: '1px solid #2a3a52', borderRadius: 10, padding: 12, background: '#0f1419' }}>
      <h2 style={{ margin: '0 0 8px 0', fontSize: 16, color: '#e6eef6' }}>Onboarding Checklist</h2>
      <div style={{ color: '#9aa4b2', marginBottom: 8 }}>Completed {completed}/{steps.length}</div>
      <ul style={{ margin: 0, paddingLeft: 18, color: '#9aa4b2', lineHeight: 1.6 }}>
        {steps.map((step) => (
          <li key={step.label} style={{ color: step.done ? '#00d19a' : '#9aa4b2' }}>
            {step.done ? '✓ ' : '○ '}{step.label}
          </li>
        ))}
      </ul>
    </section>
  )
}
