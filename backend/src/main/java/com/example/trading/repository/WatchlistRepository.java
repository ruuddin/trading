package com.example.trading.repository;

import com.example.trading.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUserId(Long userId);
    
    Optional<Watchlist> findByIdAndUserId(Long id, Long userId);
    
    @Query("SELECT COUNT(w) FROM Watchlist w WHERE w.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}
