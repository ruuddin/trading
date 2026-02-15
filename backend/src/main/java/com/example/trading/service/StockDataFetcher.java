package com.example.trading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;

@Service
public class StockDataFetcher {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${stock.api.key:demo}")  // Use demo key for free tier
    private String apiKey;
    
    // Cache to avoid too many API calls
    private final Map<String, CachedData> cache = new HashMap<>();
    private static final long CACHE_DURATION_MS = 60000; // 1 minute
    
    public StockDataFetcher() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @PostConstruct
    public void init() {
        System.out.println("StockDataFetcher initialized with API Key: " + (apiKey != null && !apiKey.isEmpty() ? apiKey.substring(0, Math.min(8, apiKey.length())) + "***" : "NOT SET"));
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("demo")) {
            System.err.println("WARNING: Using default 'demo' API key. Please set STOCK_API_KEY environment variable for production use.");
        }
    }
    
    public CompactStockData getCurrentPrice(String symbol) {
        try {
            String url = String.format(
                "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                symbol, apiKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode quote = root.get("Global Quote");
            
            if (quote != null && quote.has("05. price")) {
                BigDecimal price = new BigDecimal(quote.get("05. price").asText());
                return new CompactStockData(symbol, price);
            }
        } catch (Exception e) {
            System.err.println("Error fetching price for " + symbol + ": " + e.getMessage());
        }
        
        return null;
    }
    
    public List<HistoricalData> getHistoricalData(String symbol, String interval) {
        try {
            // interval: 1min, 5min, 15min, 30min, 60min, daily, weekly, monthly
            String function = interval.equals("daily") ? "TIME_SERIES_DAILY" : "TIME_SERIES_INTRADAY";
            String params = interval.equals("daily") ? 
                String.format("?function=%s&symbol=%s&apikey=%s", function, symbol, apiKey) :
                String.format("?function=%s&symbol=%s&interval=%s&apikey=%s", function, symbol, interval, apiKey);
            
            String url = "https://www.alphavantage.co/query" + params;
            System.out.println("Fetching from: " + url.replaceAll(apiKey, "***"));
            
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("Response: " + response.substring(0, Math.min(200, response.length())));
            
            JsonNode root = objectMapper.readTree(response);
            
            // Check for API errors
            if (root.has("Information")) {
                System.out.println("API Info: " + root.get("Information").asText());
                return new ArrayList<>();
            }
            if (root.has("Note")) {
                System.out.println("API Rate Limit: " + root.get("Note").asText());
                return new ArrayList<>();
            }
            if (root.has("Error Message")) {
                System.out.println("API Error: " + root.get("Error Message").asText());
                return new ArrayList<>();
            }
            
            List<HistoricalData> data = new ArrayList<>();
            String timeSeriesKey = getTimeSeriesKey(root);
            
            if (timeSeriesKey != null && root.has(timeSeriesKey)) {
                JsonNode timeSeries = root.get(timeSeriesKey);
                timeSeries.fields().forEachRemaining(entry -> {
                    String timestamp = entry.getKey();
                    JsonNode ohlc = entry.getValue();
                    
                    try {
                        HistoricalData hd = new HistoricalData(
                            timestamp,
                            new BigDecimal(ohlc.get("1. open").asText()),
                            new BigDecimal(ohlc.get("2. high").asText()),
                            new BigDecimal(ohlc.get("3. low").asText()),
                            new BigDecimal(ohlc.get("4. close").asText())
                        );
                        data.add(hd);
                    } catch (Exception e) {
                        // Skip entries with missing data
                    }
                });
            } else {
                List<String> keys = new ArrayList<>();
                root.fieldNames().forEachRemaining(keys::add);
                System.out.println("No time series key found. Available keys: " + String.join(", ", keys));
            }
            
            // Sort by timestamp descending (most recent first)
            data.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
            return data.subList(0, Math.min(100, data.size())); // Return last 100 data points
            
        } catch (Exception e) {
            System.err.println("Error fetching historical data for " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return new ArrayList<>();
    }
    
    private String getTimeSeriesKey(JsonNode root) {
        // Find the time series key (varies based on function)
        for (String key : new String[]{"Time Series (Daily)", "Time Series (5min)", "Time Series (15min)", "Time Series (30min)", "Time Series (60min)", "Time Series"}) {
            if (root.has(key)) return key;
        }
        return null;
    }
    
    public record CompactStockData(String symbol, BigDecimal price) {}

    public record HistoricalData(String timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {}

    private record CachedData(long timestamp, Object data) {
        CachedData(Object data) {
            this(System.currentTimeMillis(), data);
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }
}
