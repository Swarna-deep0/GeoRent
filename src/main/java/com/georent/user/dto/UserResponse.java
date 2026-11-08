package com.georent.user.dto;

import com.georent.user.entity.UserRole;

import java.time.LocalDateTime;

public class UserResponse {

    private String id;
    private String fullName;
    private String email;
    private String phone;
    private UserRole role;
    private boolean emailVerified;
    private boolean phoneVerified;
    private LocalDateTime createdAt;

    public UserResponse(String id, String fullName, String email,
                        String phone, UserRole role,
                        boolean emailVerified,
                        boolean phoneVerified,
                        LocalDateTime createdAt) {

        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}