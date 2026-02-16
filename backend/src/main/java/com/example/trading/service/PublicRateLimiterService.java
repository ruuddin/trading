package com.example.trading.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PublicRateLimiterService {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public boolean allow(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now, 0));

        synchronized (counter) {
            if (now - counter.windowStart >= windowMillis) {
                counter.windowStart = now;
                counter.count = 0;
            }

            if (counter.count >= maxRequests) {
                return false;
            }

            counter.count++;
            return true;
        }
    }

    public void resetAll() {
        counters.clear();
    }

    private static final class WindowCounter {
        long windowStart;
        int count;

        WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
