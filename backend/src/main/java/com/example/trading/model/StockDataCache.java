package com.example.trading.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for caching stock historical data with 60-minute TTL.
 * Stores OHLC data to reduce API calls and costs.
 */
@Entity
@Table(name = "stock_data_cache", indexes = {
    @Index(name = "idx_symbol_interval", columnList = "symbol,time_interval"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
public class StockDataCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, length = 20, name = "time_interval")
    private String interval;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String data; // JSON data containing OHLC records

    @Column(nullable = false)
    private String provider; // Which API provided this data

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "expires_at")
    private LocalDateTime expiresAt; // 60 minutes from creation

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMinutes(60);
    }

    public StockDataCache() {}

    public StockDataCache(String symbol, String interval, String data, String provider) {
        this.symbol = symbol;
        this.interval = interval;
        this.data = data;
        this.provider = provider;
        onCreate();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
