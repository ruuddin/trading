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
    public Watchlist createWatchlist(@RequestBody Watchlist w) {
        return repo.save(w);
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
