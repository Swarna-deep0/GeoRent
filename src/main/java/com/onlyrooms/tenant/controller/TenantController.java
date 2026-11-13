package com.onlyrooms.tenant.controller;

import com.onlyrooms.tenant.dto.TenantProfileRequest;
import com.onlyrooms.tenant.dto.TenantProfileResponse;
import com.onlyrooms.tenant.service.TenantService;
import com.onlyrooms.auth.service.OtpService;
import com.onlyrooms.tenant.dto.TenantProfileUnlockRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private final TenantService tenantService;
    private final OtpService otpService;

    public TenantController(
            TenantService tenantService,
            OtpService otpService) {

        this.tenantService = tenantService;
        this.otpService = otpService;
    }

    @PostMapping("/profile/unlock/request-otp")
    public ResponseEntity<String> requestUnlockOtp(
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();

        String response =
                otpService.requestTenantProfileUnlockOtp(userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/profile/unlock/verify-otp")
    public ResponseEntity<TenantProfileResponse> verifyUnlockOtp(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantProfileUnlockRequest request) {

        String userId = jwt.getSubject();

        otpService.verifyTenantProfileUnlockOtp(
                userId,
                request.getOtp()
        );

        TenantProfileResponse response =
                tenantService.unlockProfile(userId);

        return ResponseEntity.ok(response);
    }

    // Create tenant profile

    @PostMapping("/profile")
    public ResponseEntity<TenantProfileResponse> createProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantProfileRequest request) {

        String userId = jwt.getSubject();

        TenantProfileResponse response =
                tenantService.createProfile(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<TenantProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantProfileRequest request) {

        String userId = jwt.getSubject();

        TenantProfileResponse response =
                tenantService.updateProfile(userId, request);

        return ResponseEntity.ok(response);
    }

    // Get authenticated tenant profile

    @GetMapping("/profile")
    public ResponseEntity<TenantProfileResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();

        TenantProfileResponse response =
                tenantService.getMyProfile(userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/profile/lock")
    public ResponseEntity<TenantProfileResponse> lockProfile(
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();

        TenantProfileResponse response =
                tenantService.lockProfile(userId);

        return ResponseEntity.ok(response);
    }
}