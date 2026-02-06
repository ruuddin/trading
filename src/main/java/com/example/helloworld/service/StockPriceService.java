package com.example.helloworld.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
