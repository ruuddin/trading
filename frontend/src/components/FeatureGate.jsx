import React from 'react'
import { Link } from 'react-router-dom'

const TIER_ORDER = {
  FREE: 0,
  PRO: 1,
  PREMIUM: 2
}

export default function FeatureGate({ currentTier = 'FREE', requiredTier = 'PRO', fallback = null, children }) {
  const activeTier = String(currentTier || 'FREE').toUpperCase()
  const neededTier = String(requiredTier || 'PRO').toUpperCase()

  const activeRank = TIER_ORDER[activeTier] ?? 0
  const neededRank = TIER_ORDER[neededTier] ?? 1

  if (activeRank >= neededRank) {
    return children
  }

  if (fallback) {
    return fallback
  }

  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
      <span style={{ color: '#9aa4b2', fontSize: '12px' }}>Requires {neededTier}</span>
      <Link to="/pricing" style={{ color: '#00d19a', fontSize: '12px', textDecoration: 'none' }}>
        Upgrade
      </Link>
    </div>
  )
}
