package com.example.trading.controller;

import com.example.trading.model.Order;
import com.example.trading.model.Portfolio;
import com.example.trading.model.Stock;
import com.example.trading.model.User;
import com.example.trading.model.Watchlist;
import com.example.trading.repository.OrderRepository;
import com.example.trading.repository.PortfolioRepository;
import com.example.trading.repository.StockRepository;
import com.example.trading.repository.UserRepository;
import com.example.trading.repository.WatchlistRepository;
import com.example.trading.service.SimpleStockPriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TradingController {

    private final UserRepository users;
    private final StockRepository stocks;
    private final OrderRepository orders;
    private final PortfolioRepository portfolios;
    private final WatchlistRepository watchlists;
    private final SimpleStockPriceService priceService;

    public TradingController(UserRepository users, StockRepository stocks, OrderRepository orders, PortfolioRepository portfolios, WatchlistRepository watchlists, SimpleStockPriceService priceService) {
        this.users = users;
        this.stocks = stocks;
        this.orders = orders;
        this.portfolios = portfolios;
        this.watchlists = watchlists;
        this.priceService = priceService;
    }

    // create order; user is identified from JWT (principal name)
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(java.security.Principal principal, @RequestBody Map<String,Object> body) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        String symbol = (String) body.get("symbol");
        int qty = ((Number) body.get("quantity")).intValue();
        String side = (String) body.get("side");
        Stock s = stocks.findBySymbol(symbol).orElse(null);
        BigDecimal price = s != null ? s.getPrice() : BigDecimal.ZERO;

        Order o = new Order(u.getId(), symbol, qty, price, side, "FILLED");
        orders.save(o);

        // naive portfolio update
        List<Portfolio> pList = portfolios.findByUserId(u.getId());
        Portfolio p = pList.stream().filter(x -> x.getSymbol().equals(symbol)).findFirst().orElse(null);
        if (p == null) {
            if (side.equalsIgnoreCase("BUY")) {
                portfolios.save(new Portfolio(u.getId(), symbol, qty, price));
            }
        } else {
            int newQty = p.getQuantity() + (side.equalsIgnoreCase("BUY") ? qty : -qty);
            portfolios.delete(p);
            if (newQty > 0) {
                portfolios.save(new Portfolio(u.getId(), symbol, newQty, p.getAvgPrice()));
            }
        }

        return ResponseEntity.ok(o);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> listOrders(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        return ResponseEntity.ok(orders.findByUserId(u.getId()));
    }

    @GetMapping("/portfolio")
    public ResponseEntity<?> portfolio(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        return ResponseEntity.ok(portfolios.findByUserId(u.getId()));
    }

    // ===== WATCHLIST ENDPOINTS =====

    /**
     * Get all watchlists for the authenticated user
     */
    @GetMapping("/watchlists")
    public ResponseEntity<?> getWatchlists(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        
        List<Watchlist> userWatchlists = watchlists.findByUserId(u.getId());
        return ResponseEntity.ok(userWatchlists);
    }

    /**
     * Create a new watchlist
     * Request body: {"name": "watchlist name"}
     */
    @PostMapping("/watchlists")
    public ResponseEntity<?> createWatchlist(java.security.Principal principal, @RequestBody Map<String, Object> body) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        
        // Check max 20 watchlists per user
        long count = watchlists.countByUserId(u.getId());
        if (count >= 20) {
            return ResponseEntity.status(400).body("Maximum 20 watchlists allowed per user");
        }
        
        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.status(400).body("Watchlist name is required");
        }
        
        Watchlist w = new Watchlist(u.getId(), name.trim());
        Watchlist saved = watchlists.save(w);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get a specific watchlist
     */
    @GetMapping("/watchlists/{id}")
    public ResponseEntity<?> getWatchlist(java.security.Principal principal, @PathVariable Long id) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        
        Watchlist w = watchlists.findByIdAndUserId(id, u.getId()).orElse(null);
        if (w == null) return ResponseEntity.status(404).body("Watchlist not found");
        return ResponseEntity.ok(w);
    }

    /**
     * Update watchlist name
     * Request body: {"name": "new name"}
     */
    @PutMapping("/watchlists/{id}")
    public ResponseEntity<?> updateWatchlist(java.security.Principal principal, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        
        Watchlist w = watchlists.findByIdAndUserId(id, u.getId()).orElse(null);
        if (w == null) return ResponseEntity.status(404).body("Watchlist not found");
        
        String newName = (String) body.get("name");
        if (newName != null && !newName.trim().isEmpty()) {
            w.setName(newName.trim());
            Watchlist updated = watchlists.save(w);
            return ResponseEntity.ok(updated);
        }
        
        return ResponseEntity.status(400).body("Watchlist name is required");
    }

    /**
     * Add a symbol to watchlist
     * Request body: {"symbol": "AAPL"}
     */
    @PostMapping("/watchlists/{id}/symbols")
    public ResponseEntity<?> addSymbolToWatchlist(java.security.Principal principal, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        
        Watchlist w = watchlists.findByIdAndUserId(id, u.getId()).orElse(null);
        if (w == null) return ResponseEntity.status(404).body("Watchlist not found");
        
        Object rawSymbol = body.get("symbol");
        if (!(rawSymbol instanceof String rawSymbolText)) {
            return ResponseEntity.status(400).body("Symbol is required");
        }

        String symbol = priceService.normalizeSymbol(rawSymbolText);
        if (symbol.isEmpty()) {
            return ResponseEntity.status(400).body("Symbol is required");
        }
        
        // Validate symbol exists and is supported
        if (!priceService.isValidSymbol(symbol)) {
            return ResponseEntity.status(400).body("Invalid or unsupported stock symbol: " + symbol);
        }
        
        if (w.getSymbols().contains(symbol)) {
            return ResponseEntity.status(400).body("Symbol already in watchlist");
        }
        
        if (w.getSymbols().size() >= 30) {
            return ResponseEntity.status(400).body("Maximum 30 symbols allowed per watchlist");
        }
        
        w.addSymbol(symbol);
        Watchlist updated = watchlists.save(w);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove a symbol from watchlist
     */
    @DeleteMapping("/watchlists/{id}/symbols/{symbol}")
    public ResponseEntity<?> removeSymbolFromWatchlist(java.security.Principal principal, @PathVariable Long id, @PathVariable String symbol) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        
        Watchlist w = watchlists.findByIdAndUserId(id, u.getId()).orElse(null);
        if (w == null) return ResponseEntity.status(404).body("Watchlist not found");
        
        w.removeSymbol(symbol.toUpperCase());
        Watchlist updated = watchlists.save(w);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a watchlist
     */
    @DeleteMapping("/watchlists/{id}")
    public ResponseEntity<?> deleteWatchlist(java.security.Principal principal, @PathVariable Long id) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        String username = principal.getName();
        User u = users.findByUsername(username).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("unknown user");
        
        Watchlist w = watchlists.findByIdAndUserId(id, u.getId()).orElse(null);
        if (w == null) return ResponseEntity.status(404).body("Watchlist not found");
        
        watchlists.delete(w);
        return ResponseEntity.ok("Watchlist deleted");
    }
}
