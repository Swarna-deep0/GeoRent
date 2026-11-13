package com.onlyrooms.tenant.dto;

import com.onlyrooms.tenant.entity.TenantType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TenantProfileResponse {

    private String id;
    private String userId;
    private TenantType tenantType;
    private String occupation;
    private BigDecimal monthlyBudget;
    private List<String> preferredLocations;
    private boolean profileLocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TenantProfileResponse(
            String id,
            String userId,
            TenantType tenantType,
            String occupation,
            BigDecimal monthlyBudget,
            List<String> preferredLocations,
            boolean profileLocked,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        this.id = id;
        this.userId = userId;
        this.tenantType = tenantType;
        this.occupation = occupation;
        this.monthlyBudget = monthlyBudget;
        this.preferredLocations = preferredLocations;
        this.profileLocked = profileLocked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public TenantType getTenantType() {
        return tenantType;
    }

    public String getOccupation() {
        return occupation;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public List<String> getPreferredLocations() {
        return preferredLocations;
    }

    public boolean isProfileLocked() {
        return profileLocked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}