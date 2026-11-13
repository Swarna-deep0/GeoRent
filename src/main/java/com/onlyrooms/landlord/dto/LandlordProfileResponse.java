package com.onlyrooms.landlord.dto;

import com.onlyrooms.landlord.entity.LandlordProfile;
import com.onlyrooms.landlord.entity.LandlordVerificationStatus;

import java.time.LocalDateTime;

public class LandlordProfileResponse {

    private String id;
    private String userId;
    private String landlordType;
    private String address;
    private String city;
    private String state;
    private String postalCode;

    private boolean profileLocked;

    private LandlordVerificationStatus verificationStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LandlordProfileResponse(LandlordProfile profile) {

        this.id = profile.getId();
        this.userId = profile.getUserId();
        this.landlordType = profile.getLandlordType();
        this.address = profile.getAddress();
        this.city = profile.getCity();
        this.state = profile.getState();
        this.postalCode = profile.getPostalCode();

        this.profileLocked = profile.isProfileLocked();

        this.verificationStatus = profile.getVerificationStatus();

        this.createdAt = profile.getCreatedAt();
        this.updatedAt = profile.getUpdatedAt();
    }

    public String getId() { return id; }

    public String getUserId() { return userId; }

    public String getLandlordType() { return landlordType; }

    public String getAddress() { return address; }

    public String getCity() { return city; }

    public String getState() { return state; }

    public String getPostalCode() { return postalCode; }

    public boolean isProfileLocked() { return profileLocked; }

    public LandlordVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}