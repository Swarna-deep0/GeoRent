package com.onlyrooms.auth.controller;

import com.onlyrooms.auth.dto.ForgotPasswordRequest;
import com.onlyrooms.auth.dto.LoginRequest;
import com.onlyrooms.auth.dto.LoginResponse;
import com.onlyrooms.auth.dto.ResetPasswordRequest;
import com.onlyrooms.auth.service.AuthService;
import com.onlyrooms.auth.service.OtpService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(
            AuthService authService,
            OtpService otpService) {

        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<Map<String, String>> requestPasswordResetOtp(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String message = otpService.requestPasswordResetOtp(
                request.getEmail()
        );

        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        String message = otpService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );

        return ResponseEntity.ok(Map.of("message", message));
    }
}