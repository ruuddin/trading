package com.example.trading.controller;

import com.example.trading.model.SavedScan;
import com.example.trading.model.Stock;
import com.example.trading.repository.SavedScanRepository;
import com.example.trading.repository.StockRepository;
import com.example.trading.repository.UserRepository;
import com.example.trading.service.SimpleStockPriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/screener")
public class ScreenerController {

    private final StockRepository stocks;
    private final SavedScanRepository savedScans;
    private final UserRepository users;
    private final SimpleStockPriceService priceService;

    public ScreenerController(StockRepository stocks,
                              SavedScanRepository savedScans,
                              UserRepository users,
                              SimpleStockPriceService priceService) {
        this.stocks = stocks;
        this.savedScans = savedScans;
        this.users = users;
        this.priceService = priceService;
    }

    @GetMapping
    public ResponseEntity<?> screen(java.security.Principal principal,
                                    @RequestParam(required = false) String query,
                                    @RequestParam(required = false) BigDecimal minPrice,
                                    @RequestParam(required = false) BigDecimal maxPrice,
                                    @RequestParam(defaultValue = "50") int limit) {
        if (resolveUser(principal) == null) return ResponseEntity.status(401).body("unauthenticated");

        int boundedLimit = Math.max(1, Math.min(limit, 200));
        String q = query == null ? "" : query.trim().toUpperCase(Locale.ROOT);

        List<Map<String, Object>> results = stocks.findAll().stream()
            .filter(stock -> matchesQuery(stock, q))
            .map(stock -> {
                BigDecimal livePrice = stock.getPrice();
                var quote = priceService.getCurrentPrice(stock.getSymbol());
                if (quote != null && quote.price() != null) {
                    livePrice = quote.price();
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("symbol", stock.getSymbol());
                item.put("name", stock.getName());
                item.put("price", livePrice);
                return item;
            })
            .filter(item -> withinPriceRange((BigDecimal) item.get("price"), minPrice, maxPrice))
            .sorted(Comparator.comparing(item -> ((String) item.get("symbol"))))
            .limit(boundedLimit)
            .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query == null ? "" : query);
        response.put("minPrice", minPrice);
        response.put("maxPrice", maxPrice);
        response.put("count", results.size());
        response.put("results", results);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/saved")
    public ResponseEntity<?> saveScan(java.security.Principal principal, @RequestBody Map<String, Object> body) {
        var user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body("unauthenticated");

        Object rawName = body.get("name");
        if (!(rawName instanceof String name) || name.isBlank()) {
            return ResponseEntity.badRequest().body("name is required");
        }

        String query = body.get("query") instanceof String q ? q.trim() : null;
        BigDecimal minPrice = toBigDecimal(body.get("minPrice"));
        BigDecimal maxPrice = toBigDecimal(body.get("maxPrice"));

        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().body("minPrice must be >= 0");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().body("maxPrice must be >= 0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            return ResponseEntity.badRequest().body("minPrice must be <= maxPrice");
        }

        SavedScan saved = savedScans.save(new SavedScan(
            user.getId(),
            name.trim(),
            query,
            minPrice,
            maxPrice
        ));
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/saved")
    public ResponseEntity<?> listSaved(java.security.Principal principal) {
        var user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body("unauthenticated");

        return ResponseEntity.ok(savedScans.findByUserIdOrderByUpdatedAtDesc(user.getId()));
    }

    private com.example.trading.model.User resolveUser(java.security.Principal principal) {
        if (principal == null) return null;
        return users.findByUsername(principal.getName()).orElse(null);
    }

    private boolean matchesQuery(Stock stock, String query) {
        if (query == null || query.isBlank()) return true;
        String symbol = stock.getSymbol() == null ? "" : stock.getSymbol().toUpperCase(Locale.ROOT);
        String name = stock.getName() == null ? "" : stock.getName().toUpperCase(Locale.ROOT);
        return symbol.contains(query) || name.contains(query);
    }

    private boolean withinPriceRange(BigDecimal price, BigDecimal minPrice, BigDecimal maxPrice) {
        if (price == null) return false;
        if (minPrice != null && price.compareTo(minPrice) < 0) return false;
        if (maxPrice != null && price.compareTo(maxPrice) > 0) return false;
        return true;
    }

    private BigDecimal toBigDecimal(Object raw) {
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (raw instanceof String text && !text.isBlank()) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
