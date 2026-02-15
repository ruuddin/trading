package com.example.trading.repository;

import com.example.trading.model.WatchlistShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistShareRepository extends JpaRepository<WatchlistShare, Long> {
    List<WatchlistShare> findBySharedWithUserId(Long sharedWithUserId);
    Optional<WatchlistShare> findByWatchlistIdAndSharedWithUserId(Long watchlistId, Long sharedWithUserId);
    Optional<WatchlistShare> findByWatchlistIdAndOwnerUserIdAndSharedWithUserId(Long watchlistId, Long ownerUserId, Long sharedWithUserId);
}
