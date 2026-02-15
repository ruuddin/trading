import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import StockDetail, { filterChartDataByInterval } from './StockDetail'

describe('StockDetail', () => {
  beforeEach(() => {
    const generatedData = Array.from({ length: 60 }, (_, index) => {
      const day = String((index % 28) + 1).padStart(2, '0')
      return {
        timestamp: `2020-01-${day}`,
        open: 300 + index,
        high: 301 + index,
        low: 299 + index,
        close: 300 + index
      }
    })

    global.fetch = vi.fn(async () => ({
      ok: true,
      json: async () => ({
        symbol: 'MSFT',
        interval: 'daily',
        data: generatedData
      })
    }))

    localStorage.clear()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders symbol details from override and does not show symbol search input', async () => {
    render(
      <MemoryRouter>
        <StockDetail symbolOverride="MSFT" />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'MSFT' })).toBeInTheDocument()
    })

    expect(screen.queryByPlaceholderText(/Search stocks/i)).not.toBeInTheDocument()
    expect(screen.getAllByText('OPEN')).toHaveLength(1)
  })

  it('still supports route mode for direct stock page', async () => {
    render(
      <MemoryRouter initialEntries={['/stock/AAPL']}>
        <Routes>
          <Route path="/stock/:symbol" element={<StockDetail />} />
        </Routes>
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'AAPL' })).toBeInTheDocument()
    })
  })

  it('filters fallback windows correctly for short intervals', () => {
    const points = Array.from({ length: 40 }, (_, index) => {
      const date = new Date(Date.UTC(2020, 0, 1 + index))
      return {
        timestamp: date.toISOString(),
        open: index,
        high: index + 1,
        low: index - 1,
        close: index
      }
    })

    const now = new Date('2026-01-01T00:00:00Z').getTime()

    expect(filterChartDataByInterval(points, '1D', now)).toHaveLength(1)
    expect(filterChartDataByInterval(points, '1W', now)).toHaveLength(5)
    expect(filterChartDataByInterval(points, '1M', now)).toHaveLength(22)
    expect(filterChartDataByInterval(points, '1Y', now)).toHaveLength(40)
  })

  it('updates interval window when interval buttons are clicked', async () => {
    render(
      <MemoryRouter>
        <StockDetail symbolOverride="MSFT" />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByTestId('interval-state')).toHaveTextContent('1M:22')
    })

    fireEvent.click(screen.getByRole('button', { name: '1D' }))
    await waitFor(() => {
      expect(screen.getByTestId('interval-state')).toHaveTextContent('1D:1')
    })

    fireEvent.click(screen.getByRole('button', { name: '1W' }))
    await waitFor(() => {
      expect(screen.getByTestId('interval-state')).toHaveTextContent('1W:5')
    })

    fireEvent.click(screen.getByRole('button', { name: '1Y' }))
    await waitFor(() => {
      expect(screen.getByTestId('interval-state')).toHaveTextContent('1Y:60')
    })
  })
})
