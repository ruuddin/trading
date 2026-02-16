package com.example.trading.controller;

import com.example.trading.service.ApiUsageTracker;
import com.example.trading.service.MultiProviderStockDataFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for API usage metrics and monitoring
 */
@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricsController {

    private final Instant startedAt = Instant.now();

    @Autowired
    private ApiUsageTracker apiUsageTracker;

    @Autowired
    private MultiProviderStockDataFetcher stockDataFetcher;

    /**
     * Get all API provider metrics
     */
    @GetMapping("/usage")
    public ResponseEntity<ConcurrentHashMap<String, ApiUsageTracker.ApiUsageDto>> getUsage() {
        return ResponseEntity.ok(apiUsageTracker.getAllMetrics());
    }

    /**
     * Get specific provider metrics
     */
    @GetMapping("/usage/{provider}")
    public ResponseEntity<ApiUsageTracker.ApiUsageDto> getProviderUsage(@PathVariable String provider) {
        var metrics = apiUsageTracker.getProviderMetrics(provider);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Check if any provider is rate limited
     */
    @GetMapping("/rate-limited")
    public ResponseEntity<Map<String, Boolean>> checkRateLimits() {
        var allMetrics = apiUsageTracker.getAllMetrics();
        var result = new java.util.LinkedHashMap<String, Boolean>();
        allMetrics.forEach((provider, metrics) -> result.put(provider, metrics.rateLimited()));
        return ResponseEntity.ok(result);
    }

    /**
     * Get summary statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<MetricsSummaryResponse> getSummary() {
        var allMetrics = apiUsageTracker.getAllMetrics();
        
        int totalRequests = 0;
        int totalLimits = 0;
        int rateLimitedCount = 0;
        double avgUsagePercent = 0;
        
        for (var metric : allMetrics.values()) {
            totalRequests += metric.dailyRequestCount();
            totalLimits += metric.dailyLimit();
            if (metric.rateLimited()) rateLimitedCount++;
            avgUsagePercent += metric.dailyUsagePercent();
        }
        
        if (!allMetrics.isEmpty()) {
            avgUsagePercent /= allMetrics.size();
        }

        return ResponseEntity.ok(new MetricsSummaryResponse(
            totalRequests,
            totalLimits,
            String.format("%.2f%%", avgUsagePercent),
            rateLimitedCount,
            allMetrics,
            LocalDateTime.now()
        ));
    }

    /**
     * Get circuit breaker status for external quote providers
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, MultiProviderStockDataFetcher.CircuitBreakerStatus>> getCircuitBreakerStatus() {
        return ResponseEntity.ok(stockDataFetcher.getProviderCircuitBreakerStatus());
    }

    /**
     * Consolidated dashboard status for operations
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatusResponse> getDashboardStatus() {
        var allMetrics = apiUsageTracker.getAllMetrics();
        var rateLimited = new LinkedHashMap<String, Boolean>();
        allMetrics.forEach((provider, metric) -> rateLimited.put(provider, metric.rateLimited()));

        var circuitBreakers = stockDataFetcher.getProviderCircuitBreakerStatus();
        long openCircuitCount = circuitBreakers.values().stream().filter(MultiProviderStockDataFetcher.CircuitBreakerStatus::open).count();

        long uptimeMs = Math.max(0L, ManagementFactory.getRuntimeMXBean().getUptime());

        return ResponseEntity.ok(new DashboardStatusResponse(
            "UP",
            startedAt,
            uptimeMs,
            allMetrics.size(),
            (int) openCircuitCount,
            rateLimited,
            circuitBreakers,
            LocalDateTime.now()
        ));
    }

    private record MetricsSummaryResponse(
        int totalRequests,
        int totalDailyLimit,
        String averageUsagePercent,
        int providersRateLimited,
        ConcurrentHashMap<String, ApiUsageTracker.ApiUsageDto> providers,
        LocalDateTime timestamp
    ) {}

    private record DashboardStatusResponse(
        String status,
        Instant startedAt,
        long uptimeMs,
        int providerCount,
        int openCircuitBreakers,
        Map<String, Boolean> rateLimitedProviders,
        Map<String, MultiProviderStockDataFetcher.CircuitBreakerStatus> circuitBreakers,
        LocalDateTime timestamp
    ) {}
}
