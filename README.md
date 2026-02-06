# Stock Trading Dashboard

A modern Stock Trading dashboard built with Spring Boot backend and React/Vite frontend, featuring live price updates from Yahoo Finance and a TradingView-like UI.

## Features

- **React Dashboard** — TradingView-style interface with watchlists, charts, and price tickers
- **Stock Watchlist Management** — Create, edit, and delete watchlists
- **Live Price Updates** — Fetch real-time prices from Yahoo Finance API
- **H2 Database** — In-memory persistence for watchlists and stocks
- **REST API** — Full JSON API for all operations
- **Responsive Charts** — Interactive price charts with Recharts

## Project Structure

```
trading/
├── frontend/          # React + Vite frontend
│   ├── src/
│   │   ├── App.jsx
│   │   ├── components/
│   │   │   ├── Sidebar.jsx
│   │   │   ├── Chart.jsx
│   │   │   ├── PriceTicker.jsx
│   │   │   └── WatchlistPanel.jsx
│   │   └── main.jsx
│   ├── vite.config.js
│   ├── tailwind.config.js
│   └── package.json
├── src/main/java/    # Spring Boot backend
│   └── com/example/helloworld/
│       ├── model/
│       ├── repository/
│       ├── service/
│       └── controller/
└── pom.xml
```

## Build & Run

### Development (separate servers)

**Backend:**
```bash
cd /Users/riazuddin/trading
mvn spring-boot:run
```

**Frontend (in another terminal):**
```bash
cd /Users/riazuddin/trading/frontend
npm install
npm run dev
```

### Production (single JAR)

```bash
cd /Users/riazuddin/trading
mvn clean package
java -jar target/trading-0.0.1-SNAPSHOT.jar
```

Then open **http://localhost:8080/**

## API Endpoints

- `GET /api/watchlist` — List all watchlists
- `GET /api/watchlist/{id}` — Get watchlist by ID
- `POST /api/watchlist` — Create new watchlist
- `GET /api/prices?symbols=AAPL,MSFT` — Fetch prices for symbols
- `GET /h2-console` — H2 database console

## Tech Stack

- **Backend:** Spring Boot 3.2.2, Spring Data JPA, H2 Database, WebClient
- **Frontend:** React 18, Vite, Tailwind CSS, Recharts, Axios
- **Build:** Maven with frontend-maven-plugin

## Screenshots

- Watchlist sidebar with live prices
- Interactive price charts
- Stock ticker with 24h changes
- Responsive dashboard layout
