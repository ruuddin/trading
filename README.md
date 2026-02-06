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

## Deploy to the Internet

### Railway (Recommended - 1 Click Deploy)

1. Go to [Railway.app](https://railway.app)
2. Sign up with GitHub
3. Click "Deploy from GitHub"
4. Select `ruuddin/trading` repository
5. Railway auto-detects and configures for Spring Boot
6. Click "Deploy" → Done! 🚀

**Your app will be live at:** `https://trading-<random>.railway.app`

The Procfile and railway.json handle all build/deploy config automatically.

### Render (Alternative)

1. Go to [Render.com](https://render.com)
2. Connect GitHub
3. Create new Web Service
4. Select `ruuddin/trading`
5. Build command: `mvn clean package -DskipTests`
6. Start command: `java -jar target/trading-0.0.1-SNAPSHOT.jar`
7. Deploy

### Local Production Build

```bash
mvn clean package -DskipTests
java -jar target/trading-0.0.1-SNAPSHOT.jar
```

## Monetization

This app is **ad-supported** with Google AdSense integration. Ads are displayed at strategic locations throughout the dashboard:
- Header ad banner
- Between chart and watchlist sections  
- Footer ad banner

### Setup Google AdSense

1. Sign up at [Google AdSense](https://www.google.com/adsense/)
2. Get your Publisher ID (format: `ca-pub-xxxxxxxxxxxxxxxx`)
3. Update the following files with your Publisher ID:
   - **`frontend/index.html`** - Replace `ca-pub-xxxxxxxxxxxxxxxx` in the AdSense script tag
   - **`frontend/src/components/AdBanner.jsx`** - Replace `ca-pub-xxxxxxxxxxxxxxxx` in the `data-ad-client` attribute
4. Rebuild and redeploy: `mvn clean package`

### Ad Configuration

You can customize ad slots in `AdBanner.jsx`:
```jsx
<AdBanner slot="header-ad" />    // 728x90 horizontal banner
<AdBanner slot="middle-ad" />    // Between sections
<AdBanner slot="footer-ad" />    // Bottom of page
```

Change ad format by modifying `data-ad-format` (e.g., `"rectangle"`, `"vertical"`, `"link"`)

## Tech Stack

- **Backend:** Spring Boot 3.2.2, Spring Data JPA, H2 Database, WebClient
- **Frontend:** React 18, Vite, Tailwind CSS, Recharts, Axios
- **Build:** Maven with frontend-maven-plugin

## Screenshots

- Watchlist sidebar with live prices
- Interactive price charts
- Stock ticker with 24h changes
- Responsive dashboard layout
