import React, { useState } from 'react'

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)

  function submit(e) {
    e.preventDefault()
    fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    }).then(r => {
      if (!r.ok) throw new Error('login failed')
      return r.json()
    }).then(data => {
      const token = data.token
      localStorage.setItem('token', token)
      localStorage.setItem('username', username)
      onLogin(username)
      setError(null)
    }).catch(err => setError('Invalid credentials'))
  }

  return (
    <form onSubmit={submit} style={{ maxWidth: 320 }}>
      <h2>Login</h2>
      <div>
        <label>Username</label>
        <input value={username} onChange={e => setUsername(e.target.value)} />
      </div>
      <div>
        <label>Password</label>
        <input type="password" value={password} onChange={e => setPassword(e.target.value)} />
      </div>
      <div>
        <button type="submit">Login</button>
      </div>
      {error && <div style={{ color: 'red' }}>{error}</div>}
    </form>
  )
}
