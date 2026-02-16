import React from 'react'

export default function PrivacyPage() {
  return (
    <section style={{ maxWidth: 900 }}>
      <h2>Privacy Policy</h2>
      <p>
        We process account and usage data required to authenticate users, enforce access controls, and operate platform features.
      </p>
      <h3>Data collected</h3>
      <p>
        Data may include username, entitlement metadata, audit events, API usage metrics, and watchlist/trading records.
      </p>
      <h3>Data retention</h3>
      <p>
        Operational logs and audit records are retained based on environment policy and security incident response requirements.
      </p>
      <h3>Security controls</h3>
      <p>
        We implement authentication, session controls, rate limiting, and audit trails to protect platform integrity.
      </p>
    </section>
  )
}
