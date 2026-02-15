# Multi-Provider Stock Data System

## Overview
The Trading App now includes a smart multi-provider stock data fetching system that automatically switches between different APIs when rate limits are reached.

## Supported Data Providers

### 1. **Alpha Vantage** (Primary)
- **Free Tier:** 25 requests/day
- **Website:** https://www.alphavantage.co/api/
- **Status:** ✓ Configured in docker-compose.yml
- **Current API Key:** Provided via environment variable (`STOCK_API_KEY`)
- **Data Quality:** Excellent - Real market data
- **Supports:** Daily, Weekly, Monthly intervals

### 2. **Finnhub** (Secondary - Optional)
- **Free Tier:** 60 calls/min, 500/day
- **Website:** https://finnhub.io/
- **Status:** Not configured (empty API key)
- **Data Quality:** Excellent - Real market data
- **How to Enable:**
  ```bash
  # 1. Get free API key from https://finnhub.io/
  # 2. Update docker-compose.yml:
  FINNHUB_API_KEY: your_finnhub_api_key_here
  SPRING_FINNHUB_API_KEY: your_finnhub_api_key_here
  ```

### 3. **Twelve Data** (Tertiary - Optional)
- **Free Tier:** 800 calls/day
- **Website:** https://twelvedata.com/
- **Status:** Not configured (empty API key)
- **Data Quality:** Excellent - Real market data
- **How to Enable:**
  ```bash
  # 1. Get free API key from https://twelvedata.com/
  # 2. Update docker-compose.yml:
  TWELVEDATA_API_KEY: your_twelvedata_api_key_here
  SPRING_TWELVEDATA_API_KEY: your_twelvedata_api_key_here
  ```

### 4. **Mock Data Generator** (Fallback)
- **Free Tier:** Unlimited
- **Status:** Always available
- **Data Quality:** Simulated realistic market data
- **Used When:** All other providers are exhausted or rate limited

## How the Fallback System Works

The system automatically tries providers in this order:

```
Alpha Vantage (25 requests/day)
    ↓ If rate limited or error
Finnhub (500/day with free tier)
    ↓ If rate limited or error
Twelve Data (800/day with free tier)
    ↓ If rate limited or error
Mock Data Generator (infinite)
    ↓ Generates realistic historical data
JSON response with chart data
```

## Daily API Request Summary

| Provider | Requests/Day | Cumulative | Coverage |
|----------|-------------|-----------|----------|
| Alpha Vantage | 25 | 25 | 3-4 stocks |
| + Finnhub | 500 | 525 | 50+ stocks |
| + Twelve Data | 800 | 1,325 | 100+ stocks |
| + Mock Data | ∞ | ∞ | All stocks |

## Usage Examples

### Using with API keys configured:
```bash
# Can search any stock
# System automatically picks best provider
Search: "GOOGL" → Alpha Vantage (if requests available)
Search: "IBM" → Finnhub (if Alpha Vantage exhausted)
Search: "CUSTOM" → Twelve Data (if both exhausted)
Search: "ANY" → Mock Data (if all exhausted)
```

### Checking logs to see which provider is being used:
```bash
docker logs trading-backend-1 2>&1 | grep -E "✓|✗|⚠"

# Output examples:
✓ Fetched 100 records for AAPL from Alpha Vantage
✓ Fetched 100 records for MSFT from Finnhub
⚠ All providers exhausted, returning cached/mock data for AAPL
```

## To Add More API Providers

Edit `backend/src/main/java/com/example/trading/service/MultiProviderStockDataFetcher.java`:

1. Add API key property:
```java
@Value("${newapi.api.key:}")
private String newApiKey;
```

2. Add try method:
```java
private List<HistoricalData> tryNewApi(String symbol, String interval) {
    // Implementation
}
```

3. Add to getHistoricalData() fallback chain:
```java
data = tryNewApi(symbol, interval);
if (!data.isEmpty()) {
    System.out.println("✓ Fetched from New API");
    return data;
}
```

## Environment Variables

Update `docker-compose.yml` to add new API keys:
```yaml
backend:
  environment:
    STOCK_API_KEY: your_alpha_vantage_key
    FINNHUB_API_KEY: your_finnhub_key
    TWELVEDATA_API_KEY: your_twelve_data_key
```

## Rate Limit Management

### Current Implementation:
- **Alpha Vantage:** Tracks call count, stops at 24/25 to avoid hard limit
- **Finnhub:** Monitored for rate limit responses
- **Twelve Data:** Monitored for rate limit responses
- **Fallback:** Auto switches to next provider on any error

### Reset Schedule:
- **Alpha Vantage:** Resets at UTC 00:00
- **Finnhub:** Per-minute limit resets continuously
- **Twelve Data:** Resets at UTC 00:00

## Caching Strategy

Stock data is cached in browser `localStorage` with:
- **Cache Duration:** 1 hour
- **Invalidation:** Automatic after 1 hour or manual refresh
- **Benefits:** 
  - Reduces API calls
  - Faster page loads
  - Works offline

## Future Improvements

Planned additions:
- [ ] Polygon.io integration (free tier available)
- [ ] Yahoo Finance integration (no API key needed)
- [ ] Redis caching for distributed deployments
- [ ] API provider dashboard showing usage stats
- [ ] User-configurable provider preferences
- [ ] Webhook notifications for API limit warnings

## Troubleshooting

### All data showing mock data?
```bash
# Check logs for rate limit messages
docker logs trading-backend-1 | grep "rate limit\|exhausted"

# Solutions:
# 1. Wait 24 hours for daily limits to reset
# 2. Add Finnhub API key to enable secondary provider
# 3. Add Twelve Data API key for tertiary provider
```

### Specific API not working?
```bash
# Check initialization logs
docker logs trading-backend-1 | grep "Multi-Provider" -A 5

# Verify API keys are set in docker-compose.yml
docker inspect trading-backend-1 | grep -i "API_KEY"
```

### Want to monitor API usage?
```bash
# Watch provider selection in real-time
docker logs trading-backend-1 -f 2>&1 | grep -E "✓|✗|⚠|Fetched"
```
