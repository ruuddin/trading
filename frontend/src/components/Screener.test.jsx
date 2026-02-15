import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import Screener from './Screener'

describe('Screener', () => {
  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('shows sign-in hint without token', () => {
    render(<Screener />)
    expect(screen.getByText('Sign in to use screener and saved scans.')).toBeInTheDocument()
  })

  it('runs screener and saves/loads scans', async () => {
    localStorage.setItem('token', 'test-token')

    global.fetch = vi.fn(async (url, options) => {
      const method = options?.method || 'GET'

      if (String(url).startsWith('/api/screener?') && method === 'GET') {
        return {
          ok: true,
          json: async () => ({
            count: 1,
            results: [{ symbol: 'NVDA', name: 'NVIDIA', price: 450 }]
          })
        }
      }

      if (String(url) === '/api/screener/saved' && method === 'POST') {
        return {
          ok: true,
          json: async () => ({ id: 1, name: 'AI Momentum' })
        }
      }

      if (String(url) === '/api/screener/saved' && method === 'GET') {
        return {
          ok: true,
          json: async () => ([{ id: 1, name: 'AI Momentum', query: 'NV', minPrice: 100, maxPrice: 700 }])
        }
      }

      return { ok: false, status: 500, text: async () => 'unexpected request' }
    })

    render(<Screener />)

    fireEvent.change(screen.getByPlaceholderText('e.g. NVDA'), { target: { value: 'NV' } })
    fireEvent.change(screen.getByPlaceholderText('Saved scan name'), { target: { value: 'AI Momentum' } })

    fireEvent.click(screen.getByRole('button', { name: 'Run Screener' }))

    await waitFor(() => {
      expect(screen.getByText('Results: 1')).toBeInTheDocument()
      expect(screen.getByText('NVDA')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: 'Save Scan' }))

    await waitFor(() => {
      expect(screen.getByText('AI Momentum')).toBeInTheDocument()
    })
  })
})
