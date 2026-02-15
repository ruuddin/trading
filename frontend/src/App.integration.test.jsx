import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import App from './App'

vi.mock('./components/Watchlist', () => ({
  default: ({ onSelectSymbol }) => (
    <button onClick={() => onSelectSymbol('MSFT')}>select-msft</button>
  )
}))

vi.mock('./components/StockDetail', () => ({
  default: ({ symbolOverride }) => <div data-testid="stock-symbol">{symbolOverride}</div>
}))

vi.mock('./components/Login', () => ({ default: () => <div>Login</div> }))
vi.mock('./components/ApiUsageLegend', () => ({ default: () => null }))

describe('App integration', () => {
  beforeEach(() => {
    localStorage.setItem('username', 'demo')
    localStorage.setItem('token', 'demo-token')

    global.fetch = vi.fn(async () => ({
      ok: true,
      json: async () => ([{ id: 1, name: 'Main', symbols: ['AAPL', 'MSFT'] }])
    }))
  })

  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('updates stock detail symbol after 1-second delay when watchlist symbol changes', async () => {
    render(<App />)

    await waitFor(() => {
      expect(screen.getByTestId('stock-symbol')).toHaveTextContent('AAPL')
    })

    fireEvent.click(screen.getByText('select-msft'))

    expect(screen.getByTestId('stock-symbol')).toHaveTextContent('AAPL')

    await waitFor(
      () => {
        expect(screen.getByTestId('stock-symbol')).toHaveTextContent('MSFT')
      },
      { timeout: 2500 }
    )
  })
})
