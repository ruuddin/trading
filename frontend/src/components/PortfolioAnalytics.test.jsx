import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import PortfolioAnalytics from './PortfolioAnalytics'

describe('PortfolioAnalytics', () => {
  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('shows sign-in hint when token is missing', () => {
    render(<PortfolioAnalytics />)
    expect(screen.getByText('Sign in to view portfolio analytics.')).toBeInTheDocument()
  })

  it('renders summary and performance data', async () => {
    localStorage.setItem('token', 'test-token')

    global.fetch = vi.fn(async (url) => {
      if (String(url).includes('/api/analytics/portfolio-summary')) {
        return {
          ok: true,
          json: async () => ({
            positions: 2,
            marketValue: 1000,
            costBasis: 900,
            unrealizedPnL: 100,
            realizedPnL: 50,
            totalPnL: 150,
            totalReturnPct: 11.11
          })
        }
      }

      return {
        ok: true,
        json: async () => ({
          range: '1M',
          series: [
            { date: '2026-01-01', value: 900 },
            { date: '2026-01-08', value: 950 },
            { date: '2026-01-15', value: 1000 }
          ]
        })
      }
    })

    render(<PortfolioAnalytics />)

    await waitFor(() => {
      expect(screen.getByText('Portfolio Analytics')).toBeInTheDocument()
      expect(screen.getByText('Latest value: $1000.00')).toBeInTheDocument()
      expect(screen.getByText('Market Value')).toBeInTheDocument()
      expect(screen.getAllByText('$1000.00').length).toBeGreaterThan(0)
    })

    fireEvent.click(screen.getByRole('button', { name: '3M' }))

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/analytics/performance?range=3M', expect.any(Object))
    })
  })
})
