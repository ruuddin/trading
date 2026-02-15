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
    global.fetch = vi.fn(async (url) => {
      if (String(url).includes('/api/stocks/MSFT/price')) {
        return { ok: true, json: async () => ({ price: 474.24 }) }
      }
      if (String(url).includes('/api/stocks/AAPL/price')) {
        return { ok: true, json: async () => ({ price: 286.36 }) }
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
})
