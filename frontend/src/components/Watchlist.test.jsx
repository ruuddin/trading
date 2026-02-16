import React from 'react'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import Watchlist from './Watchlist'

describe('Watchlist', () => {
  const baseProps = {
    watchlists: [{ id: 1, name: 'Main', symbols: ['MSFT', 'AAPL'] }],
    selectedWatchlist: { id: 1, name: 'Main', symbols: ['MSFT', 'AAPL'] },
    selectedSymbol: 'MSFT',
    onSelectWatchlist: vi.fn(),
    onSelectSymbol: vi.fn(),
    onWatchlistCreated: vi.fn(),
    onWatchlistUpdated: vi.fn(),
    onWatchlistDeleted: vi.fn()
  }

  beforeEach(() => {
    window.alert = vi.fn()
    global.fetch = vi.fn(async (url) => {
      if (String(url).includes('/api/stocks/MSFT/price')) {
        return { ok: true, json: async () => ({ price: 474.24, source: 'REFERENCE' }) }
      }
      if (String(url).includes('/api/stocks/AAPL/price')) {
        return { ok: true, json: async () => ({ price: 286.36, source: 'LIVE' }) }
      }
      return { ok: true, json: async () => ({}) }
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('uses icon actions and header sorting without sort dropdown', async () => {
    render(<Watchlist {...baseProps} />)

    await waitFor(() => {
      expect(screen.getByText('$474.24')).toBeInTheDocument()
      expect(screen.getByText('$286.36')).toBeInTheDocument()
      expect(screen.getByText('REFERENCE')).toBeInTheDocument()
      expect(screen.getByText('LIVE')).toBeInTheDocument()
    })

    expect(screen.queryByText('Sort')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Add symbol')).toBeInTheDocument()
    expect(screen.getByLabelText('Delete watchlist')).toBeInTheDocument()
    expect(screen.getByLabelText('Rename watchlist')).toBeInTheDocument()
    expect(screen.getByLabelText('Create new watchlist')).toBeInTheDocument()

    fireEvent.click(screen.getByText(/Price/))

    const rows = within(screen.getByRole('table')).getAllByRole('row')
    const firstBodyRow = rows[1]
    expect(within(firstBodyRow).getByText('MSFT')).toBeInTheDocument()
  })

  it('selects symbol when row is clicked', async () => {
    const onSelectSymbol = vi.fn()
    render(<Watchlist {...baseProps} onSelectSymbol={onSelectSymbol} />)

    await waitFor(() => expect(screen.getByText('AAPL')).toBeInTheDocument())

    fireEvent.click(screen.getByText('AAPL'))
    expect(onSelectSymbol).toHaveBeenCalledWith('AAPL')
  })

  it('shows inline error when add symbol fails with duplicate message', async () => {
    const onWatchlistUpdated = vi.fn()
    global.fetch = vi.fn(async (url, options) => {
      if (String(url).includes('/api/stocks/MSFT/price')) {
        return { ok: true, json: async () => ({ price: 474.24, source: 'REFERENCE' }) }
      }
      if (String(url).includes('/api/stocks/AAPL/price')) {
        return { ok: true, json: async () => ({ price: 286.36, source: 'LIVE' }) }
      }
      if (String(url).includes('/api/watchlists/1/symbols') && options?.method === 'POST') {
        return { ok: false, text: async () => 'Symbol already in watchlist' }
      }
      return { ok: true, json: async () => ({}) }
    })

    render(<Watchlist {...baseProps} onWatchlistUpdated={onWatchlistUpdated} />)

    fireEvent.click(screen.getByLabelText('Add symbol'))
    fireEvent.change(screen.getByPlaceholderText('Stock symbol (e.g., AAPL, MSFT)'), {
      target: { value: 'MSFT' }
    })
    fireEvent.click(screen.getByRole('button', { name: 'Add' }))

    await waitFor(() => {
      expect(screen.getByText('Symbol already in watchlist')).toBeInTheDocument()
    })

    expect(onWatchlistUpdated).not.toHaveBeenCalled()
  })
})
