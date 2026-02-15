# Trading App (React + Spring Boot + MariaDB)

This repository is a small trading app scaffold (watchlist + simple stock API) intended as a learning replica of a retail trading UI. It includes:

- Frontend: React + Vite (in `frontend/`)
- Backend: Spring Boot (in `backend/`) targeting Java 25
- Database: MariaDB (configure connection in `backend/src/main/resources/application.properties`)

Important notes
- The project `pom.xml` sets `<java.version>25`. You must have a JDK 25 installed to build and run the backend. If your environment does not support Java 25, change the `java.version` in `pom.xml` to a supported version and update the compiler plugin accordingly.
- Start a MariaDB server and create a database `tradingdb` or adjust `spring.datasource.url`.

Run backend (from project root):

```bash
mvn -f pom.xml spring-boot:run
```

Run frontend:

```bash
cd frontend
npm install
npm run dev
```

This scaffold is intentionally minimal. If you want, I can:
- Add authentication, orders, and portfolio models
- Add Docker Compose for MariaDB + backend + frontend
- Replace in-memory seed with market data ingestion

Authentication & Orders (demo)

This scaffold adds a simple authentication demo and order/portfolio models:

- Register: `POST /api/auth/register` with JSON `{ "username": "u", "password": "p" }`
- Login: `POST /api/auth/login` with JSON `{ "username": "u", "password": "p" }` (returns a demo token)
- Place order: `POST /api/orders` with header `Authorization: Bearer <token>` and body `{ "symbol":"AAPL","quantity":1,"side":"BUY" }`
- List orders: `GET /api/orders` with header `Authorization: Bearer <token>`
- Portfolio: `GET /api/portfolio` with header `Authorization: Bearer <token>`

The frontend includes a simple login form and an Orders page that uses JWT bearer authentication.

Docker (recommended)

Build and run everything with Docker Compose (recommended):

```bash
docker compose up --build
```

This starts MariaDB, the backend (port 8080) and the frontend served by nginx (port 3000).

## Current UI Behavior

- `/` renders **Watchlist (left)** and **Stock Detail (right)** on the same page.
- Selecting a symbol in watchlist updates stock detail after a **1-second delay**.
- Watchlist supports sorting by clicking table headers (`Symbol`, `Price`).
- Row-level remove action is an **`×` icon on hover** (no persistent `Remove` button).
- Stock detail symbol search input is removed in combined view; symbol source is watchlist selection.

## Test Coverage and Commands

Frontend (unit + integration):

```bash
cd frontend
npm run test
```

Frontend browser E2E (headed by default, slow interactions):

```bash
cd frontend
npm run test:e2e
```

Backend integration + API E2E:

```bash
cd backend
mvn test
```

Backend API E2E scenarios are covered in:

- `backend/src/test/java/com/example/trading/integration/TradingApplicationIntegrationTest.java`
- `backend/src/test/java/com/example/trading/integration/BackendApiEndToEndTest.java`

### API E2E Coverage Map

```mermaid
flowchart TD
	A[Auth: register/login] --> B[JWT protected requests]
	B --> C[Watchlist CRUD]
	C --> D[Symbol add/remove + validation]
	B --> E[Orders create/list]
	E --> F[Portfolio quantity updates]
	G[Public market endpoints] --> H[Price + history]
	I[Public metrics endpoint] --> J[Summary usage]
```

## Full Code Documentation

Detailed architecture and file-level documentation is available in:

- [CODE_DOCUMENTATION.md](CODE_DOCUMENTATION.md)

## Sequence Diagrams (All Major Use Cases)

### 1) User Registration

```mermaid
sequenceDiagram
	actor U as User
	participant FE as Frontend Login Page
	participant AC as AuthController
	participant UR as UserRepository
	participant DB as Database

	U->>FE: Submit username/password
	FE->>AC: POST /api/auth/register
	AC->>UR: findByUsername(username)
	UR->>DB: SELECT user
	DB-->>UR: none or existing
	alt user exists
		AC-->>FE: 409 user exists
	else new user
		AC->>UR: save(User with BCrypt hash)
		UR->>DB: INSERT user
		DB-->>UR: saved
		AC-->>FE: 200 { username }
	end
```

### 2) User Login (JWT)

```mermaid
sequenceDiagram
	actor U as User
	participant FE as Frontend Login Page
	participant AC as AuthController
	participant UR as UserRepository
	participant JU as JwtUtil

	U->>FE: Submit credentials
	FE->>AC: POST /api/auth/login
	AC->>UR: findByUsername(username)
	UR-->>AC: user + passwordHash
	AC->>AC: verify BCrypt password
	alt valid
		AC->>JU: generateToken(username)
		JU-->>AC: jwt
		AC-->>FE: 200 { token }
		FE->>FE: store token in localStorage
	else invalid
		AC-->>FE: 401 invalid credentials
	end
```

### 3) Authenticated Watchlist Fetch

```mermaid
sequenceDiagram
	actor U as User
	participant FE as App/Watchlist
	participant SF as Security Filter Chain + JwtFilter
	participant TC as TradingController
	participant WR as WatchlistRepository
	participant DB as Database

	U->>FE: Open app
	FE->>SF: GET /api/watchlists (Bearer token)
	SF->>SF: validate JWT + set principal
	SF->>TC: forward authenticated request
	TC->>WR: findByUserId(principal.id)
	WR->>DB: SELECT watchlists
	DB-->>WR: rows
	WR-->>TC: watchlists
	TC-->>FE: 200 [watchlists]
```

### 4) Watchlist CRUD + Symbol Management + Delayed Detail Sync

```mermaid
sequenceDiagram
	actor U as User
	participant FE as Watchlist + Detail UI
	participant TC as TradingController
	participant PS as SimpleStockPriceService
	participant MC as MarketController
	participant WR as WatchlistRepository
	participant DB as Database

	U->>FE: Create/Rename/Delete/Add/Remove Symbols
	FE->>TC: POST/PUT/DELETE /api/watchlists...
	alt add symbol
		TC->>PS: isValidSymbol(symbol)
		PS-->>TC: true/false
	end
	TC->>WR: save/update/delete watchlist
	WR->>DB: persist change
	DB-->>WR: result
	WR-->>TC: updated entity
	TC-->>FE: updated watchlist / status
	U->>FE: Click symbol row
	FE->>FE: wait 1 second
	FE->>MC: GET /api/stocks/{symbol}/history
	MC-->>FE: historical payload
```

### 5) Live Price Fetch for Watchlist Rows

```mermaid
sequenceDiagram
	actor U as User
	participant FE as Watchlist UI
	participant MC as MarketController
	participant SPS as SimpleStockPriceService
	participant YF as Yahoo Finance API
	participant AV as Alpha Vantage API

	U->>FE: Open watchlist
	loop each symbol (initial + every 30s)
		FE->>MC: GET /api/stocks/{symbol}/price
		MC->>SPS: getCurrentPrice(symbol)
		SPS->>SPS: check 5-min memory cache
		alt cache hit
			SPS-->>MC: cached price
		else cache miss
			SPS->>YF: quote request
			alt Yahoo success
				YF-->>SPS: regularMarketPrice/high/low
				SPS-->>MC: live price
			else Yahoo failed
				SPS->>AV: GLOBAL_QUOTE
				AV-->>SPS: quote or empty
				SPS-->>MC: live price or null
			end
		end
		MC-->>FE: price payload or 404
	end
```

### 6) Stock Detail Historical Chart (Fallback + Caching)

```mermaid
sequenceDiagram
	actor U as User
	participant FE as StockDetail UI
	participant MC as MarketController
	participant MP as MultiProviderStockDataFetcher
	participant SC as StockDataCacheRepository
	participant APIs as External Providers

	U->>FE: Open /stock/{symbol}
	FE->>MC: GET /api/stocks/{symbol}/history?interval=...
	MC->>MP: getHistoricalData(symbol, interval)
	MP->>MP: check memory cache (5m)
	alt memory miss
		MP->>SC: findValidCache(symbol, interval)
		alt db miss
			MP->>APIs: try Alpha->Finnhub->TwelveData->Massive
			alt all fail
				MP->>MP: generate mock historical data
			end
			MP->>SC: save cache (if provider data)
		end
	end
	MP-->>MC: historical series
	MC-->>FE: { symbol, interval, data[] }
```

### 7) Place Order and Update Portfolio

```mermaid
sequenceDiagram
	actor U as User
	participant FE as Orders UI
	participant SF as Security Filter Chain + JwtFilter
	participant TC as TradingController
	participant OR as OrderRepository
	participant PR as PortfolioRepository
	participant SR as StockRepository
	participant DB as Database

	U->>FE: Submit BUY/SELL order
	FE->>SF: POST /api/orders (Bearer token)
	SF->>TC: authenticated principal
	TC->>SR: findBySymbol(symbol)
	SR->>DB: SELECT stock
	DB-->>SR: stock price
	TC->>OR: save order(status=FILLED)
	OR->>DB: INSERT order
	TC->>PR: find/update portfolio position
	PR->>DB: INSERT/UPDATE portfolio
	TC-->>FE: 200 order
```

### 8) API Metrics Footer Polling

```mermaid
sequenceDiagram
	actor U as User
	participant FE as ApiUsageLegend
	participant MC as MetricsController
	participant AT as ApiUsageTracker

	U->>FE: Open app
	loop every 10 seconds
		FE->>MC: GET /api/metrics/summary
		MC->>AT: getAllMetrics()
		AT-->>MC: provider usage DTOs
		MC-->>FE: summary + providers
		FE->>FE: render usage chips/colors
	end
```

### 9) Read Orders and Portfolio

```mermaid
sequenceDiagram
	actor U as User
	participant FE as Orders/Portfolio UI
	participant SF as Security Filter Chain + JwtFilter
	participant TC as TradingController
	participant OR as OrderRepository
	participant PR as PortfolioRepository

	U->>FE: Open orders or portfolio page
	FE->>SF: GET /api/orders or GET /api/portfolio (Bearer token)
	SF->>TC: authenticated principal
	alt /api/orders
		TC->>OR: findByUserId(userId)
		OR-->>TC: orders
		TC-->>FE: 200 [orders]
	else /api/portfolio
		TC->>PR: findByUserId(userId)
		PR-->>TC: holdings
		TC-->>FE: 200 [portfolio]
	end
```
