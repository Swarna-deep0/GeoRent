package com.onlyrooms.auth.dto;

import com.onlyrooms.user.entity.UserRole;

public class LoginResponse {

    private String message;
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String userId;
    private String fullName;
    private UserRole role;

    public LoginResponse(String message, String accessToken,
                         long expiresIn, String userId,
                         String fullName, UserRole role) {

        this.message = message;
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.fullName = fullName;
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }
}