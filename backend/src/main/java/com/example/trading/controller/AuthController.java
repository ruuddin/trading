package com.example.trading.controller;

import com.example.trading.model.User;
import com.example.trading.repository.UserRepository;
import com.example.trading.security.JwtUtil;
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

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.users = users;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) return ResponseEntity.badRequest().build();
        if (users.findByUsername(username).isPresent()) return ResponseEntity.status(409).body("user exists");
        User u = new User(username, encoder.encode(password));
        users.save(u);
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
        Map<String,String> result = Collections.singletonMap("token", jwtUtil.generateToken(user.getUsername()));
        return ResponseEntity.ok(result);
    }
}
