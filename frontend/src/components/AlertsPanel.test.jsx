import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import AlertsPanel from './AlertsPanel'

describe('AlertsPanel', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('token', 'test-token')
    global.fetch = vi.fn(async (url, options = {}) => {
      if (String(url).startsWith('/api/alerts?symbol=')) {
        return {
          ok: true,
          json: async () => []
        }
      }

      if (String(url) === '/api/alerts' && options.method === 'POST') {
        return {
          ok: true,
          json: async () => ({
            id: 1,
            symbol: 'MSFT',
            conditionType: 'ABOVE',
            targetPrice: 300
          })
        }
      }

      return {
        ok: true,
        json: async () => ({ deleted: true })
      }
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('creates alert for active symbol', async () => {
    render(<AlertsPanel symbol="MSFT" />)

    await waitFor(() => {
      expect(screen.getByText('No alerts for MSFT yet.')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('Target price'), {
      target: { value: '300' }
    })

    fireEvent.click(screen.getByRole('button', { name: 'Add Alert' }))

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/alerts', expect.objectContaining({ method: 'POST' }))
    })
  })

  it('shows auth hint when user is not logged in', () => {
    localStorage.removeItem('token')
    render(<AlertsPanel symbol="MSFT" />)
    expect(screen.getByText('Sign in to create price alerts.')).toBeInTheDocument()
  })
})
