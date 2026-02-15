package com.example.trading.service;

import com.example.trading.model.PlanTier;
import com.example.trading.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class BillingService {

    private final UserRepository users;

    public BillingService(UserRepository users) {
        this.users = users;
    }

    public boolean applyWebhookEvent(Map<String, Object> payload) {
        String username = extractString(payload, "username");
        if (username == null) {
            Object data = payload.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                username = extractString((Map<String, Object>) dataMap, "username");
                if (username == null) {
                    Object object = dataMap.get("object");
                    if (object instanceof Map<?, ?> objectMap) {
                        Object metadata = ((Map<?, ?>) objectMap).get("metadata");
                        if (metadata instanceof Map<?, ?> metadataMap) {
                            Object u = metadataMap.get("username");
                            if (u instanceof String value && !value.isBlank()) {
                                username = value;
                            }
                        }
                    }
                }
            }
        }

        if (username == null || username.isBlank()) {
            return false;
        }

        var userOpt = users.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }

        var user = userOpt.get();

        String planRaw = extractString(payload, "planTier");
        if (planRaw == null) {
            Object data = payload.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                planRaw = extractString((Map<String, Object>) dataMap, "planTier");
            }
        }

        String statusRaw = extractString(payload, "billingStatus");
        if (statusRaw == null) {
            Object data = payload.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                statusRaw = extractString((Map<String, Object>) dataMap, "billingStatus");
            }
        }

        PlanTier nextTier = resolvePlanTier(planRaw);
        user.setPlanTier(nextTier);
        user.setBillingStatus(statusRaw == null || statusRaw.isBlank() ? "ACTIVE" : statusRaw.toUpperCase(Locale.ROOT));

        if (nextTier == PlanTier.FREE) {
            user.setTrialEndsAt(LocalDateTime.now().plusDays(14));
        } else {
            user.setTrialEndsAt(LocalDateTime.now().plusYears(1));
        }

        users.save(user);
        return true;
    }

    public Map<String, Object> createCheckoutSessionPlaceholder(String username, String planTier) {
        PlanTier requestedPlan = resolvePlanTier(planTier);

        return Map.of(
            "username", username,
            "requestedPlan", requestedPlan,
            "status", "NOT_CONFIGURED",
            "sessionId", "mock_session_" + System.currentTimeMillis(),
            "checkoutUrl", "/pricing?checkout=mock&plan=" + requestedPlan
        );
    }

    private PlanTier resolvePlanTier(String raw) {
        if (raw == null || raw.isBlank()) {
            return PlanTier.FREE;
        }
        try {
            return PlanTier.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return PlanTier.FREE;
        }
    }

    private String extractString(Map<String, Object> map, String key) {
        Object raw = map.get(key);
        if (raw instanceof String s && !s.isBlank()) {
            return s;
        }
        return null;
    }
}
