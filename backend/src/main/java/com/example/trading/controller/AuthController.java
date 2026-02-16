package com.example.trading.controller;

import com.example.trading.model.User;
import com.example.trading.repository.UserRepository;
import com.example.trading.security.JwtUtil;
import com.example.trading.service.AuditLogService;
import com.example.trading.service.TwoFactorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;
    private final TwoFactorService twoFactorService;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtUtil jwtUtil, AuditLogService auditLogService, TwoFactorService twoFactorService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.auditLogService = auditLogService;
        this.twoFactorService = twoFactorService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) return ResponseEntity.badRequest().build();
        if (users.findByUsername(username).isPresent()) return ResponseEntity.status(409).body("user exists");
        User u = new User(username, encoder.encode(password));
        users.save(u);
        auditLogService.record(username, "AUTH_REGISTER", "USER", username, "User registration completed");
        return ResponseEntity.ok(Map.of("username", username));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) return ResponseEntity.badRequest().build();
        var userOpt = users.findByUsername(username);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("invalid credentials");
        var user = userOpt.get();
        if (!encoder.matches(password, user.getPasswordHash())) return ResponseEntity.status(401).body("invalid credentials");

        if (user.getTwoFactorEnabled()) {
            String twoFactorCode = body.get("twoFactorCode");
            if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), twoFactorCode)) {
                return ResponseEntity.status(401).body("invalid twoFactorCode");
            }
        }

        auditLogService.record(user.getUsername(), "AUTH_LOGIN", "USER", user.getUsername(), "User login succeeded");
        Map<String,String> result = Collections.singletonMap("token", jwtUtil.generateToken(user.getUsername(), user.getTokenVersion()));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setupTwoFactor(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");

        var userOpt = users.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("unknown user");

        var user = userOpt.get();
        String secret = twoFactorService.generateSecret();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(false);
        users.save(user);

        String currentCode = twoFactorService.currentCode(secret);
        auditLogService.record(user.getUsername(), "AUTH_2FA_SETUP", "USER", user.getUsername(), "2FA setup initialized");

        return ResponseEntity.ok(Map.of(
            "twoFactorEnabled", false,
            "secret", secret,
            "verificationCode", currentCode
        ));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enableTwoFactor(java.security.Principal principal, @RequestBody Map<String, String> body) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");

        var userOpt = users.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("unknown user");

        var user = userOpt.get();
        String code = body.get("code");
        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            return ResponseEntity.status(400).body("invalid code");
        }

        user.setTwoFactorEnabled(true);
        users.save(user);
        auditLogService.record(user.getUsername(), "AUTH_2FA_ENABLE", "USER", user.getUsername(), "2FA enabled");
        return ResponseEntity.ok(Map.of("twoFactorEnabled", true));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disableTwoFactor(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");

        var userOpt = users.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("unknown user");

        var user = userOpt.get();
        user.setTwoFactorEnabled(false);
        users.save(user);
        auditLogService.record(user.getUsername(), "AUTH_2FA_DISABLE", "USER", user.getUsername(), "2FA disabled");
        return ResponseEntity.ok(Map.of("twoFactorEnabled", false));
    }

    @PostMapping("/sessions/revoke")
    public ResponseEntity<?> revokeAllSessions(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");

        var userOpt = users.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("unknown user");

        var user = userOpt.get();
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.save(user);

        auditLogService.record(user.getUsername(), "AUTH_SESSIONS_REVOKE", "USER", user.getUsername(), "All sessions revoked");
        return ResponseEntity.ok(Map.of("revoked", true));
    }
}
