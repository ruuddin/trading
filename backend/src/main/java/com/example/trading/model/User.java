package com.example.trading.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    private PlanTier planTier;
    private LocalDateTime trialEndsAt;
    private String billingStatus;
    private Integer tokenVersion;

    public User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.planTier = PlanTier.FREE;
        this.trialEndsAt = LocalDateTime.now().plusDays(14);
        this.billingStatus = "TRIAL";
        this.tokenVersion = 0;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public PlanTier getPlanTier() { return planTier; }
    public LocalDateTime getTrialEndsAt() { return trialEndsAt; }
    public String getBillingStatus() { return billingStatus; }
    public Integer getTokenVersion() { return tokenVersion == null ? 0 : tokenVersion; }

    public void setPlanTier(PlanTier planTier) { this.planTier = planTier; }
    public void setTrialEndsAt(LocalDateTime trialEndsAt) { this.trialEndsAt = trialEndsAt; }
    public void setBillingStatus(String billingStatus) { this.billingStatus = billingStatus; }
    public void setTokenVersion(Integer tokenVersion) { this.tokenVersion = tokenVersion; }
}
