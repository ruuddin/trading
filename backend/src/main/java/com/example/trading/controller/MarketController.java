package com.example.trading.controller;

import com.example.trading.model.Stock;
import com.example.trading.repository.StockRepository;
import com.example.trading.service.MultiProviderStockDataFetcher;
import com.example.trading.service.QuoteResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class MarketController {

    private final StockRepository repo;
    private final MultiProviderStockDataFetcher fetcher;
    private final QuoteResolutionService quoteResolutionService;

    public MarketController(StockRepository repo, MultiProviderStockDataFetcher fetcher, QuoteResolutionService quoteResolutionService) {
        this.repo = repo;
        this.fetcher = fetcher;
        this.quoteResolutionService = quoteResolutionService;
    }

    @GetMapping
    public List<Stock> list() {
        return repo.findAll();
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<Stock> getBySymbol(@PathVariable String symbol) {
        return repo.findBySymbol(symbol).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{symbol}/history")
    public ResponseEntity<?> getHistoricalData(@PathVariable String symbol, @RequestParam(defaultValue = "daily") String interval) {
        List<MultiProviderStockDataFetcher.HistoricalData> data = fetcher.getHistoricalData(symbol, interval);
        return ResponseEntity.ok(new HistoricalDataResponse(symbol, interval, data));
    }

    /**
     * Get live price for a symbol (simple service with fallback pricing)
     */
    @GetMapping("/{symbol}/price")
    public ResponseEntity<?> getLivePrice(@PathVariable String symbol) {
        try {
            QuoteResolutionService.ResolvedQuote quote = quoteResolutionService.resolve(symbol);
            if (quote == null) {
                String normalized = symbol == null ? "" : symbol.trim().toUpperCase();
                return ResponseEntity.status(503).body("Live price unavailable for symbol: " + normalized);
            }

            return ResponseEntity.ok(new LivePriceResponse(
                quote.symbol(),
                quote.price(),
                quote.high(),
                quote.low(),
                quote.date(),
                quote.source()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching price: " + e.getMessage());
        }
    }

    private record HistoricalDataResponse(
        String symbol,
        String interval,
        List<MultiProviderStockDataFetcher.HistoricalData> data
    ) {}

    private record LivePriceResponse(
        String symbol,
        java.math.BigDecimal price,
        java.math.BigDecimal high,
        java.math.BigDecimal low,
        String date,
        String source
    ) {}
}
