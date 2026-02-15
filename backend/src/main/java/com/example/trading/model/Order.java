package com.example.trading.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity(name = "stock_order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String symbol;
    private int quantity;
    private BigDecimal price;
    private String side; // BUY or SELL
    private String status; // PENDING, FILLED

    public Order() {}

    public Order(Long userId, String symbol, int quantity, BigDecimal price, String side, String status) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public String getSide() { return side; }
    public String getStatus() { return status; }
}
