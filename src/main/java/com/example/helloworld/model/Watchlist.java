package com.example.helloworld.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Watchlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockItem> stocks = new ArrayList<>();

    public Watchlist() {}

    public Watchlist(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<StockItem> getStocks() { return stocks; }
    public void setStocks(List<StockItem> stocks) { this.stocks = stocks; }
    public void addStock(StockItem s) { s.setWatchlist(this); this.stocks.add(s); }
    public void removeStock(StockItem s) { this.stocks.remove(s); s.setWatchlist(null); }
}
