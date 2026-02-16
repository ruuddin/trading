package com.example.trading.controller;

import com.example.trading.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<?> listMyAuditLogs(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("unauthenticated");
        return ResponseEntity.ok(auditLogService.listForActor(principal.getName()));
    }
}
