import React from 'react'
import { render, screen } from '@testing-library/react'
import TermsPage from './TermsPage'
import PrivacyPage from './PrivacyPage'
import RiskDisclosurePage from './RiskDisclosurePage'

describe('Legal pages', () => {
  it('renders terms content', () => {
    render(<TermsPage />)
    expect(screen.getByRole('heading', { name: 'Terms of Service' })).toBeInTheDocument()
    expect(screen.getByText(/No investment advice/i)).toBeInTheDocument()
  })

  it('renders privacy content', () => {
    render(<PrivacyPage />)
    expect(screen.getByRole('heading', { name: 'Privacy Policy' })).toBeInTheDocument()
    expect(screen.getByText(/Data retention/i)).toBeInTheDocument()
  })

  it('renders risk disclosure content', () => {
    render(<RiskDisclosurePage />)
    expect(screen.getByRole('heading', { name: 'Risk Disclosure' })).toBeInTheDocument()
    expect(screen.getByText(/Technology risk/i)).toBeInTheDocument()
  })
})
