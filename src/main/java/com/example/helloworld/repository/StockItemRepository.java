package com.example.helloworld.repository;

import com.example.helloworld.model.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    Optional<StockItem> findBySymbolIgnoreCase(String symbol);
}
