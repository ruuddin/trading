# Commercialization Backlog (90 Days)

## Goal
Ship a revenue-ready trading app in 12 weeks with clear feature gating, monetization, retention loops, and operational readiness.

## Delivery Cadence
- Sprint length: 2 weeks
- Total: 6 sprints
- Priority: P0 (must), P1 (should), P2 (later)

---

## Sprint 1 (Weeks 1-2): Billing + Entitlements Foundation

### P0 Tasks
1. Add plan model and entitlement checks in backend
   - Files:
     - `backend/src/main/java/com/example/trading/model/User.java`
     - `backend/src/main/java/com/example/trading/controller/AuthController.java`
     - `backend/src/main/java/com/example/trading/config/SecurityConfig.java`
   - Add:
     - `PlanTier` enum (`FREE`, `PRO`, `PREMIUM`)
     - fields on user: `planTier`, `trialEndsAt`, `billingStatus`

2. Add billing webhook endpoint (Stripe-ready)
   - New files:
     - `backend/src/main/java/com/example/trading/controller/BillingController.java`
     - `backend/src/main/java/com/example/trading/service/BillingService.java`
   - Endpoints:
     - `POST /api/billing/webhook`
     - `GET /api/billing/me`

3. Frontend paywall shell + plan page
   - Files:
     - `frontend/src/App.jsx`
     - New `frontend/src/components/Pricing.jsx`
     - New `frontend/src/components/FeatureGate.jsx`

### Acceptance
- User has plan info retrievable from API.
- Feature gates hide premium controls for free users.
- Webhook updates plan state.

---

## Sprint 2 (Weeks 3-4): Real-time Data + Alerts v1

### P0 Tasks
1. Add websocket stream for live quotes
   - New files:
     - `backend/src/main/java/com/example/trading/config/WebSocketConfig.java`
     - `backend/src/main/java/com/example/trading/service/QuoteStreamService.java`
   - Endpoint/topic:
     - `/ws/quotes`

2. Add alerts model + CRUD
   - New files:
     - `backend/src/main/java/com/example/trading/model/AlertRule.java`
     - `backend/src/main/java/com/example/trading/repository/AlertRuleRepository.java`
     - `backend/src/main/java/com/example/trading/controller/AlertController.java`
   - Endpoints:
     - `GET /api/alerts`
     - `POST /api/alerts`
     - `DELETE /api/alerts/{id}`

3. Frontend alert UI
   - New files:
     - `frontend/src/components/AlertsPanel.jsx`
   - Update:
     - `frontend/src/components/StockDetail.jsx`
     - `frontend/src/components/Watchlist.jsx`

### Acceptance
- Watchlist/detail prices update via stream.
- User can create/delete basic threshold alerts.

---

## Sprint 3 (Weeks 5-6): Portfolio Analytics v1

### P0 Tasks
1. Add analytics service
   - New files:
     - `backend/src/main/java/com/example/trading/service/PortfolioAnalyticsService.java`
     - `backend/src/main/java/com/example/trading/controller/AnalyticsController.java`
   - Endpoints:
     - `GET /api/analytics/portfolio-summary`
     - `GET /api/analytics/performance?range=1M|3M|1Y|ALL`

2. Frontend analytics pages
   - New files:
     - `frontend/src/components/PortfolioAnalytics.jsx`
   - Update:
     - `frontend/src/App.jsx` routes/nav

### Acceptance
- User sees realized/unrealized P/L and simple benchmark comparison.

---

## Sprint 4 (Weeks 7-8): Screener + Retention Features

### P1 Tasks
1. Add screener endpoint and saved scans
   - New files:
     - `backend/src/main/java/com/example/trading/controller/ScreenerController.java`
     - `backend/src/main/java/com/example/trading/model/SavedScan.java`
     - `backend/src/main/java/com/example/trading/repository/SavedScanRepository.java`
   - Endpoints:
     - `GET /api/screener`
     - `POST /api/screener/saved`
     - `GET /api/screener/saved`

2. Frontend screener UI
   - New files:
     - `frontend/src/components/Screener.jsx`

3. Add onboarding checklist + activation events
   - Update:
     - `frontend/src/App.jsx`
     - New `frontend/src/services/analyticsService.js`

### Acceptance
- Users can run filters and save scans.
- Activation funnel events available in logs/analytics sink.

---

## Sprint 5 (Weeks 9-10): API Product + Team Features

### P1 Tasks
1. Developer API keys
   - New files:
     - `backend/src/main/java/com/example/trading/model/ApiKey.java`
     - `backend/src/main/java/com/example/trading/repository/ApiKeyRepository.java`
     - `backend/src/main/java/com/example/trading/controller/DeveloperApiController.java`
   - Endpoints:
     - `POST /api/dev/keys`
     - `GET /api/dev/usage`

2. Shared watchlists
   - New files:
     - `backend/src/main/java/com/example/trading/model/WatchlistShare.java`
     - `backend/src/main/java/com/example/trading/repository/WatchlistShareRepository.java`
   - Update:
     - `backend/src/main/java/com/example/trading/controller/TradingController.java`

3. Frontend shared workspace UI
   - New files:
     - `frontend/src/components/SharedWatchlists.jsx`

### Acceptance
- Premium users can generate API keys and inspect usage.
- Watchlists can be shared read-only with another user.

---

## Sprint 6 (Weeks 11-12): Commercial Readiness

### P0 Tasks
1. Security and compliance hardening
   - Add 2FA + session management
   - Add audit log model and endpoint (`/api/audit`)

2. Reliability/SLO
   - Add circuit breakers on external quote providers
   - Add rate limiting middleware for public routes
   - Add status endpoint dashboard docs

3. Legal/operations
   - Terms, Privacy, Risk disclosures pages
   - Production env checklist and incident runbook

### Acceptance
- Security baseline and operational docs are production-ready.

---

## Tests to Add per Sprint
- Backend: controller integration tests under `backend/src/test/java/com/example/trading/integration/`
- Frontend: component tests under `frontend/src/components/*.test.jsx`
- E2E: extend `frontend/e2e/app.usecases.spec.js` or split by feature:
  - `billing.usecases.spec.js`
  - `alerts.usecases.spec.js`
  - `analytics.usecases.spec.js`

---

## KPI Targets
- Activation: >60% users create first watchlist + first alert in 24h
- Conversion: >5% trial-to-paid by day 14
- Retention: D30 >25%
- Reliability: quote update success >99%, p95 API latency <500ms

---

## Immediate Next 5 Tasks (Start Tomorrow)
1. Create `PlanTier` on user and migration.
2. Build `GET /api/billing/me` endpoint.
3. Add `FeatureGate` component and gate one premium action.
4. Add Stripe webhook skeleton endpoint.
5. Add one integration test proving plan gate behavior.
