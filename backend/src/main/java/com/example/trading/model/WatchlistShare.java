package com.example.trading.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_share")
public class WatchlistShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long watchlistId;
    private Long ownerUserId;
    private Long sharedWithUserId;

    private LocalDateTime createdAt;

    public WatchlistShare() {}

    public WatchlistShare(Long watchlistId, Long ownerUserId, Long sharedWithUserId) {
        this.watchlistId = watchlistId;
        this.ownerUserId = ownerUserId;
        this.sharedWithUserId = sharedWithUserId;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getWatchlistId() { return watchlistId; }
    public Long getOwnerUserId() { return ownerUserId; }
    public Long getSharedWithUserId() { return sharedWithUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
