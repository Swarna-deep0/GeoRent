package com.onlyrooms.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TenantProfileUnlockRequest {

    @NotBlank(message = "OTP is required.")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "OTP must contain exactly six digits."
    )
    private String otp;

    public TenantProfileUnlockRequest() {
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}