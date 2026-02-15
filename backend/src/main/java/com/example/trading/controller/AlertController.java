package com.example.trading.controller;

import com.example.trading.model.AlertRule;
import com.example.trading.repository.AlertRuleRepository;
import com.example.trading.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRuleRepository alertRules;
    private final UserRepository users;

    public AlertController(AlertRuleRepository alertRules, UserRepository users) {
        this.alertRules = alertRules;
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<?> list(java.security.Principal principal,
                                  @RequestParam(required = false) String symbol) {
        var user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body("unauthenticated");

        if (symbol == null || symbol.isBlank()) {
            return ResponseEntity.ok(alertRules.findByUserIdOrderByCreatedAtDesc(user.getId()));
        }

        return ResponseEntity.ok(alertRules.findByUserIdAndSymbolOrderByCreatedAtDesc(user.getId(), normalizeSymbol(symbol)));
    }

    @PostMapping
    public ResponseEntity<?> create(java.security.Principal principal, @RequestBody Map<String, Object> body) {
        var user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body("unauthenticated");

        Object rawSymbol = body.get("symbol");
        Object rawCondition = body.get("conditionType");
        Object rawTarget = body.get("targetPrice");

        if (!(rawSymbol instanceof String symbolInput) || symbolInput.isBlank()) {
            return ResponseEntity.badRequest().body("symbol is required");
        }
        if (!(rawCondition instanceof String conditionInput) || conditionInput.isBlank()) {
            return ResponseEntity.badRequest().body("conditionType is required");
        }
        if (!(rawTarget instanceof Number targetNumber)) {
            return ResponseEntity.badRequest().body("targetPrice is required");
        }

        String conditionType = conditionInput.trim().toUpperCase(Locale.ROOT);
        if (!conditionType.equals("ABOVE") && !conditionType.equals("BELOW")) {
            return ResponseEntity.badRequest().body("conditionType must be ABOVE or BELOW");
        }

        BigDecimal targetPrice = BigDecimal.valueOf(targetNumber.doubleValue());
        if (targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("targetPrice must be > 0");
        }

        AlertRule alert = new AlertRule(user.getId(), normalizeSymbol(symbolInput), conditionType, targetPrice);
        return ResponseEntity.ok(alertRules.save(alert));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(java.security.Principal principal, @PathVariable Long id) {
        var user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body("unauthenticated");

        var alert = alertRules.findByIdAndUserId(id, user.getId()).orElse(null);
        if (alert == null) return ResponseEntity.status(404).body("alert not found");

        alertRules.delete(alert);
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }

    private com.example.trading.model.User resolveUser(java.security.Principal principal) {
        if (principal == null) return null;
        return users.findByUsername(principal.getName()).orElse(null);
    }

    private String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
