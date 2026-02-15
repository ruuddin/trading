package com.example.trading.repository;

import com.example.trading.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    List<ApiKey> findByUserIdOrderByCreatedAtDesc(Long userId);
}
