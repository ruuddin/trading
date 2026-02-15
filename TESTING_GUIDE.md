# Trading App Testing Guide

This guide covers how to verify all major shipped features (auth, watchlists, trading, billing, alerts, realtime quotes, analytics, and screener APIs).

## 1) Prerequisites

- Docker + Docker Compose available
- Node.js + npm (for frontend test commands)
- JDK 25 available locally for direct backend test runs
- Repo root: `/Users/riazuddin/repos/vscode/trading`

## 2) Start the full stack

```bash
docker compose up -d --build
```

Services:
- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`

## 3) Automated test suites

### Backend (integration + API E2E)

```bash
cd backend
export JAVA_HOME=/Users/riazuddin/.sdkman/candidates/java/25.0.1-amzn
export PATH="$JAVA_HOME/bin:$PATH"
mvn test
```

### Frontend unit/integration

```bash
cd frontend
npm test -- --run
```

### Frontend browser E2E

```bash
cd frontend
npx playwright test
```

## 4) Manual feature verification

## 4.1 Auth + JWT

1. Open `http://localhost:3000/login`
2. Enter a new username/password and click `Register` (auto-sign-in)
3. If user already exists, click `Login`
3. Confirm header shows `Signed in as <username>`

API checks:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"qa_user","password":"Pass123!"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"qa_user","password":"Pass123!"}'
```

## 4.2 Watchlist + Symbol Management

1. On `/`, create a watchlist
2. Add symbols (`AAPL`, `MSFT`, `TSLA`)
3. Rename watchlist, sort by `Price`, remove a symbol
4. Delete watchlist and confirm switch behavior

API checks (authenticated):

```bash
# create watchlist
curl -X POST http://localhost:8080/api/watchlists \
  -H "Authorization: Bearer <TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{"name":"QA Watchlist"}'
```

## 4.3 Stock Detail + Chart Controls

1. Select symbol from watchlist and verify detail updates after ~1 second
2. Switch intervals (`1D`, `1W`, `1Y`) and verify chart refresh
3. Verify `Candlestick` button is gated/locked for free plan
4. In watchlist table, verify each symbol shows a price source label under price:
  - `LIVE` for provider/websocket data
  - `REFERENCE` when falling back to stored symbol price
  - `UNAVAILABLE` when neither source is available

## 4.4 Orders + Portfolio

1. Place a BUY order from UI/API
2. Verify order appears in order list
3. Verify portfolio position updates

API check:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{"symbol":"MSFT","quantity":2,"side":"BUY"}'
```

## 4.5 Billing + Entitlements + Pricing

1. Open `/pricing`
2. Confirm current plan/status/trial values are shown
3. Click `Choose Pro` and verify placeholder checkout response renders

API checks:

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/billing/me

curl -X POST http://localhost:8080/api/billing/checkout-session \
  -H "Authorization: Bearer <TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{"planTier":"PRO"}'

curl -X POST http://localhost:8080/api/billing/webhook \
  -H 'Content-Type: application/json' \
  -d '{"username":"qa_user","planTier":"PREMIUM","billingStatus":"active"}'
```

## 4.6 Alerts CRUD

1. In stock detail, use `Price Alerts` panel
2. Create an `ABOVE`/`BELOW` alert with target price
3. Confirm alert list updates
4. Delete alert and confirm removal

API checks:

```bash
curl -X POST http://localhost:8080/api/alerts \
  -H "Authorization: Bearer <TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{"symbol":"MSFT","conditionType":"ABOVE","targetPrice":300}'

curl -H "Authorization: Bearer <TOKEN>" \
  "http://localhost:8080/api/alerts?symbol=MSFT"
```

## 4.7 Realtime Quotes (WebSocket)

1. Keep `/` open with at least one symbol in watchlist
2. Verify prices update without waiting full 30s poll cycle

WebSocket smoke check:

```bash
curl -i http://localhost:8080/ws/quotes
```

Expected: `400 Can "Upgrade" only to "WebSocket"` (indicates endpoint is active and awaiting WS upgrade)

## 4.8 Portfolio Analytics

1. Open `/analytics`
2. Confirm summary cards render
3. Change range (`1M`, `3M`, `1Y`, `ALL`) and confirm performance section updates

API checks:

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/analytics/portfolio-summary

curl -H "Authorization: Bearer <TOKEN>" \
  "http://localhost:8080/api/analytics/performance?range=1M"
```

## 4.9 Screener + Saved Scans

1. Open `/screener`
2. Enter query/price filters and click `Run Screener`
3. Confirm result count and table rows render
4. Enter a saved scan name and click `Save Scan`
5. Click `Load Saved` and verify saved scan appears

API checks:

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  "http://localhost:8080/api/screener?query=NV&minPrice=100&maxPrice=700&limit=20"

curl -X POST http://localhost:8080/api/screener/saved \
  -H "Authorization: Bearer <TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{"name":"AI Momentum","query":"NV","minPrice":100,"maxPrice":700}'

curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/api/screener/saved
```

## 4.10 Onboarding Checklist + Activation Events

1. Sign in and confirm `Onboarding Checklist` appears on main page
2. Confirm progress updates as you complete actions:
  - Sign in
  - Create first watchlist
  - Add first symbol
  - Visit `/analytics`
  - Visit `/screener`
3. Verify activation events are written to browser storage:
  - Open DevTools Console and run:

```js
JSON.parse(localStorage.getItem('activation_events') || '[]')
```

Expected: event objects with names such as `route_visited`, `watchlist_created`, `screener_run`, `screener_saved`.

## 4.11 Developer API Keys + Usage (Premium)

1. Ensure user is PREMIUM (billing webhook or DB fixture)
2. Create developer API key with authenticated request
3. Confirm response includes one-time `apiKey`, `keyPrefix`, and `status`
4. Fetch usage and confirm summary + keys list are returned
5. Confirm FREE users receive `403`

API checks:

```bash
curl -X POST http://localhost:8080/api/dev/keys \
  -H "Authorization: Bearer <PREMIUM_TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Prod Key"}'

curl -H "Authorization: Bearer <PREMIUM_TOKEN>" \
  http://localhost:8080/api/dev/usage
```

## 5) Troubleshooting

- If API changes are not visible, rebuild containers:

```bash
docker compose up -d --build backend frontend
```

- If Playwright fails due stale state, clear browser/localStorage in test setup or run fresh:

```bash
cd frontend
npx playwright test --workers=1
```

- If backend Maven tool complains about Java 21/25 mismatch, run with explicit `JAVA_HOME` as shown above.
