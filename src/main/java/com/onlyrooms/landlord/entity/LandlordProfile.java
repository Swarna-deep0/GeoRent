package com.onlyrooms.landlord.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import com.onlyrooms.landlord.entity.LandlordVerificationStatus;

@Document(collection = "landlord_profiles")
public class LandlordProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String landlordType;
    private String address;
    private String city;
    private String state;
    private String postalCode;

    private boolean profileLocked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LandlordVerificationStatus verificationStatus =
            LandlordVerificationStatus.PENDING;

    public LandlordProfile() {
    }

    public LandlordProfile(String userId, String landlordType) {
        this.userId = userId;
        this.landlordType = landlordType;
        this.profileLocked = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLandlordType() { return landlordType; }
    public void setLandlordType(String landlordType) { this.landlordType = landlordType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public boolean isProfileLocked() { return profileLocked; }
    public void setProfileLocked(boolean profileLocked) {
        this.profileLocked = profileLocked;
    }

    public LandlordVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(
            LandlordVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}