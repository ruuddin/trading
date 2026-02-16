package com.example.trading.service;

import com.example.trading.model.StockDataCache;
import com.example.trading.repository.StockDataCacheRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Multi-provider stock data fetcher with intelligent caching and fallback strategy
 * Cache Tiers:
 * 1. In-Memory Cache (5 minutes) - Fastest
 * 2. Database Cache (60 minutes) - Persistent
 * 
 * API Providers (in fallback order):
 * 1. Alpha Vantage (25 requests/day)
 * 2. Finnhub (500/day)
 * 3. Twelve Data (800/day)
 * 4. Massive (1000/day)
 * 5. Mock Data (unlimited)
 */
@Service
public class MultiProviderStockDataFetcher {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StockDataCacheRepository cacheRepository;
    private final ApiUsageTracker apiUsageTracker;
    
    @Value("${stock.api.key:demo}")
    private String alphaVantageKey;
    
    @Value("${finnhub.api.key:}")
    private String finnhubKey;
    
    @Value("${twelvedata.api.key:}")
    private String twelveDataKey;
    
    @Value("${massive.api.key:}")
    private String massiveKey;

    @Value("${app.circuit-breaker.provider.failure-threshold:3}")
    private int providerFailureThreshold;

    @Value("${app.circuit-breaker.provider.open-seconds:60}")
    private int providerOpenSeconds;
    
    // In-memory cache: 5 minutes
    private static final Map<String, MemoryCachedData> memoryCache = new ConcurrentHashMap<>();
    private static final long MEMORY_CACHE_DURATION_MS = 300000; // 5 minutes
    private static final int MAX_HISTORY_POINTS = 5000;
    private static final int MOCK_DATA_POINTS = 4000;
    private final Map<String, CircuitBreakerState> providerCircuitBreakers = new ConcurrentHashMap<>();
    
    @Autowired
    public MultiProviderStockDataFetcher(StockDataCacheRepository cacheRepository, 
                                         ApiUsageTracker apiUsageTracker) {
        this.cacheRepository = cacheRepository;
        this.apiUsageTracker = apiUsageTracker;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @PostConstruct
    public void init() {
        System.out.println("\n=== Multi-Provider Stock Data Fetcher Initialized ===");
        System.out.println("Cache Strategy:");
        System.out.println("  1. In-Memory Cache (5 min)");
        System.out.println("  2. Database Cache (60 min)");
        System.out.println("\nAPI Providers:");
        System.out.println("  1️⃣  Alpha Vantage - " + (alphaVantageKey != null && !alphaVantageKey.isEmpty() ? "✓ Configured" : "✗ Not configured"));
        System.out.println("  2️⃣  Finnhub - " + (finnhubKey != null && !finnhubKey.isEmpty() ? "✓ Configured" : "✗ Not configured"));
        System.out.println("  3️⃣  Twelve Data - " + (twelveDataKey != null && !twelveDataKey.isEmpty() ? "✓ Configured" : "✗ Not configured"));
        System.out.println("  4️⃣  Massive - " + (massiveKey != null && !massiveKey.isEmpty() ? "✓ Configured" : "✗ Not configured"));
        System.out.println("  5️⃣  Mock Data (Fallback) - ✓ Always Available");
        providerCircuitBreakers.put("ALPHA_VANTAGE", new CircuitBreakerState());
        providerCircuitBreakers.put("FINNHUB", new CircuitBreakerState());
        providerCircuitBreakers.put("TWELVEDATA", new CircuitBreakerState());
        providerCircuitBreakers.put("MASSIVE", new CircuitBreakerState());
        System.out.println("=====================================================\n");
    }
    
    /**
     * Main method to get historical data with intelligent caching
     */
    public List<HistoricalData> getHistoricalData(String symbol, String interval) {
        try {
            String cacheKey = symbol + "|" + interval;
            
            // Step 1: Check in-memory cache (5 minutes)
            MemoryCachedData memCached = memoryCache.get(cacheKey);
            if (memCached != null && !memCached.isExpired()) {
                System.out.println("✓ Cache HIT (Memory | 5min) for " + symbol);
                return new ArrayList<>(memCached.data());
            }
            
            // Step 2: Check database cache (60 minutes)
            Optional<StockDataCache> dbCached = cacheRepository.findValidCache(symbol, interval);
            if (dbCached.isPresent()) {
                System.out.println("✓ Cache HIT (Database | 60min) for " + symbol + " from provider: " + dbCached.get().getProvider());
                List<HistoricalData> data = parseJsonData(dbCached.get().getData());
                // Update in-memory cache
                memoryCache.put(cacheKey, new MemoryCachedData(data));
                return data;
            }
            
            System.out.println("⓪ Cache MISS for " + symbol + " - Fetching from API providers");
            
            // Step 3: Try API providers in order
            List<HistoricalData> data = fetchWithCircuitBreaker(
                "ALPHA_VANTAGE",
                interval,
                () -> tryAlphaVantage(symbol, interval)
            );
            if (isUsableData(data, interval)) {
                cacheData(symbol, interval, data, "ALPHA_VANTAGE");
                System.out.println("✓ Fetched " + data.size() + " records for " + symbol + " from Alpha Vantage");
                return data;
            }
            
            data = fetchWithCircuitBreaker(
                "FINNHUB",
                interval,
                () -> tryFinnhub(symbol, interval)
            );
            if (isUsableData(data, interval)) {
                cacheData(symbol, interval, data, "FINNHUB");
                System.out.println("✓ Fetched " + data.size() + " records for " + symbol + " from Finnhub");
                return data;
            }
            
            data = fetchWithCircuitBreaker(
                "TWELVEDATA",
                interval,
                () -> tryTwelveData(symbol, interval)
            );
            if (isUsableData(data, interval)) {
                cacheData(symbol, interval, data, "TWELVEDATA");
                System.out.println("✓ Fetched " + data.size() + " records for " + symbol + " from Twelve Data");
                return data;
            }
            
            data = fetchWithCircuitBreaker(
                "MASSIVE",
                interval,
                () -> tryMassive(symbol, interval)
            );
            if (isUsableData(data, interval)) {
                cacheData(symbol, interval, data, "MASSIVE");
                System.out.println("✓ Fetched " + data.size() + " records for " + symbol + " from Massive");
                return data;
            }
            
            System.out.println("⚠ All API providers exhausted, falling back to mock data for " + symbol);
            data = getMockData(symbol);
            // Don't cache mock data to force re-attempt next time
            return data;
            
        } catch (Exception e) {
            System.err.println("Error fetching data for " + symbol + ": " + e.getMessage());
            return getMockData(symbol);
        }
    }
    
    /**
     * Cache data in both database and memory
     */
    private void cacheData(String symbol, String interval, List<HistoricalData> data, String provider) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            
            // Save to database (60-minute TTL handled by StockDataCache entity)
            StockDataCache cache = new StockDataCache(symbol, interval, jsonData, provider);
            cacheRepository.save(cache);
            
            // Save to memory (5-minute TTL)
            String cacheKey = symbol + "|" + interval;
            memoryCache.put(cacheKey, new MemoryCachedData(data));
            
            System.out.println("💾 Cached " + data.size() + " records for " + symbol + " (Memory 5min + DB 60min)");
        } catch (Exception e) {
            System.err.println("❌ Error caching data: " + e.getMessage());
        }
    }
    
    /**
     * Parse JSON string back to HistoricalData list
     */
    private List<HistoricalData> parseJsonData(String json) {
        try {
            return Arrays.asList(objectMapper.readValue(json, HistoricalData[].class));
        } catch (Exception e) {
            System.err.println("❌ Error parsing cached JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Try Alpha Vantage API
     */
    private List<HistoricalData> tryAlphaVantage(String symbol, String interval) {
        if (alphaVantageKey == null || alphaVantageKey.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (!apiUsageTracker.canMakeRequest("ALPHA_VANTAGE")) {
            System.out.println("⚠ Alpha Vantage minute limit reached");
            return new ArrayList<>();
        }

        if (!apiUsageTracker.recordRequest("ALPHA_VANTAGE")) {
            System.out.println("⚠ Alpha Vantage daily limit reached");
            return new ArrayList<>();
        }
        
        try {
            // Map interval to Alpha Vantage function
            String function;
            if (interval.equals("weekly")) {
                function = "TIME_SERIES_WEEKLY";
            } else if (interval.equals("monthly")) {
                function = "TIME_SERIES_MONTHLY";
            } else {
                // Default to daily
                function = "TIME_SERIES_DAILY";
            }
            
            String url = String.format("https://www.alphavantage.co/query?function=%s&symbol=%s&apikey=%s", 
                function, symbol, alphaVantageKey);

            if ("TIME_SERIES_DAILY".equals(function)) {
                url = String.format(
                    "https://www.alphavantage.co/query?function=%s&symbol=%s&outputsize=full&apikey=%s",
                    function, symbol, alphaVantageKey
                );
            }
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            // Check for rate limit or errors
            if (root.has("Information") || root.has("Note") || root.has("Error Message")) {
                System.out.println("⚠ Alpha Vantage rate limit or error detected");
                return new ArrayList<>();
            }
            
            return parseAlphaVantageData(root);
            
        } catch (Exception e) {
            System.err.println("❌ Alpha Vantage failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Try Finnhub API
     */
    private List<HistoricalData> tryFinnhub(String symbol, String interval) {
        if (finnhubKey == null || finnhubKey.isEmpty()) {
            return new ArrayList<>();
        }

        if (!"daily".equals(interval)) {
            return new ArrayList<>();
        }
        
        if (!apiUsageTracker.recordRequest("FINNHUB")) {
            System.out.println("⚠ Finnhub daily limit reached");
            return new ArrayList<>();
        }
        
        try {
            String url = String.format(
                "https://finnhub.io/api/v1/quote?symbol=%s&token=%s",
                symbol, finnhubKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.has("error")) {
                System.out.println("❌ Finnhub Error: " + root.get("error").asText());
                return new ArrayList<>();
            }
            
            return parseFinnhubData(root, symbol);
            
        } catch (Exception e) {
            System.err.println("❌ Finnhub failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Try Twelve Data API
     */
    private List<HistoricalData> tryTwelveData(String symbol, String interval) {
        if (twelveDataKey == null || twelveDataKey.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (!apiUsageTracker.recordRequest("TWELVEDATA")) {
            System.out.println("⚠ Twelve Data daily limit reached");
            return new ArrayList<>();
        }
        
        try {
            String tdInterval = switch (interval) {
                case "weekly" -> "1week";
                case "monthly" -> "1month";
                default -> "1d";
            };
            int outputSize = switch (interval) {
                case "weekly" -> 1500;
                case "monthly" -> 1200;
                default -> 5000;
            };
            String url = String.format(
                "https://api.twelvedata.com/time_series?symbol=%s&interval=%s&outputsize=%d&apikey=%s",
                symbol, tdInterval, outputSize, twelveDataKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.has("status") && root.get("status").asText().equals("error")) {
                System.out.println("❌ Twelve Data Error: " + root.get("message").asText());
                return new ArrayList<>();
            }
            
            return parseTwelveDataResponse(root);
            
        } catch (Exception e) {
            System.err.println("❌ Twelve Data failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Try Massive API (new provider)
     */
    private List<HistoricalData> tryMassive(String symbol, String interval) {
        if (massiveKey == null || massiveKey.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (!apiUsageTracker.recordRequest("MASSIVE")) {
            System.out.println("⚠ Massive daily limit reached");
            return new ArrayList<>();
        }
        
        try {
            // Using Massive API endpoint (adjust based on actual API documentation)
            String url = String.format(
                "https://api.massive.com/v1/historical?symbol=%s&interval=%s&key=%s",
                symbol, interval, massiveKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.has("error")) {
                System.out.println("❌ Massive Error: " + root.get("error").asText());
                return new ArrayList<>();
            }
            
            return parseMassiveResponse(root);
            
        } catch (Exception e) {
            System.err.println("❌ Massive failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Parse Alpha Vantage response
     */
    private List<HistoricalData> parseAlphaVantageData(JsonNode root) {
        List<HistoricalData> data = new ArrayList<>();
        String timeSeriesKey = getTimeSeriesKey(root);
        if (timeSeriesKey != null && root.has(timeSeriesKey)) {
            JsonNode timeSeries = root.get(timeSeriesKey);
            
            timeSeries.fields().forEachRemaining(entry -> {
                try {
                    String timestamp = entry.getKey();
                    JsonNode ohlc = entry.getValue();
                    data.add(new HistoricalData(
                        timestamp,
                        new BigDecimal(ohlc.get("1. open").asText()),
                        new BigDecimal(ohlc.get("2. high").asText()),
                        new BigDecimal(ohlc.get("3. low").asText()),
                        new BigDecimal(ohlc.get("4. close").asText())
                    ));
                } catch (Exception e) {
                    // Skip malformed entries
                }
            });
        }
        data.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        return data.subList(0, Math.min(MAX_HISTORY_POINTS, data.size()));
    }

    private boolean isUsableData(List<HistoricalData> data, String interval) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        if ("daily".equals(interval)) {
            return data.size() >= 30;
        }
        return data.size() >= 2;
    }

    private List<HistoricalData> fetchWithCircuitBreaker(String provider, String interval, Supplier<List<HistoricalData>> fetcher) {
        if (!isProviderConfigured(provider) || !supportsInterval(provider, interval)) {
            return new ArrayList<>();
        }

        CircuitBreakerState state = providerCircuitBreakers.computeIfAbsent(provider, ignored -> new CircuitBreakerState());
        long now = System.currentTimeMillis();
        long openDurationMs = providerOpenSeconds * 1000L;
        synchronized (state) {
            if (state.open && now < state.openUntilEpochMs) {
                System.out.println("⚠ Circuit open for " + provider + " until " + new Date(state.openUntilEpochMs));
                return new ArrayList<>();
            }
            if (state.open && now >= state.openUntilEpochMs) {
                state.open = false;
                state.consecutiveFailures = 0;
            }
        }

        List<HistoricalData> data = fetcher.get();
        if (data != null && !data.isEmpty()) {
            markProviderSuccess(provider);
            return data;
        }

        markProviderFailure(provider, openDurationMs);
        return new ArrayList<>();
    }

    private void markProviderSuccess(String provider) {
        CircuitBreakerState state = providerCircuitBreakers.computeIfAbsent(provider, ignored -> new CircuitBreakerState());
        synchronized (state) {
            state.consecutiveFailures = 0;
            state.open = false;
            state.openUntilEpochMs = 0L;
        }
    }

    private void markProviderFailure(String provider, long openDurationMs) {
        CircuitBreakerState state = providerCircuitBreakers.computeIfAbsent(provider, ignored -> new CircuitBreakerState());
        synchronized (state) {
            state.consecutiveFailures++;
            if (state.consecutiveFailures >= providerFailureThreshold) {
                state.open = true;
                state.openUntilEpochMs = System.currentTimeMillis() + openDurationMs;
                state.consecutiveFailures = 0;
                System.out.println("⚠ Circuit opened for " + provider + " for " + providerOpenSeconds + "s");
            }
        }
    }

    private boolean isProviderConfigured(String provider) {
        return switch (provider) {
            case "ALPHA_VANTAGE" -> alphaVantageKey != null && !alphaVantageKey.isBlank();
            case "FINNHUB" -> finnhubKey != null && !finnhubKey.isBlank();
            case "TWELVEDATA" -> twelveDataKey != null && !twelveDataKey.isBlank();
            case "MASSIVE" -> massiveKey != null && !massiveKey.isBlank();
            default -> false;
        };
    }

    private boolean supportsInterval(String provider, String interval) {
        if ("FINNHUB".equals(provider)) {
            return "daily".equals(interval);
        }
        return true;
    }

    public Map<String, CircuitBreakerStatus> getProviderCircuitBreakerStatus() {
        Map<String, CircuitBreakerStatus> status = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        providerCircuitBreakers.forEach((provider, state) -> {
            synchronized (state) {
                boolean open = state.open && now < state.openUntilEpochMs;
                long remainingMs = open ? Math.max(0L, state.openUntilEpochMs - now) : 0L;
                status.put(provider, new CircuitBreakerStatus(open, state.consecutiveFailures, remainingMs));
            }
        });
        return status;
    }
    
    /**
     * Parse Finnhub response
     */
    private List<HistoricalData> parseFinnhubData(JsonNode root, String symbol) {
        List<HistoricalData> data = new ArrayList<>();
        try {
            BigDecimal close = new BigDecimal(root.get("c").asDouble());
            BigDecimal high = new BigDecimal(root.get("h").asDouble());
            BigDecimal low = new BigDecimal(root.get("l").asDouble());
            BigDecimal open = new BigDecimal(root.get("o").asDouble());
            
            long timestamp = root.get("t").asLong() * 1000;
            String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date(timestamp));
            
            data.add(new HistoricalData(date, open, high, low, close));
        } catch (Exception e) {
            System.err.println("❌ Error parsing Finnhub data: " + e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Parse Twelve Data response
     */
    private List<HistoricalData> parseTwelveDataResponse(JsonNode root) {
        List<HistoricalData> data = new ArrayList<>();
        try {
            JsonNode dataArray = null;
            if (root.has("data")) {
                dataArray = root.get("data");
            } else if (root.has("values")) {
                dataArray = root.get("values");
            }

            if (dataArray != null && dataArray.isArray()) {
                for (int i = 0; i < Math.min(MAX_HISTORY_POINTS, dataArray.size()); i++) {
                    JsonNode item = dataArray.get(i);
                    data.add(new HistoricalData(
                        item.get("datetime").asText(),
                        new BigDecimal(item.get("open").asText()),
                        new BigDecimal(item.get("high").asText()),
                        new BigDecimal(item.get("low").asText()),
                        new BigDecimal(item.get("close").asText())
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing Twelve Data: " + e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Parse Massive API response
     */
    private List<HistoricalData> parseMassiveResponse(JsonNode root) {
        List<HistoricalData> data = new ArrayList<>();
        try {
            JsonNode dataArray = root.has("data") ? root.get("data") : root;
            
            if (dataArray.isArray()) {
                for (int i = 0; i < Math.min(MAX_HISTORY_POINTS, dataArray.size()); i++) {
                    JsonNode item = dataArray.get(i);
                    data.add(new HistoricalData(
                        item.get("timestamp").asText(),
                        new BigDecimal(item.get("open").asText()),
                        new BigDecimal(item.get("high").asText()),
                        new BigDecimal(item.get("low").asText()),
                        new BigDecimal(item.get("close").asText())
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing Massive response: " + e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Generate mock data for stocks when all APIs are exhausted
     */
    private List<HistoricalData> getMockData(String symbol) {
        List<HistoricalData> data = new ArrayList<>();
        double basePrice = getSymbolBasePrice(symbol);
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        
        for (int i = 0; i < MOCK_DATA_POINTS; i++) {
            double volatility = (Math.random() - 0.5) * basePrice * 0.04;
            double open = basePrice + (Math.random() - 0.5) * basePrice * 0.02;
            double close = open + volatility;
            double high = Math.max(open, close) + Math.random() * basePrice * 0.015;
            double low = Math.min(open, close) - Math.random() * basePrice * 0.015;
            
            basePrice = close;
            
            data.add(new HistoricalData(
                sdf.format(cal.getTime()),
                new BigDecimal(String.format("%.2f", open)),
                new BigDecimal(String.format("%.2f", high)),
                new BigDecimal(String.format("%.2f", low)),
                new BigDecimal(String.format("%.2f", close))
            ));
            
            cal.add(Calendar.DATE, -1);
        }
        
        data.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));
        System.out.println("🎲 Generated mock data for " + symbol);
        return data;
    }
    
    private double getSymbolBasePrice(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "AAPL" -> 195.0;
            case "MSFT" -> 370.0;
            case "TSLA" -> 240.0;
            case "GOOGL" -> 140.0;
            case "AMZN" -> 180.0;
            case "NVDA" -> 890.0;
            case "META" -> 380.0;
            case "MU" -> 110.0;
            default -> 100.0;
        };
    }
    
    private String getTimeSeriesKey(JsonNode root) {
        // Check for different API response formats
        if (root.has("Time Series (Daily)")) return "Time Series (Daily)";
        if (root.has("Time Series (Weekly)")) return "Time Series (Weekly)";
        if (root.has("Time Series (Monthly)")) return "Time Series (Monthly)";
        if (root.has("Time Series Intraday (5min)")) return "Time Series Intraday (5min)";
        
        // Alpha Vantage new format (without parentheses)
        if (root.has("Daily Time Series")) return "Daily Time Series";
        if (root.has("Weekly Time Series")) return "Weekly Time Series";
        if (root.has("Monthly Time Series")) return "Monthly Time Series";
        
        // Fallback: search for any key containing "Time Series"
        List<String> keys = new ArrayList<>();
        root.fieldNames().forEachRemaining(keys::add);
        return keys.stream()
            .filter(k -> k.contains("Time Series") || k.contains("Intraday"))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * In-memory cached data with 5-minute TTL
     */
    private record MemoryCachedData(long timestamp, List<HistoricalData> data) {
        MemoryCachedData(List<HistoricalData> data) {
            this(System.currentTimeMillis(), new ArrayList<>(data));
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > MEMORY_CACHE_DURATION_MS;
        }
    }

    private static final class CircuitBreakerState {
        private int consecutiveFailures = 0;
        private boolean open = false;
        private long openUntilEpochMs = 0L;
    }

    public record CircuitBreakerStatus(boolean open, int consecutiveFailures, long openForMs) {}

    public record HistoricalData(String timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {}
}
