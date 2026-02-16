import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import SharedWatchlists from './SharedWatchlists'

describe('SharedWatchlists', () => {
  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('shows sign-in hint without token', () => {
    render(<SharedWatchlists />)
    expect(screen.getByText('Sign in to manage and view shared watchlists.')).toBeInTheDocument()
  })

  it('loads data and supports share and revoke actions', async () => {
    localStorage.setItem('token', 'test-token')

    global.fetch = vi.fn(async (url, options) => {
      const method = options?.method || 'GET'

      if (String(url) === '/api/watchlists' && method === 'GET') {
        return {
          ok: true,
          json: async () => ([{ id: 1, name: 'Main', symbols: ['MSFT'] }])
        }
      }

      if (String(url) === '/api/watchlists/shared' && method === 'GET') {
        return {
          ok: true,
          json: async () => ([{ id: 10, name: 'Team Ideas', symbols: ['NVDA'] }])
        }
      }

      if (String(url) === '/api/watchlists/1/share' && method === 'POST') {
        return {
          ok: true,
          json: async () => ({ watchlistId: 1, sharedWith: 'alice', shareId: 99, mode: 'READ_ONLY' })
        }
      }

      if (String(url) === '/api/watchlists/1/share/alice' && method === 'DELETE') {
        return {
          ok: true,
          json: async () => ({ deleted: true })
        }
      }

      return {
        ok: false,
        status: 500,
        text: async () => 'unexpected request'
      }
    })

    render(<SharedWatchlists />)

    await waitFor(() => {
      expect(screen.getByText('Team Ideas')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('Target username'), { target: { value: 'alice' } })
    fireEvent.click(screen.getByRole('button', { name: 'Share' }))

    await waitFor(() => {
      expect(screen.getByText('Shared with alice')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('Revoke watchlist id'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Revoke username'), { target: { value: 'alice' } })
    fireEvent.click(screen.getByRole('button', { name: 'Revoke' }))

    await waitFor(() => {
      expect(screen.getByText('Share revoked')).toBeInTheDocument()
    })
  })
})
