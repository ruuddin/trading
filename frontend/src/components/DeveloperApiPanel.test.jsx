import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import DeveloperApiPanel from './DeveloperApiPanel'

describe('DeveloperApiPanel', () => {
  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('shows sign-in hint when token is missing', () => {
    render(<DeveloperApiPanel planTier="PREMIUM" />)
    expect(screen.getByText('Sign in to manage developer API keys.')).toBeInTheDocument()
  })

  it('shows upgrade hint for non-premium plans', () => {
    localStorage.setItem('token', 'token')
    render(<DeveloperApiPanel planTier="FREE" />)
    expect(screen.getByText('Upgrade to PREMIUM to create and manage developer API keys.')).toBeInTheDocument()
  })

  it('loads usage and creates key for premium user', async () => {
    localStorage.setItem('token', 'token')

    global.fetch = vi.fn(async (url, options) => {
      const method = options?.method || 'GET'

      if (String(url) === '/api/dev/usage' && method === 'GET') {
        return {
          ok: true,
          json: async () => ({
            summary: { activeKeys: 1, totalRequests: 10, requestsToday: 2 },
            keys: [{ id: 1, name: 'Default Key', keyPrefix: 'trd_1234', status: 'ACTIVE' }]
          })
        }
      }

      if (String(url) === '/api/dev/keys' && method === 'POST') {
        return {
          ok: true,
          json: async () => ({ apiKey: 'trd_secret_key', keyPrefix: 'trd_1234', status: 'ACTIVE' })
        }
      }

      return { ok: false, status: 500, text: async () => 'unexpected request' }
    })

    render(<DeveloperApiPanel planTier="PREMIUM" />)

    await waitFor(() => {
      expect(screen.getByText('Active Keys: 1')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: 'Generate API Key' }))

    await waitFor(() => {
      expect(screen.getByText(/New key \(copy now\):/)).toBeInTheDocument()
      expect(screen.getByText('trd_secret_key')).toBeInTheDocument()
    })
  })
})
