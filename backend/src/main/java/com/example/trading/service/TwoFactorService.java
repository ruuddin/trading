package com.example.trading.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Service
public class TwoFactorService {

    private final SecureRandom random = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public String currentCode(String secret) {
        return codeForWindow(secret, System.currentTimeMillis() / 30000L);
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || code.isBlank()) return false;

        long window = System.currentTimeMillis() / 30000L;
        String current = codeForWindow(secret, window);
        String previous = codeForWindow(secret, window - 1);
        String next = codeForWindow(secret, window + 1);
        return code.equals(current) || code.equals(previous) || code.equals(next);
    }

    private String codeForWindow(String secret, long window) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((secret + ":" + window).getBytes(StandardCharsets.UTF_8));

            int value = ((hash[0] & 0x7F) << 24)
                | ((hash[1] & 0xFF) << 16)
                | ((hash[2] & 0xFF) << 8)
                | (hash[3] & 0xFF);

            int codeInt = Math.floorMod(value, 1_000_000);
            return String.format("%06d", codeInt);
        } catch (Exception ex) {
            return "000000";
        }
    }
}
