package com.example.trading.repository;

import com.example.trading.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<AlertRule> findByUserIdAndSymbolOrderByCreatedAtDesc(Long userId, String symbol);
    Optional<AlertRule> findByIdAndUserId(Long id, Long userId);
}
