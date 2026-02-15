package com.example.trading.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track API usage and rate limits across all providers.
 * Maintains in-memory counters for the current day.
 */
@Service
public class ApiUsageTracker {

    private static final class ProviderMetrics {
        int dailyRequestCount = 0;
        int dailyLimit;
        LocalDateTime lastResetTime = LocalDateTime.now();
        long minuteRequestCount = 0;
        int minuteLimit;
        LocalDateTime lastMinuteResetTime = LocalDateTime.now();
        boolean rateLimited = false;

        ProviderMetrics(int dailyLimit, int minuteLimit) {
            this.dailyLimit = dailyLimit;
            this.minuteLimit = minuteLimit;
        }

        synchronized boolean incrementDaily() {
            // Reset if new day
            if (ChronoUnit.DAYS.between(lastResetTime, LocalDateTime.now()) >= 1) {
                dailyRequestCount = 0;
                lastResetTime = LocalDateTime.now();
                rateLimited = false;
            }
            
            if (dailyRequestCount >= dailyLimit) {
                rateLimited = true;
                return false;
            }
            
            dailyRequestCount++;
            return true;
        }

        synchronized boolean canMakeMinuteRequest() {
            // Reset if past minute
            if (ChronoUnit.MINUTES.between(lastMinuteResetTime, LocalDateTime.now()) >= 1) {
                minuteRequestCount = 0;
                lastMinuteResetTime = LocalDateTime.now();
            }
            
            if (minuteRequestCount >= minuteLimit) {
                return false;
            }
            
            minuteRequestCount++;
            return true;
        }

        synchronized ApiUsageDto getMetrics() {
            return new ApiUsageDto(
                dailyRequestCount,
                dailyLimit,
                (long) minuteRequestCount,
                minuteLimit,
                rateLimited,
                dailyLimit > 0 ? (dailyRequestCount * 100.0 / dailyLimit) : 0,
                minuteLimit > 0 ? (minuteRequestCount * 100.0 / minuteLimit) : 0
            );
        }
    }

    private final ConcurrentHashMap<String, ProviderMetrics> providers = new ConcurrentHashMap<>();

    public ApiUsageTracker() {
        // Alpha Vantage: 25 requests/day, 5 per minute
        providers.put("ALPHA_VANTAGE", new ProviderMetrics(25, 5));
        // Finnhub: 500 requests/day, 60 per minute
        providers.put("FINNHUB", new ProviderMetrics(500, 60));
        // Twelve Data: 800 requests/day, 60 per minute
        providers.put("TWELVEDATA", new ProviderMetrics(800, 60));
        // Massive: 1000 requests/day, 100 per minute (assumed high volume)
        providers.put("MASSIVE", new ProviderMetrics(1000, 100));
    }

    /**
     * Record a request for a provider
     */
    public boolean recordRequest(String provider) {
        ProviderMetrics metrics = providers.get(provider.toUpperCase());
        if (metrics == null) {
            System.out.println("⚠ Unknown provider: " + provider);
            return false;
        }
        
        return metrics.incrementDaily();
    }

    /**
     * Check if provider can make a minute-level request
     */
    public boolean canMakeRequest(String provider) {
        ProviderMetrics metrics = providers.get(provider.toUpperCase());
        if (metrics == null) return false;
        return metrics.canMakeMinuteRequest();
    }

    /**
     * Get metrics for a specific provider
     */
    public ApiUsageDto getProviderMetrics(String provider) {
        ProviderMetrics metrics = providers.get(provider.toUpperCase());
        if (metrics == null) {
            return new ApiUsageDto(0, 0, 0, 0, true, 0, 0);
        }
        return metrics.getMetrics();
    }

    /**
     * Get all provider metrics
     */
    public ConcurrentHashMap<String, ApiUsageDto> getAllMetrics() {
        ConcurrentHashMap<String, ApiUsageDto> allMetrics = new ConcurrentHashMap<>();
        providers.forEach((name, metrics) -> allMetrics.put(name, metrics.getMetrics()));
        return allMetrics;
    }

    /**
     * Reset metrics for a provider (for testing)
     */
    public void resetMetrics(String provider) {
        ProviderMetrics metrics = providers.get(provider.toUpperCase());
        if (metrics != null) {
            metrics.dailyRequestCount = 0;
            metrics.minuteRequestCount = 0;
            metrics.rateLimited = false;
            metrics.lastResetTime = LocalDateTime.now();
            metrics.lastMinuteResetTime = LocalDateTime.now();
        }
    }

    /**
     * DTO for API usage metrics
     */
    public record ApiUsageDto(
        int dailyRequestCount,
        int dailyLimit,
        long minuteRequestCount,
        int minuteLimit,
        boolean rateLimited,
        double dailyUsagePercent,
        double minuteUsagePercent
    ) {}
}
