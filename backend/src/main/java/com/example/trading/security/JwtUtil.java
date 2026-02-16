package com.example.trading.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // Use the configured secret; ensure it's long enough in production
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username) {
        return generateToken(username, 0);
    }

    public String generateToken(String username, int tokenVersion) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("tv", tokenVersion)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 1000L * 60 * 60 * 24 * 7)) // 7 days
                .signWith(key)
                .compact();
    }

    public String getUsername(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public int getTokenVersion(String token) {
        try {
            Integer version = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("tv", Integer.class);
            return version == null ? 0 : version;
        } catch (JwtException e) {
            return 0;
        }
    }
}
