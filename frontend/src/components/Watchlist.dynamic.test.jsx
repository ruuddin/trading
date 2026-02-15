import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'

const quoteState = vi.hoisted(() => ({ onQuotes: null }))

vi.mock('../services/quoteStream', () => ({
  subscribeToQuoteStream: vi.fn((_symbols, onQuotes) => {
    quoteState.onQuotes = onQuotes
    return () => {
      quoteState.onQuotes = null
    }
  })
}))

import Watchlist from './Watchlist'

describe('Watchlist dynamic pricing', () => {
  const baseProps = {
    watchlists: [{ id: 1, name: 'Main', symbols: ['MSFT'] }],
    selectedWatchlist: { id: 1, name: 'Main', symbols: ['MSFT'] },
    selectedSymbol: 'MSFT',
    onSelectWatchlist: vi.fn(),
    onSelectSymbol: vi.fn(),
    onWatchlistCreated: vi.fn(),
    onWatchlistUpdated: vi.fn(),
    onWatchlistDeleted: vi.fn()
  }

  beforeEach(() => {
    global.fetch = vi.fn(async (url) => {
      if (String(url).includes('/api/stocks/MSFT/price')) {
        return { ok: true, json: async () => ({ price: 100.0, source: 'REFERENCE' }) }
      }
      return { ok: true, json: async () => ({}) }
    })
  })

  afterEach(() => {
    quoteState.onQuotes = null
    vi.clearAllMocks()
  })

  it('updates displayed price from quote stream messages', async () => {
    render(<Watchlist {...baseProps} />)

    await waitFor(() => {
      expect(screen.getByText('$100.00')).toBeInTheDocument()
      expect(screen.getByText('REFERENCE')).toBeInTheDocument()
    })

    act(() => {
      quoteState.onQuotes?.([{ symbol: 'MSFT', price: 123.45 }])
    })

    await waitFor(() => {
      expect(screen.getByText('$123.45')).toBeInTheDocument()
      expect(screen.getByText('LIVE')).toBeInTheDocument()
    })
  })
})
