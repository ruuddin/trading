package com.example.trading.controller;

import com.example.trading.repository.UserRepository;
import com.example.trading.service.PortfolioAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final UserRepository users;
    private final PortfolioAnalyticsService analytics;

    public AnalyticsController(UserRepository users, PortfolioAnalyticsService analytics) {
        this.users = users;
        this.analytics = analytics;
    }

    @GetMapping("/portfolio-summary")
    public ResponseEntity<?> portfolioSummary(java.security.Principal principal) {
        var user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body("unauthenticated");

        return ResponseEntity.ok(analytics.getPortfolioSummary(user.getId()));
    }

    @GetMapping("/performance")
    public ResponseEntity<?> performance(java.security.Principal principal,
                                         @RequestParam(defaultValue = "1M") String range) {
        var user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body("unauthenticated");

        return ResponseEntity.ok(analytics.getPerformanceSeries(user.getId(), range));
    }

    private com.example.trading.model.User resolveUser(java.security.Principal principal) {
        if (principal == null) return null;
        return users.findByUsername(principal.getName()).orElse(null);
    }
}
