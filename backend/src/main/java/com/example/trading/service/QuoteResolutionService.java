package com.example.trading.service;

import com.example.trading.model.Stock;
import com.example.trading.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class QuoteResolutionService {

    private final SimpleStockPriceService priceService;
    private final StockRepository stockRepository;

    public QuoteResolutionService(SimpleStockPriceService priceService, StockRepository stockRepository) {
        this.priceService = priceService;
        this.stockRepository = stockRepository;
    }

    public ResolvedQuote resolve(String symbol) {
        String normalized = priceService.normalizeSymbol(symbol);
        if (!priceService.isValidSymbol(normalized)) {
            return null;
        }

        SimpleStockPriceService.StockPrice live = priceService.getCurrentPrice(normalized);
        if (live != null && live.price() != null) {
            return new ResolvedQuote(
                live.symbol(),
                live.price(),
                live.high(),
                live.low(),
                live.date(),
                "LIVE"
            );
        }

        Stock stock = stockRepository.findBySymbol(normalized).orElse(null);
        if (stock == null || stock.getPrice() == null) {
            return null;
        }

        BigDecimal referencePrice = stock.getPrice();
        return new ResolvedQuote(
            normalized,
            referencePrice,
            referencePrice,
            referencePrice,
            LocalDate.now().toString(),
            "REFERENCE"
        );
    }

    public record ResolvedQuote(
        String symbol,
        BigDecimal price,
        BigDecimal high,
        BigDecimal low,
        String date,
        String source
    ) {}
}
