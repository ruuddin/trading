package com.example.trading.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String name;
    private String keyPrefix;
    private String last4;
    private String keyHash;
    private String status;

    private long totalRequests;
    private int requestsToday;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;

    public ApiKey() {}

    public ApiKey(Long userId, String name, String keyPrefix, String last4, String keyHash) {
        this.userId = userId;
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.last4 = last4;
        this.keyHash = keyHash;
        this.status = "ACTIVE";
        this.totalRequests = 0;
        this.requestsToday = 0;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getKeyPrefix() { return keyPrefix; }
    public String getLast4() { return last4; }
    public String getKeyHash() { return keyHash; }
    public String getStatus() { return status; }
    public long getTotalRequests() { return totalRequests; }
    public int getRequestsToday() { return requestsToday; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
    public void setRequestsToday(int requestsToday) { this.requestsToday = requestsToday; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
