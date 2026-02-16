package com.example.trading.repository;

import com.example.trading.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByActorUsernameOrderByCreatedAtDesc(String actorUsername);
}
