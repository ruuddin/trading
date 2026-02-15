package com.example.trading.controller;

import com.example.trading.model.PlanTier;
import com.example.trading.repository.UserRepository;
import com.example.trading.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final UserRepository users;
    private final BillingService billingService;

    public BillingController(UserRepository users, BillingService billingService) {
        this.users = users;
        this.billingService = billingService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        var userOpt = users.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("unknown user");

        var user = userOpt.get();
        PlanTier tier = user.getPlanTier() == null ? PlanTier.FREE : user.getPlanTier();
        String billingStatus = user.getBillingStatus() == null ? "TRIAL" : user.getBillingStatus();
        LocalDateTime trialEndsAt = user.getTrialEndsAt();

        return ResponseEntity.ok(Map.of(
            "username", user.getUsername(),
            "planTier", tier,
            "billingStatus", billingStatus,
            "trialEndsAt", trialEndsAt,
            "trialActive", trialEndsAt != null && trialEndsAt.isAfter(LocalDateTime.now())
        ));
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<?> checkoutSession(java.security.Principal principal, @RequestBody Map<String, Object> body) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");

        Object rawPlan = body.get("planTier");
        if (!(rawPlan instanceof String requestedPlan) || requestedPlan.isBlank()) {
            return ResponseEntity.badRequest().body("planTier is required");
        }

        try {
            PlanTier.valueOf(requestedPlan.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("invalid planTier");
        }

        return ResponseEntity.ok(billingService.createCheckoutSessionPlaceholder(principal.getName(), requestedPlan));
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        boolean applied = billingService.applyWebhookEvent(payload);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("accepted", applied));
    }
}
