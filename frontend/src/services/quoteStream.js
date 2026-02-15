export function subscribeToQuoteStream(symbols, onQuotes) {
  if (typeof window === 'undefined' || typeof window.WebSocket === 'undefined') {
    return () => {}
  }

  const uniqueSymbols = Array.from(
    new Set(
      (symbols || [])
        .map((symbol) => String(symbol || '').trim().toUpperCase())
        .filter(Boolean)
    )
  )

  if (uniqueSymbols.length === 0) {
    return () => {}
  }

  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const socket = new window.WebSocket(`${protocol}://${window.location.host}/ws/quotes`)

  socket.onopen = () => {
    socket.send(JSON.stringify({
      type: 'subscribe',
      symbols: uniqueSymbols
    }))
  }

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      if (payload?.type !== 'quotes' || !Array.isArray(payload?.quotes)) {
        return
      }
      onQuotes(payload.quotes)
    } catch (err) {
      console.error('Invalid quote stream payload:', err)
    }
  }

  socket.onerror = () => {
    // keep polling fallback path active in components
  }

  return () => {
    try {
      socket.close()
    } catch {
      // noop
    }
  }
}
