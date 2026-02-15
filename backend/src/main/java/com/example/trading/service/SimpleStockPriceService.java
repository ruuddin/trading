package com.example.trading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;
import java.util.*;

/**
 * Stock price service using live market quote APIs.
 * Returns null when no trustworthy live quote is available.
 */
@Service
public class SimpleStockPriceService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${stock.api.key:demo}")
    private String alphaVantageApiKey;
    
    // Real-time market data cache (per-session, not persistent)
    private static final Map<String, PriceData> priceCache = new HashMap<>();
    private static final long CACHE_DURATION_MS = 300000; // 5 minutes
    private static final Pattern TICKER_PATTERN = Pattern.compile("^[A-Z]{1,5}(\\.[A-Z]{1,2})?$");
    
    /**
     * Get current price for a stock symbol
     * Returns null if symbol is invalid
     */
    public StockPrice getCurrentPrice(String symbol) {
        String upperSymbol = normalizeSymbol(symbol);
        if (!isValidSymbol(upperSymbol)) {
            return null;
        }
        
        // Check cache first
        PriceData cached = priceCache.get(upperSymbol);
        if (cached != null && !cached.isExpired()) {
            return cached.price;
        }
        
        StockPrice yahooPrice = tryYahooQuote(upperSymbol);
        if (yahooPrice != null) {
            cachePrice(upperSymbol, yahooPrice);
            return yahooPrice;
        }

        // Try to fetch from Alpha Vantage - this validates if symbol is real
        StockPrice price = tryAlphaVantageQuote(upperSymbol);
        if (price != null) {
            cachePrice(upperSymbol, price);
            return price;
        }

        // No synthetic pricing: return null when live providers are unavailable.
        return null;

    }
        /**
         * Try to fetch live quote from Yahoo Finance (no API key required).
         */
        protected StockPrice tryYahooQuote(String symbol) {
            try {
                String url = String.format(
                    "https://query1.finance.yahoo.com/v7/finance/quote?symbols=%s",
                    symbol
                );

                String response = restTemplate.getForObject(url, String.class);
                if (response == null) return null;

                JsonNode root = objectMapper.readTree(response);
                JsonNode results = root.path("quoteResponse").path("result");

                if (!results.isArray() || results.isEmpty()) {
                    return null;
                }

                JsonNode quote = results.get(0);
                JsonNode marketPrice = quote.get("regularMarketPrice");
                if (marketPrice == null || marketPrice.isNull()) {
                    return null;
                }

                double price = marketPrice.asDouble();
                double high = quote.path("regularMarketDayHigh").asDouble(price);
                double low = quote.path("regularMarketDayLow").asDouble(price);

                return new StockPrice(
                    symbol,
                    new BigDecimal(String.format("%.2f", price)),
                    new BigDecimal(String.format("%.2f", high)),
                    new BigDecimal(String.format("%.2f", low)),
                    LocalDate.now().toString()
                );
            } catch (Exception e) {
                System.out.println("Yahoo quote fetch failed for " + symbol + ": " + e.getMessage());
                return null;
            }
        }

    
    public String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Validates ticker format for watchlist/order input.
     * Live quote availability is checked separately when fetching price.
     */
    public boolean isValidSymbol(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized.isEmpty()) {
            return false;
        }
        return TICKER_PATTERN.matcher(normalized).matches();
    }
    
    /**
     * Try to fetch live data from Alpha Vantage using global quote endpoint
     */
    protected StockPrice tryAlphaVantageQuote(String symbol) {
        try {
            String url = String.format(
                "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                symbol,
                alphaVantageApiKey
            );
            
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return null;
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode quote = root.path("Global Quote");
            
            if (quote == null || !quote.has("05. price") || quote.size() == 0) {
                return null;
            }
            
            String priceStr = quote.path("05. price").asText();
            String highStr = quote.path("03. high").asText();
            String lowStr = quote.path("04. low").asText();
            
            if (priceStr.isEmpty()) {
                return null;
            }
            
            double price = Double.parseDouble(priceStr);
            double high = highStr.isEmpty() ? price * 1.02 : Double.parseDouble(highStr);
            double low = lowStr.isEmpty() ? price * 0.98 : Double.parseDouble(lowStr);
            
            return new StockPrice(
                symbol,
                new BigDecimal(String.format("%.2f", price)),
                new BigDecimal(String.format("%.2f", high)),
                new BigDecimal(String.format("%.2f", low)),
                LocalDate.now().toString()
            );
        } catch (Exception e) {
            System.out.println("Alpha Vantage quote fetch failed for " + symbol + ": " + e.getMessage());
            return null;
        }
    }
    
    private void cachePrice(String symbol, StockPrice price) {
        priceCache.put(symbol, new PriceData(price, System.currentTimeMillis()));
    }

    public record StockPrice(String symbol, BigDecimal price, BigDecimal high, BigDecimal low, String date) {}

    private record PriceData(StockPrice price, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }
}
