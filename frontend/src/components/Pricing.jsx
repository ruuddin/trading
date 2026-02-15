import React from 'react'

function PlanCard({ title, price, features, highlighted = false, isCurrentPlan = false, onSelectPlan, loading }) {
  const canUpgrade = !isCurrentPlan && typeof onSelectPlan === 'function'

  return (
    <div
      style={{
        border: highlighted ? '2px solid #00d19a' : '1px solid #2a3a52',
        borderRadius: '10px',
        padding: '18px',
        background: '#0f1419',
        minWidth: '220px',
        flex: '1 1 220px'
      }}
    >
      <h3 style={{ marginTop: 0, color: '#e6eef6' }}>{title}</h3>
      <div style={{ fontSize: '26px', fontWeight: 'bold', color: '#00d19a', marginBottom: '12px' }}>{price}</div>
      <ul style={{ margin: 0, paddingLeft: '18px', color: '#9aa4b2', lineHeight: 1.7 }}>
        {features.map((feature) => (
          <li key={feature}>{feature}</li>
        ))}
      </ul>

      <div style={{ marginTop: '14px' }}>
        {isCurrentPlan ? (
          <span style={{ color: '#00d19a', fontWeight: 'bold' }}>Current plan</span>
        ) : (
          <button
            type="button"
            disabled={!canUpgrade || loading}
            onClick={() => onSelectPlan(title.toUpperCase())}
            style={{
              background: '#00d19a',
              border: 'none',
              color: '#0f1419',
              fontWeight: 'bold',
              borderRadius: '8px',
              padding: '8px 12px',
              cursor: canUpgrade && !loading ? 'pointer' : 'not-allowed'
            }}
          >
            {loading ? 'Preparing...' : `Choose ${title}`}
          </button>
        )}
      </div>
    </div>
  )
}

export default function Pricing({ entitlement = { planTier: 'FREE', billingStatus: 'TRIAL', trialActive: true } }) {
  const [checkoutResult, setCheckoutResult] = React.useState(null)
  const [checkoutError, setCheckoutError] = React.useState('')
  const [loadingPlan, setLoadingPlan] = React.useState('')

  const planTier = entitlement?.planTier || 'FREE'
  const billingStatus = entitlement?.billingStatus || 'TRIAL'
  const trialActive = entitlement?.trialActive ?? true

  const handlePlanSelection = async (requestedPlan) => {
    setLoadingPlan(requestedPlan)
    setCheckoutError('')
    setCheckoutResult(null)

    try {
      const token = localStorage.getItem('token')
      const response = await fetch('/api/billing/checkout-session', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ planTier: requestedPlan })
      })

      if (!response.ok) {
        throw new Error(`Request failed (${response.status})`)
      }

      const data = await response.json()
      setCheckoutResult(data)
    } catch (error) {
      setCheckoutError(error?.message || 'Unable to start checkout')
    } finally {
      setLoadingPlan('')
    }
  }

  return (
    <div style={{ padding: '20px 0', color: '#e6eef6' }}>
      <h2 style={{ marginTop: 0 }}>Pricing</h2>
      <p style={{ color: '#9aa4b2' }}>
        Current Plan: <strong style={{ color: '#00d19a' }}>{planTier}</strong>
        {' · '}
        Billing Status: <strong style={{ color: '#00d19a' }}>{billingStatus}</strong>
        {' · '}
        Trial Active: <strong style={{ color: '#00d19a' }}>{trialActive ? 'Yes' : 'No'}</strong>
      </p>

      <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
        <PlanCard
          title="Free"
          price="$0/mo"
          isCurrentPlan={planTier === 'FREE'}
          onSelectPlan={handlePlanSelection}
          loading={loadingPlan === 'FREE'}
          features={[
            'Delayed/limited data',
            'Basic watchlist and charts',
            'Community support'
          ]}
        />

        <PlanCard
          title="Pro"
          price="$19/mo"
          highlighted
          isCurrentPlan={planTier === 'PRO'}
          onSelectPlan={handlePlanSelection}
          loading={loadingPlan === 'PRO'}
          features={[
            'Real-time quotes',
            'Advanced chart tools',
            'Alerts and notifications'
          ]}
        />

        <PlanCard
          title="Premium"
          price="$59/mo"
          isCurrentPlan={planTier === 'PREMIUM'}
          onSelectPlan={handlePlanSelection}
          loading={loadingPlan === 'PREMIUM'}
          features={[
            'Portfolio analytics',
            'Saved screeners and insights',
            'Priority support + API access'
          ]}
        />
      </div>

      {checkoutError ? (
        <p style={{ color: '#ff7f7f', marginTop: '14px' }}>{checkoutError}</p>
      ) : null}

      {checkoutResult ? (
        <p style={{ color: '#9aa4b2', marginTop: '14px' }}>
          Checkout placeholder ready: <strong style={{ color: '#00d19a' }}>{checkoutResult.status}</strong>
          {' · '}
          <a href={checkoutResult.checkoutUrl} style={{ color: '#00d19a' }}>Open checkout URL</a>
        </p>
      ) : null}
    </div>
  )
}
