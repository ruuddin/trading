package com.example.helloworld.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockPriceService {
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Fetch latest prices for the provided symbols using Yahoo Finance public endpoint.
     */
    public Map<String, BigDecimal> fetchPrices(List<String> symbols) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (symbols == null || symbols.isEmpty()) return result;

        String joined = String.join("%2C", symbols);
        String url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=" + joined;

        try {
            String body = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (body == null) return result;
            JsonNode root = mapper.readTree(body);
            JsonNode arr = root.path("quoteResponse").path("result");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String sym = n.path("symbol").asText();
                    double price = n.path("regularMarketPrice").asDouble(Double.NaN);
                    if (!Double.isNaN(price)) result.put(sym.toUpperCase(), BigDecimal.valueOf(price));
                }
            }
        } catch (Exception e) {
            // swallow and return whatever we have
        }

        return result;
    }

    /**
     * Fetch historical chart data for a symbol with specified interval.
     * Interval: 1m, 5m, 15m, 1h, 1d
     * Range: 1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max
     */
    public List<Map<String, Object>> fetchHistoricalData(String symbol, String interval, String range) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // Validate interval and range
        if (!isValidInterval(interval)) interval = "1h";
        if (!isValidRange(range)) range = "1d";
        
        String url = String.format("https://query1.finance.yahoo.com/v10/finance/quoteSummary/%s?modules=financialData",
                symbol.toUpperCase());

        try {
            // Use historical chart API
            String chartUrl = String.format(
                    "https://query1.finance.yahoo.com/v7/finance/chart/%s?interval=%s&range=%s",
                    symbol.toUpperCase(), interval, range);
            
            String body = webClient.get()
                    .uri(chartUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (body == null) return result;
            JsonNode root = mapper.readTree(body);
            JsonNode chart = root.path("chart").path("result");
            
            if (chart.isArray() && chart.size() > 0) {
                JsonNode result0 = chart.get(0);
                JsonNode timestamps = result0.path("timestamp");
                JsonNode quotes = result0.path("indicators").path("quote").get(0);
                JsonNode closes = quotes.path("close");

                for (int i = 0; i < timestamps.size() && i < closes.size(); i++) {
                    long ts = timestamps.get(i).asLong();
                    double price = closes.get(i).asDouble();
                    if (!Double.isNaN(price)) {
                        Map<String, Object> point = new HashMap<>();
                        point.put("timestamp", ts * 1000); // Convert to ms
                        point.put("price", BigDecimal.valueOf(price));
                        point.put("time", new Date(ts * 1000).toString());
                        result.add(point);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Search for symbols by partial match
     */
    public List<Map<String, String>> searchSymbols(String query) {
        List<Map<String, String>> result = new ArrayList<>();
        if (query == null || query.trim().isEmpty() || query.length() < 1) return result;
        
        try {
            String searchUrl = String.format(
                    "https://query1.finance.yahoo.com/v10/finance/autocomplete?query=%s&region=US&lang=en&corsDomain=finance.yahoo.com",
                    query.toUpperCase());
            
            String body = webClient.get()
                    .uri(searchUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (body == null) return result;
            JsonNode root = mapper.readTree(body);
            JsonNode quotes = root.path("quotes");
            
            if (quotes.isArray()) {
                for (JsonNode quote : quotes) {
                    String symbol = quote.path("symbol").asText();
                    String displayName = quote.path("shortname").asText();
                    if (!symbol.isEmpty()) {
                        Map<String, String> item = new HashMap<>();
                        item.put("symbol", symbol);
                        item.put("displayName", displayName);
                        result.add(item);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.stream().limit(10).collect(Collectors.toList());
    }

    private boolean isValidInterval(String interval) {
        return interval != null && (interval.equals("1m") || interval.equals("5m") || 
                interval.equals("15m") || interval.equals("1h") || interval.equals("1d"));
    }

    private boolean isValidRange(String range) {
        return range != null && (range.equals("1d") || range.equals("5d") || 
                range.equals("1mo") || range.equals("3mo") || range.equals("6mo") || 
                range.equals("1y") || range.equals("2y") || range.equals("5y") || 
                range.equals("10y") || range.equals("ytd") || range.equals("max"));
    }
}
