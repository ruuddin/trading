package com.example.trading.service;

import com.example.trading.model.AuditLog;
import com.example.trading.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogs;

    public AuditLogService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    public AuditLog record(String actorUsername, String action, String entityType, String entityId, String details) {
        return auditLogs.save(new AuditLog(actorUsername, action, entityType, entityId, details));
    }

    public List<AuditLog> listForActor(String actorUsername) {
        return auditLogs.findTop200ByActorUsernameOrderByCreatedAtDesc(actorUsername);
    }
}
