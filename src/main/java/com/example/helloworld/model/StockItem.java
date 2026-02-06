package com.example.helloworld.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class StockItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String displayName;
    private BigDecimal lastPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    private Watchlist watchlist;

    public StockItem() {}
    public StockItem(String symbol) { this.symbol = symbol; }

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public BigDecimal getLastPrice() { return lastPrice; }
    public void setLastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; }
    public Watchlist getWatchlist() { return watchlist; }
    public void setWatchlist(Watchlist watchlist) { this.watchlist = watchlist; }
}
