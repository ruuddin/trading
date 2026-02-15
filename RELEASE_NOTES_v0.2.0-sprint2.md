# v0.2.0-sprint2

## Summary
Sprint 2 milestone focused on commercialization foundations and real-time UX upgrades. This release adds billing/entitlement primitives, alerts CRUD, websocket quote streaming, pricing/paywall UI, and expanded automated test coverage.

## Highlights
- Added billing foundation:
  - `PlanTier` model (`FREE`, `PRO`, `PREMIUM`)
  - User entitlement fields (`planTier`, `trialEndsAt`, `billingStatus`)
  - `GET /api/billing/me`
  - `POST /api/billing/webhook`
  - `POST /api/billing/checkout-session` placeholder
- Added alerts v1:
  - `GET /api/alerts`
  - `POST /api/alerts`
  - `DELETE /api/alerts/{id}`
  - New `AlertRule` model + repository + controller
- Added real-time quote stream:
  - WebSocket endpoint: `/ws/quotes`
  - Backend quote broadcast service with symbol subscriptions
  - Frontend websocket client integration in watchlist and stock detail
- Added pricing and feature gating UI:
  - New Pricing page and upgrade CTA flow
  - Feature-gated candlestick chart for non-Pro users
  - Checkout placeholder state handling
- Improved E2E/dev workflow:
  - Added websocket and pricing Playwright use-cases
  - Added demo Playwright config/script (`test:e2e:demo`)
  - Updated nginx websocket proxy configuration (`/ws/`)

## API Changes
### New Endpoints
- `GET /api/billing/me`
- `POST /api/billing/webhook`
- `POST /api/billing/checkout-session`
- `GET /api/alerts`
- `POST /api/alerts`
- `DELETE /api/alerts/{id}`
- `WS /ws/quotes`

### Behavior Notes
- `POST /api/billing/checkout-session` now validates `planTier` and returns `400` for invalid values.
- Candlestick chart action is locked for free tier in the UI.

## Testing
- Backend tests passing (JDK 25 runtime used for local validation).
- Frontend unit/integration tests passing.
- Full Playwright suite passing after websocket and selector stability fixes.

## Files of Interest
- Backend:
  - `backend/src/main/java/com/example/trading/controller/BillingController.java`
  - `backend/src/main/java/com/example/trading/service/BillingService.java`
  - `backend/src/main/java/com/example/trading/controller/AlertController.java`
  - `backend/src/main/java/com/example/trading/service/QuoteStreamService.java`
  - `backend/src/main/java/com/example/trading/config/WebSocketConfig.java`
- Frontend:
  - `frontend/src/components/Pricing.jsx`
  - `frontend/src/components/FeatureGate.jsx`
  - `frontend/src/components/AlertsPanel.jsx`
  - `frontend/src/services/quoteStream.js`
  - `frontend/e2e/app.usecases.spec.js`

## Tag
- `v0.2.0-sprint2`
