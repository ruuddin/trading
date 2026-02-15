import React from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import Pricing from './Pricing'

describe('Pricing', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('token', 'test-token')
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('shows error when checkout request fails', async () => {
    global.fetch = vi.fn(async () => ({
      ok: false,
      status: 500,
      json: async () => ({})
    }))

    render(<Pricing entitlement={{ planTier: 'FREE', billingStatus: 'TRIAL', trialActive: true }} />)

    fireEvent.click(screen.getByRole('button', { name: 'Choose Pro' }))

    await waitFor(() => {
      expect(screen.getByText('Request failed (500)')).toBeInTheDocument()
    })
  })
})
