package com.example.trading.repository;

import com.example.trading.model.SavedScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedScanRepository extends JpaRepository<SavedScan, Long> {
    List<SavedScan> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
