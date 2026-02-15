import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import Login from './Login'

describe('Login', () => {
  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('registers then logs user in', async () => {
    const onLogin = vi.fn()

    global.fetch = vi.fn()
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ username: 'qa_user' }) })
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ token: 'jwt-token' }) })

    render(<Login onLogin={onLogin} />)

    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'qa_user' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Pass123!' } })
    fireEvent.click(screen.getByRole('button', { name: 'Register' }))

    await waitFor(() => {
      expect(onLogin).toHaveBeenCalledWith('qa_user')
    })

    expect(global.fetch).toHaveBeenNthCalledWith(1, '/api/auth/register', expect.any(Object))
    expect(global.fetch).toHaveBeenNthCalledWith(2, '/api/auth/login', expect.any(Object))
    expect(localStorage.getItem('token')).toBe('jwt-token')
  })
})
