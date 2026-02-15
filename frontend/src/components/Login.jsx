import React, { useState } from 'react'

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [status, setStatus] = useState(null)
  const [busy, setBusy] = useState(false)

  async function login() {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })

    if (!response.ok) {
      throw new Error('login failed')
    }

    const data = await response.json()
    const token = data.token
    localStorage.setItem('token', token)
    localStorage.setItem('username', username)
    onLogin(username)
  }

  async function submit(e) {
    e.preventDefault()
    if (!username.trim() || !password) {
      setError('Username and password are required')
      return
    }

    setBusy(true)
    try {
      await login()
      setError(null)
      setStatus(null)
    } catch (err) {
      setError('Invalid credentials')
      setStatus(null)
    } finally {
      setBusy(false)
    }
  }

  async function register() {
    if (!username.trim() || !password) {
      setError('Username and password are required')
      return
    }

    setBusy(true)
    setError(null)
    setStatus(null)

    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      })

      if (!response.ok) {
        if (response.status === 409) {
          throw new Error('user exists')
        }
        throw new Error('register failed')
      }

      setStatus('Registration successful. Signing you in...')
      await login()
      setStatus(null)
    } catch (err) {
      if (err.message === 'user exists') {
        setError('User already exists. Try logging in instead.')
      } else {
        setError('Registration failed')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} style={{ maxWidth: 320 }}>
      <h2>Login</h2>
      <div>
        <label htmlFor="login-username">Username</label>
        <input id="login-username" value={username} onChange={e => setUsername(e.target.value)} />
      </div>
      <div>
        <label htmlFor="login-password">Password</label>
        <input id="login-password" type="password" value={password} onChange={e => setPassword(e.target.value)} />
      </div>
      <div>
        <button type="submit" disabled={busy}>Login</button>
        {' '}
        <button type="button" onClick={register} disabled={busy}>Register</button>
      </div>
      {error && <div style={{ color: 'red' }}>{error}</div>}
      {status && <div style={{ color: 'green' }}>{status}</div>}
    </form>
  )
}
