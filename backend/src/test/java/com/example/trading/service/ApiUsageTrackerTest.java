package com.example.trading.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiUsageTrackerTest {

    @Test
    void recordAndReadMetricsForProvider() {
        ApiUsageTracker tracker = new ApiUsageTracker();

        assertTrue(tracker.recordRequest("ALPHA_VANTAGE"));
        assertTrue(tracker.canMakeRequest("ALPHA_VANTAGE"));

        ApiUsageTracker.ApiUsageDto metrics = tracker.getProviderMetrics("ALPHA_VANTAGE");

        assertEquals(1, metrics.dailyRequestCount());
        assertEquals(25, metrics.dailyLimit());
        assertEquals(1, metrics.minuteRequestCount());
        assertFalse(metrics.rateLimited());
    }

    @Test
    void unknownProviderIsHandledSafely() {
        ApiUsageTracker tracker = new ApiUsageTracker();

        assertFalse(tracker.recordRequest("UNKNOWN_PROVIDER"));

        ApiUsageTracker.ApiUsageDto metrics = tracker.getProviderMetrics("UNKNOWN_PROVIDER");
        assertTrue(metrics.rateLimited());
        assertEquals(0, metrics.dailyRequestCount());
    }
}
