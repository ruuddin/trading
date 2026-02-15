package com.example.trading.repository;

import com.example.trading.model.StockDataCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StockDataCacheRepository extends JpaRepository<StockDataCache, Long> {

    /**
     * Find valid (non-expired) cache entry for a symbol and interval
     */
    @Query("SELECT s FROM StockDataCache s WHERE s.symbol = :symbol AND s.interval = :interval AND s.expiresAt > CURRENT_TIMESTAMP ORDER BY s.createdAt DESC LIMIT 1")
    Optional<StockDataCache> findValidCache(@Param("symbol") String symbol, @Param("interval") String interval);

    /**
     * Delete all expired cache entries
     */
    @Query("DELETE FROM StockDataCache s WHERE s.expiresAt <= CURRENT_TIMESTAMP")
    void deleteExpiredCache();

    /**
     * Delete specific cache entry
     */
    void deleteBySymbolAndInterval(String symbol, String interval);

    /**
     * Count valid cache entries
     */
    @Query("SELECT COUNT(s) FROM StockDataCache s WHERE s.expiresAt > CURRENT_TIMESTAMP")
    long countValidEntries();
}
