package com.example.helloworld.controller;

import com.example.helloworld.model.Watchlist;
import com.example.helloworld.repository.WatchlistRepository;
import com.example.helloworld.service.StockPriceService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin(origins = "*")
public class WatchlistRestController {
    private final WatchlistRepository repo;
    private final StockPriceService priceService;
    
    private static final int MAX_WATCHLISTS = 10;
    private static final int MAX_SYMBOLS_PER_WATCHLIST = 20;

    public WatchlistRestController(WatchlistRepository r, StockPriceService p) {
        this.repo = r;
        this.priceService = p;
    }

    @GetMapping
    public List<Watchlist> getAllWatchlists() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Watchlist getWatchlist(@PathVariable Long id) {
        return repo.findById(id).orElse(null);
    }

    @PostMapping
    public Map<String, Object> createWatchlist(@RequestBody Watchlist w) {
        Map<String, Object> response = new HashMap<>();
        
        if (repo.count() >= MAX_WATCHLISTS) {
            response.put("success", false);
            response.put("error", "Maximum " + MAX_WATCHLISTS + " watchlists allowed");
            return response;
        }
        
        Watchlist saved = repo.save(w);
        response.put("success", true);
        response.put("data", saved);
        return response;
    }
    
    @PostMapping("/{id}/add-symbol")
    public Map<String, Object> addSymbol(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        
        Watchlist w = repo.findById(id).orElse(null);
        if (w == null) {
            response.put("success", false);
            response.put("error", "Watchlist not found");
            return response;
        }
        
        if (w.getStocks().size() >= MAX_SYMBOLS_PER_WATCHLIST) {
            response.put("success", false);
            response.put("error", "Maximum " + MAX_SYMBOLS_PER_WATCHLIST + " symbols per watchlist");
            return response;
        }
        
        String symbol = body.get("symbol");
        if (symbol != null && !symbol.isEmpty()) {
            // Check if symbol already in watchlist
            boolean exists = w.getStocks().stream()
                    .anyMatch(s -> s.getSymbol().equalsIgnoreCase(symbol));
            
            if (exists) {
                response.put("success", false);
                response.put("error", "Symbol already in watchlist");
                return response;
            }
            
            w.getStocks().add(new com.example.helloworld.model.StockItem(symbol, symbol, BigDecimal.ZERO, w));
            repo.save(w);
            response.put("success", true);
            response.put("data", w);
        }
        
        return response;
    }
}

@RestController
@RequestMapping("/api/prices")
@CrossOrigin(origins = "*")
class PriceController {
    private final StockPriceService priceService;

    PriceController(StockPriceService p) { this.priceService = p; }

    @GetMapping
    public Map<String, BigDecimal> getPrices(@RequestParam String symbols) {
        List<String> syms = List.of(symbols.split(","));
        return priceService.fetchPrices(syms);
    }
}

@RestController
@RequestMapping("/api/chart")
@CrossOrigin(origins = "*")
class ChartController {
    private final StockPriceService priceService;

    ChartController(StockPriceService p) { this.priceService = p; }

    @GetMapping("/history")
    public List<Map<String, Object>> getChartData(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1h") String interval,
            @RequestParam(defaultValue = "1d") String range) {
        return priceService.fetchHistoricalData(symbol, interval, range);
    }
}

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
class SearchController {
    private final StockPriceService priceService;

    SearchController(StockPriceService p) { this.priceService = p; }

    @GetMapping("/symbols")
    public List<Map<String, String>> searchSymbols(@RequestParam String q) {
        return priceService.searchSymbols(q);
    }
}
