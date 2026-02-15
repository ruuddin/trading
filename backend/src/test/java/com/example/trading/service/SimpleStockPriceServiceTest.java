package com.example.trading.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SimpleStockPriceServiceTest {

    private static class StubSimpleStockPriceService extends SimpleStockPriceService {
        private final StockPrice yahooQuote;
        private final StockPrice alphaQuote;

        StubSimpleStockPriceService(StockPrice yahooQuote, StockPrice alphaQuote) {
            this.yahooQuote = yahooQuote;
            this.alphaQuote = alphaQuote;
        }

        @Override
        protected StockPrice tryYahooQuote(String symbol) {
            return yahooQuote;
        }

        @Override
        protected StockPrice tryAlphaVantageQuote(String symbol) {
            return alphaQuote;
        }
    }

    @Test
    void returnsNullWhenProvidersUnavailable() {
        SimpleStockPriceService service = new StubSimpleStockPriceService(null, null);

        SimpleStockPriceService.StockPrice result = service.getCurrentPrice("MU");

        assertNull(result);
    }

    @Test
    void returnsNullForInvalidSymbol() {
        SimpleStockPriceService service = new StubSimpleStockPriceService(null, null);

        SimpleStockPriceService.StockPrice price = service.getCurrentPrice("MU$");

        assertNull(price);
    }

    @Test
    void usesProviderPriceWhenAvailable() {
        SimpleStockPriceService.StockPrice yahoo = new SimpleStockPriceService.StockPrice(
            "ZZTOP",
            new BigDecimal("123.45"),
            new BigDecimal("125.00"),
            new BigDecimal("122.00"),
            "2026-02-14"
        );

        SimpleStockPriceService service = new StubSimpleStockPriceService(yahoo, null);

        SimpleStockPriceService.StockPrice result = service.getCurrentPrice("ZZTOP");

        assertNotNull(result);
        assertEquals(yahoo, result);
    }
}
