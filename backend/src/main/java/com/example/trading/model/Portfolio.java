package com.example.trading.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String symbol;
    private int quantity;
    private BigDecimal avgPrice;

    public Portfolio() {}

    public Portfolio(Long userId, String symbol, int quantity, BigDecimal avgPrice) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public int getQuantity() { return quantity; }
    public BigDecimal getAvgPrice() { return avgPrice; }
}
