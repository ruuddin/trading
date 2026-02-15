package com.example.trading.controller;

import com.example.trading.service.ApiUsageTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for API usage metrics and monitoring
 */
@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricsController {

    @Autowired
    private ApiUsageTracker apiUsageTracker;

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

    private record MetricsSummaryResponse(
        int totalRequests,
        int totalDailyLimit,
        String averageUsagePercent,
        int providersRateLimited,
        ConcurrentHashMap<String, ApiUsageTracker.ApiUsageDto> providers,
        LocalDateTime timestamp
    ) {}
}
