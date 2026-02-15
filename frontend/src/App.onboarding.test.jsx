import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import App from './App'

vi.mock('./components/Watchlist', () => ({
  default: () => <div>Watchlist</div>
}))

vi.mock('./components/StockDetail', () => ({
  default: () => <div>Stock Detail</div>
}))

vi.mock('./components/Login', () => ({ default: () => <div>Login</div> }))
vi.mock('./components/ApiUsageLegend', () => ({ default: () => null }))

describe('App onboarding checklist', () => {
  beforeEach(() => {
    localStorage.setItem('username', 'demo')
    localStorage.setItem('token', 'demo-token')

    global.fetch = vi.fn(async (url) => {
      if (String(url).includes('/api/watchlists')) {
        return {
          ok: true,
          json: async () => ([{ id: 1, name: 'Main', symbols: ['AAPL'] }])
        }
      }

      return {
        ok: true,
        json: async () => ({ planTier: 'FREE', billingStatus: 'TRIAL', trialActive: true })
      }
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('updates checklist when screener route is visited', async () => {
    render(<App />)

    await waitFor(() => {
      expect(screen.getByText('Completed 3/5')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('Screener'))

    await waitFor(() => {
      expect(screen.getByText('Completed 4/5')).toBeInTheDocument()
    })

    const events = JSON.parse(localStorage.getItem('activation_events') || '[]')
    expect(events.length).toBeGreaterThan(0)
  })
})
