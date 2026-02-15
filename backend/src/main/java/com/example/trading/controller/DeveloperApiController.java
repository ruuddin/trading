package com.example.trading.controller;

import com.example.trading.model.ApiKey;
import com.example.trading.model.PlanTier;
import com.example.trading.repository.ApiKeyRepository;
import com.example.trading.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dev")
public class DeveloperApiController {

    private final UserRepository users;
    private final ApiKeyRepository apiKeys;
    private final PasswordEncoder encoder;

    public DeveloperApiController(UserRepository users, ApiKeyRepository apiKeys, PasswordEncoder encoder) {
        this.users = users;
        this.apiKeys = apiKeys;
        this.encoder = encoder;
    }

    @PostMapping("/keys")
    public ResponseEntity<?> createKey(java.security.Principal principal, @RequestBody(required = false) Map<String, Object> body) {
        var user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body("unauthenticated");
        if (user.getPlanTier() != PlanTier.PREMIUM) return ResponseEntity.status(403).body("upgrade required: PREMIUM");

        String rawName = body == null ? null : (body.get("name") instanceof String s ? s : null);
        String name = (rawName == null || rawName.isBlank()) ? "Default Key" : rawName.trim();

        String rawKey = generateRawKey();
        String prefix = rawKey.substring(0, 10);
        String last4 = rawKey.substring(rawKey.length() - 4);

        ApiKey saved = apiKeys.save(new ApiKey(
            user.getId(),
            name,
            prefix,
            last4,
            encoder.encode(rawKey)
        ));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", saved.getId());
        response.put("name", saved.getName());
        response.put("keyPrefix", saved.getKeyPrefix());
        response.put("last4", saved.getLast4());
        response.put("apiKey", rawKey);
        response.put("status", saved.getStatus());
        response.put("createdAt", saved.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/usage")
    public ResponseEntity<?> usage(java.security.Principal principal) {
        var user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).body("unauthenticated");
        if (user.getPlanTier() != PlanTier.PREMIUM) return ResponseEntity.status(403).body("upgrade required: PREMIUM");

        List<ApiKey> keys = apiKeys.findByUserIdOrderByCreatedAtDesc(user.getId());

        long totalRequests = keys.stream().mapToLong(ApiKey::getTotalRequests).sum();
        int requestsToday = keys.stream().mapToInt(ApiKey::getRequestsToday).sum();
        long activeKeys = keys.stream().filter(k -> "ACTIVE".equalsIgnoreCase(k.getStatus())).count();

        List<Map<String, Object>> keyItems = keys.stream().map(this::toUsageItem).toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("activeKeys", activeKeys);
        summary.put("totalRequests", totalRequests);
        summary.put("requestsToday", requestsToday);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("keys", keyItems);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toUsageItem(ApiKey key) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", key.getId());
        item.put("name", key.getName());
        item.put("keyPrefix", key.getKeyPrefix());
        item.put("status", key.getStatus());
        item.put("createdAt", key.getCreatedAt());
        item.put("lastUsedAt", key.getLastUsedAt());
        item.put("totalRequests", key.getTotalRequests());
        item.put("requestsToday", key.getRequestsToday());
        return item;
    }

    private com.example.trading.model.User resolveUser(java.security.Principal principal) {
        if (principal == null) return null;
        return users.findByUsername(principal.getName()).orElse(null);
    }

    private String generateRawKey() {
        String token = UUID.randomUUID().toString().replace("-", "");
        return "trd_" + token;
    }
}
