import React, { useEffect, useState } from 'react'

export default function Orders() {
  const [orders, setOrders] = useState([])
  const [symbol, setSymbol] = useState('AAPL')
  const [quantity, setQuantity] = useState(1)
  const [side, setSide] = useState('BUY')

  const username = localStorage.getItem('username')
  const token = localStorage.getItem('token')

  function load() {
    if (!username || !token) return
    fetch('/api/orders', { headers: { 'Authorization': 'Bearer ' + token } })
      .then(r => r.json())
      .then(setOrders)
      .catch(() => setOrders([]))
  }

  useEffect(() => { load() }, [])

  function place(e) {
    e.preventDefault()
    if (!username || !token) return alert('login first')
    fetch('/api/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ symbol, quantity, side })
    }).then(r => r.json()).then(() => load())
  }

  return (
    <div>
      <h2>Your Orders</h2>
      <form onSubmit={place} style={{ marginBottom: 20 }}>
        <select value={symbol} onChange={e => setSymbol(e.target.value)}>
          <option>AAPL</option>
          <option>MSFT</option>
          <option>TSLA</option>
        </select>
        <input type="number" value={quantity} onChange={e => setQuantity(parseInt(e.target.value))} style={{ width: 80, marginLeft: 8 }} />
        <select value={side} onChange={e => setSide(e.target.value)} style={{ marginLeft: 8 }}>
          <option>BUY</option>
          <option>SELL</option>
        </select>
        <button style={{ marginLeft: 8 }} type="submit">Place</button>
      </form>

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr><th>Symbol</th><th>Qty</th><th>Price</th><th>Side</th><th>Status</th></tr>
        </thead>
        <tbody>
          {orders.map(o => (
            <tr key={o.id}><td>{o.symbol}</td><td>{o.quantity}</td><td>{o.price}</td><td>{o.side}</td><td>{o.status}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
