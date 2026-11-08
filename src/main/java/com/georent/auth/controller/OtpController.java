package com.georent.auth.controller;

import com.georent.auth.dto.OtpRequest;
import com.georent.auth.dto.OtpVerificationRequest;
import com.georent.auth.service.OtpService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(
            @Valid @RequestBody OtpRequest request) {

        String message = otpService.requestOtp(
                request.getDestination(),
                request.getPurpose()
        );

        return ResponseEntity.ok(
                Map.of("message", message)
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request) {

        String message = otpService.verifyOtp(
                request.getDestination(),
                request.getOtp(),
                request.getPurpose()
        );

        return ResponseEntity.ok(
                Map.of("message", message)
        );
    }
}