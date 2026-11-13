package com.onlyrooms.tenant.dto;

import com.onlyrooms.tenant.entity.TenantType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class TenantProfileRequest {

    @NotNull(message = "Tenant type is required")
    private TenantType tenantType;

    @Size(max = 100, message = "Occupation cannot exceed 100 characters")
    private String occupation;

    @DecimalMin(value = "0.0", message = "Budget cannot be negative")
    private BigDecimal monthlyBudget;

    private List<
            @Size(max = 100, message = "Location name is too long")
                    String
            > preferredLocations;

    public TenantType getTenantType() {
        return tenantType;
    }

    public void setTenantType(TenantType tenantType) {
        this.tenantType = tenantType;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public List<String> getPreferredLocations() {
        return preferredLocations;
    }

    public void setPreferredLocations(List<String> preferredLocations) {
        this.preferredLocations = preferredLocations;
    }
}