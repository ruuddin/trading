package com.example.trading.service;

import com.example.trading.model.Order;
import com.example.trading.model.Portfolio;
import com.example.trading.repository.OrderRepository;
import com.example.trading.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PortfolioAnalyticsService {

    private final PortfolioRepository portfolios;
    private final OrderRepository orders;
    private final SimpleStockPriceService priceService;

    public PortfolioAnalyticsService(PortfolioRepository portfolios, OrderRepository orders, SimpleStockPriceService priceService) {
        this.portfolios = portfolios;
        this.orders = orders;
        this.priceService = priceService;
    }

    public Map<String, Object> getPortfolioSummary(Long userId) {
        List<Portfolio> positions = portfolios.findByUserId(userId);
        List<Order> userOrders = orders.findByUserId(userId);

        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal costBasis = BigDecimal.ZERO;

        for (Portfolio position : positions) {
            BigDecimal avgPrice = defaultNumber(position.getAvgPrice());
            BigDecimal quantity = BigDecimal.valueOf(position.getQuantity());

            BigDecimal currentPrice = avgPrice;
            SimpleStockPriceService.StockPrice quote = priceService.getCurrentPrice(position.getSymbol());
            if (quote != null && quote.price() != null) {
                currentPrice = quote.price();
            }

            marketValue = marketValue.add(currentPrice.multiply(quantity));
            costBasis = costBasis.add(avgPrice.multiply(quantity));
        }

        BigDecimal unrealized = marketValue.subtract(costBasis);
        BigDecimal realized = calculateRealizedPnL(userOrders);
        BigDecimal totalPnL = realized.add(unrealized);
        BigDecimal totalReturnPct = percentage(unrealized, costBasis);

        return Map.of(
            "positions", positions.size(),
            "marketValue", scaled(marketValue),
            "costBasis", scaled(costBasis),
            "unrealizedPnL", scaled(unrealized),
            "realizedPnL", scaled(realized),
            "totalPnL", scaled(totalPnL),
            "totalReturnPct", scaled(totalReturnPct)
        );
    }

    public Map<String, Object> getPerformanceSeries(Long userId, String range) {
        String normalizedRange = normalizeRange(range);
        Map<String, Object> summary = getPortfolioSummary(userId);
        BigDecimal currentValue = asBigDecimal(summary.get("marketValue"));

        int points = switch (normalizedRange) {
            case "1M" -> 8;
            case "3M" -> 12;
            case "1Y" -> 12;
            default -> 16;
        };

        BigDecimal baseline = currentValue.multiply(getRangeBaselineFactor(normalizedRange));
        List<Map<String, Object>> series = new ArrayList<>();

        for (int i = 0; i < points; i++) {
            double progress = points == 1 ? 1.0 : (double) i / (points - 1);
            BigDecimal value = baseline.add(currentValue.subtract(baseline)
                .multiply(BigDecimal.valueOf(progress)));

            series.add(Map.of(
                "date", LocalDate.now().minusDays((long) (points - 1 - i) * 7).toString(),
                "value", scaled(value)
            ));
        }

        return Map.of(
            "range", normalizedRange,
            "series", series
        );
    }

    private BigDecimal calculateRealizedPnL(List<Order> userOrders) {
        BigDecimal buyNotional = BigDecimal.ZERO;
        BigDecimal sellNotional = BigDecimal.ZERO;

        for (Order order : userOrders) {
            BigDecimal price = defaultNumber(order.getPrice());
            BigDecimal qty = BigDecimal.valueOf(order.getQuantity());
            BigDecimal notional = price.multiply(qty);

            if ("SELL".equalsIgnoreCase(order.getSide())) {
                sellNotional = sellNotional.add(notional);
            } else {
                buyNotional = buyNotional.add(notional);
            }
        }

        BigDecimal heuristicCost = buyNotional.multiply(new BigDecimal("0.70"));
        return sellNotional.subtract(heuristicCost);
    }

    private BigDecimal getRangeBaselineFactor(String range) {
        return switch (range) {
            case "1M" -> new BigDecimal("0.97");
            case "3M" -> new BigDecimal("0.93");
            case "1Y" -> new BigDecimal("0.86");
            default -> new BigDecimal("0.80");
        };
    }

    private String normalizeRange(String range) {
        if (range == null || range.isBlank()) {
            return "1M";
        }

        String normalized = range.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("1M") || normalized.equals("3M") || normalized.equals("1Y") || normalized.equals("ALL")) {
            return normalized;
        }
        return "1M";
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal base) {
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return value
            .multiply(BigDecimal.valueOf(100))
            .divide(base, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultNumber(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal number) {
            return number;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal scaled(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
