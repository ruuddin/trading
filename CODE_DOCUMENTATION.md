# Trading App Code Documentation

## 1. System Overview

This repository contains a full-stack trading demo application:

- Frontend: React + Vite + Recharts (`frontend/`)
- Backend: Spring Boot (`backend/`)
- Database: MariaDB/H2 via Spring Data JPA
- Auth: JWT (Bearer token)
- Runtime: Docker Compose for db/backend/frontend

Primary user capabilities:

- Register/login
- Manage watchlists and symbols
- View live prices and historical charts
- Place and view orders
- View portfolio holdings
- View API provider usage metrics

## 2. Runtime Architecture

- Browser UI calls frontend routes and backend APIs through `/api`.
- Vite dev proxy and nginx container proxy route `/api` to backend `localhost:8080`.
- Backend validates JWT via `JwtFilter` and loads principal into `SecurityContext`.
- Data persistence is handled via Spring Data repositories.
- Historical stock data uses multi-provider fallback + memory/db cache.

## 3. Backend Package Documentation

### 3.1 Application Bootstrap

- `com.example.trading.TradingApplication`
  - Spring Boot entrypoint.

### 3.2 Security

- `config/SecurityConfig`
  - Configures CORS, CSRF exclusions, stateless sessions, JWT filter chain.
  - Permits auth, stock, metrics routes; requires auth for watchlists and most `/api/**` routes.
- `security/JwtFilter`
  - Reads `Authorization: Bearer <token>`.
  - Validates token and sets `UsernamePasswordAuthenticationToken`.
- `security/JwtUtil`
  - Generates, validates, and parses JWT tokens.

### 3.3 Controllers (HTTP API)

- `controller/AuthController`
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- `controller/TradingController`
  - Orders: `POST /api/orders`, `GET /api/orders`
  - Portfolio: `GET /api/portfolio`
  - Watchlists:
    - `GET /api/watchlists`
    - `POST /api/watchlists`
    - `GET /api/watchlists/{id}`
    - `PUT /api/watchlists/{id}`
    - `DELETE /api/watchlists/{id}`
    - `POST /api/watchlists/{id}/symbols`
    - `DELETE /api/watchlists/{id}/symbols/{symbol}`
- `controller/MarketController`
  - `GET /api/stocks`
  - `GET /api/stocks/{symbol}`
  - `GET /api/stocks/{symbol}/price`
  - `GET /api/stocks/{symbol}/history?interval=daily|weekly|monthly`
- `controller/MetricsController`
  - `GET /api/metrics/usage`
  - `GET /api/metrics/usage/{provider}`
  - `GET /api/metrics/rate-limited`
  - `GET /api/metrics/summary`

### 3.4 Services

- `service/SimpleStockPriceService`
  - Gets live quote from Yahoo Finance first, then Alpha Vantage fallback.
  - Uses in-memory quote cache (`5 min`).
- `service/MultiProviderStockDataFetcher`
  - Retrieves historical data using provider fallback order:
    1. Alpha Vantage
    2. Finnhub
    3. Twelve Data
    4. Massive
    5. Mock data fallback
  - Uses in-memory cache (`5 min`) + DB cache (`60 min`) via `StockDataCache`.
- `service/ApiUsageTracker`
  - Tracks daily/minute request usage and rate-limit status per provider.
- `service/StockDataFetcher`
  - Alternative/legacy Alpha Vantage fetcher utilities.

### 3.5 Domain Models

- `model/User` — user account with hashed password.
- `model/Stock` — stock symbol/name/reference price.
- `model/Order` — trade order record (buy/sell, qty, status).
- `model/Portfolio` — user holdings by symbol.
- `model/Watchlist` — named list of symbols for a user.
- `model/StockDataCache` — persisted historical cache payload + provider metadata.

### 3.6 Repositories

- `repository/UserRepository`
- `repository/StockRepository`
- `repository/OrderRepository`
- `repository/PortfolioRepository`
- `repository/WatchlistRepository`
- `repository/StockDataCacheRepository`

These use Spring Data JPA query methods for CRUD and filtered fetches.

### 3.7 Configuration and Resources

- `backend/src/main/resources/application.properties`
  - datasource, JWT secret, stock provider keys, logging, compression.
- `backend/src/main/resources/application-h2.properties`
  - alternate local H2 profile.

## 4. Frontend Documentation

### 4.1 App Shell and Routing

- `src/App.jsx`
  - Defines routes and shared app state for watchlists and login status.
  - `/` renders watchlist and stock detail together (10/90 split).
  - Uses delayed symbol synchronization (1 second) before updating detail view.
  - Routes:
    - `/` → `Watchlist`
    - `/stock/:symbol` → `StockDetail`
    - `/login` → `Login`

### 4.2 Components

- `components/Login.jsx`
  - Sends credentials to `/api/auth/login`.
  - Stores token in `localStorage`.
- `components/Watchlist.jsx`
  - Watchlist CRUD and symbol management.
  - Fetches per-symbol live prices from `/api/stocks/{symbol}/price`.
  - Supports sort-by-header interactions (`Symbol`, `Price`) with asc/desc toggles.
  - Uses icon-only actions for add/delete/rename/new-list.
  - Row delete is hover-only `×` icon.
- `components/StockDetail.jsx`
  - Fetches historical data and renders mountain/candlestick charts.
  - Includes custom candlestick renderer and OHLCV tooltip overlay.
  - Removes symbol search input in embedded watchlist-driven mode.
  - Removes lower OHLC summary cards below chart.
- `components/Orders.jsx`
  - Places and lists orders for authenticated user.
- `components/ApiUsageLegend.jsx`
  - Polls `/api/metrics/summary` and displays provider usage chips.

### 4.3 Utility Services

- `services/logoService.js`
  - Symbol logo URL helpers and fallbacks.

### 4.4 Styling and Build

- `src/index.css`
  - App theme tokens and base styles.
- `vite.config.js`
  - Dev server port `3000` and `/api` proxy to backend `8080`.

## 5. Data and Security Notes

- Protected endpoints use JWT bearer auth.
- Watchlists and orders rely on authenticated principal username.
- User passwords are hashed via BCrypt.
- Historical API usage and fallback logic are instrumented via metrics endpoints.

## 6. Known Functional Characteristics

- Historical chart volume is currently synthetic in frontend for visual support.
- Order execution price in `TradingController` uses `StockRepository` stored stock price, not guaranteed real-time quote.
- Multi-provider historical fallback may return mock data when all providers are exhausted.

## 7. Recommended Next Improvements

1. Align order execution price with live quote service at order time.
2. Replace synthetic chart volume with real provider volume where available.
3. Add integration tests for auth/watchlist/order flows.
4. Add OpenAPI/Swagger for endpoint contracts.
5. Introduce service interfaces to separate provider adapters cleanly.

## 8. Test Coverage

### Frontend

- Unit/integration tests (Vitest + Testing Library):
  - `frontend/src/components/Watchlist.test.jsx`
  - `frontend/src/components/StockDetail.test.jsx`
  - `frontend/src/App.integration.test.jsx`
- Browser E2E tests (Playwright, headed by default):
  - `frontend/e2e/app.usecases.spec.js`

### Backend

- Integration tests:
  - `backend/src/test/java/com/example/trading/integration/TradingApplicationIntegrationTest.java`
- API end-to-end tests:
  - `backend/src/test/java/com/example/trading/integration/BackendApiEndToEndTest.java`
  - Coverage includes:
    - auth register/login
    - protected endpoint authorization checks
    - watchlist CRUD + symbol validation
    - order creation/listing + portfolio quantity update
    - public market and metrics endpoints
