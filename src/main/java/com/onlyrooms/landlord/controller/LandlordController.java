package com.onlyrooms.landlord.controller;

import com.onlyrooms.landlord.dto.LandlordProfileRequest;
import com.onlyrooms.landlord.dto.LandlordProfileResponse;
import com.onlyrooms.landlord.service.LandlordService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlord/profile")
public class LandlordController {

    private final LandlordService landlordService;

    public LandlordController(LandlordService landlordService) {
        this.landlordService = landlordService;
    }

    @PostMapping
    public ResponseEntity<LandlordProfileResponse> createProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LandlordProfileRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(landlordService.createProfile(
                        jwt.getSubject(), request));
    }

    @GetMapping
    public ResponseEntity<LandlordProfileResponse> getProfile(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                landlordService.getProfile(jwt.getSubject()));
    }

    @PutMapping
    public ResponseEntity<LandlordProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LandlordProfileRequest request) {

        return ResponseEntity.ok(
                landlordService.updateProfile(
                        jwt.getSubject(), request));
    }
}