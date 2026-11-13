package com.onlyrooms.auth.dto;

import com.onlyrooms.auth.entity.OtpPurpose;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OtpRequest {

    @NotBlank(message = "Email or phone is required")
    private String destination;

    @NotNull(message = "OTP purpose is required")
    private OtpPurpose purpose;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }
}