package com.onlyrooms.auth.service;

import com.onlyrooms.auth.delivery.OtpDeliveryService;
import com.onlyrooms.auth.entity.OtpChallenge;
import com.onlyrooms.auth.entity.OtpPurpose;
import com.onlyrooms.auth.repository.OtpRepository;
import com.onlyrooms.user.entity.User;
import com.onlyrooms.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import com.onlyrooms.tenant.entity.TenantProfile;
import com.onlyrooms.tenant.repository.TenantProfileRepository;
import com.onlyrooms.user.entity.UserRole;

@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDeliveryService;
    private final TenantProfileRepository tenantProfileRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            OtpRepository otpRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpDeliveryService otpDeliveryService,
            TenantProfileRepository tenantProfileRepository) {

        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpDeliveryService = otpDeliveryService;
        this.tenantProfileRepository = tenantProfileRepository;
    }

    // Existing email/phone OTP functionality

    public String requestOtp(String destination, OtpPurpose purpose) {

        if (purpose == OtpPurpose.PASSWORD_RESET) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use the forgot password endpoint."
            );
        }

        destination = normalizeDestination(destination, purpose);

        User user = findUser(destination, purpose);

        if (purpose == OtpPurpose.EMAIL_VERIFICATION
                && user.isEmailVerified()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is already verified."
            );
        }

        if (purpose == OtpPurpose.PHONE_VERIFICATION
                && user.isPhoneVerified()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone number is already verified."
            );
        }

        Instant now = Instant.now();

        List<OtpChallenge> activeChallenges =
                otpRepository.findByUserIdAndPurposeAndConsumedFalse(
                        user.getId(), purpose
                );

        for (OtpChallenge challenge : activeChallenges) {

            if (Duration.between(
                    challenge.getLastSentAt(), now).getSeconds()
                    < RESEND_COOLDOWN_SECONDS) {

                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait 60 seconds before requesting another OTP."
                );
            }

            challenge.setConsumed(true);
            otpRepository.save(challenge);
        }

        String otp = generateOtp();

        String otpHash = passwordEncoder.encode(otp);

        OtpChallenge challenge = new OtpChallenge(
                user.getId(),
                destination,
                purpose,
                otpHash,
                now,
                now.plus(Duration.ofMinutes(OTP_EXPIRY_MINUTES)),
                now
        );

        otpRepository.save(challenge);

        otpDeliveryService.sendOtp(destination, otp, purpose);

        return "OTP sent successfully. It expires in 5 minutes.";
    }

    public String verifyOtp(
            String destination,
            String otp,
            OtpPurpose purpose) {

        if (purpose == OtpPurpose.PASSWORD_RESET) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use the password reset endpoint."
            );
        }

        destination = normalizeDestination(destination, purpose);

        User user = findUser(destination, purpose);

        Instant now = Instant.now();

        OtpChallenge challenge = otpRepository
                .findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                        user.getId(), purpose
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No active OTP found."
                ));

        if (!challenge.getDestination().equals(destination)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP destination."
            );
        }

        if (!now.isBefore(challenge.getExpiresAt())) {

            challenge.setConsumed(true);
            otpRepository.save(challenge);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP has expired. Please request a new OTP."
            );
        }

        if (challenge.getAttempts() >= MAX_ATTEMPTS) {

            challenge.setConsumed(true);
            otpRepository.save(challenge);

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum OTP attempts exceeded."
            );
        }

        boolean matches = passwordEncoder.matches(
                otp, challenge.getOtpHash()
        );

        if (!matches) {

            challenge.setAttempts(challenge.getAttempts() + 1);

            if (challenge.getAttempts() >= MAX_ATTEMPTS) {
                challenge.setConsumed(true);
            }

            otpRepository.save(challenge);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Incorrect OTP."
            );
        }

        challenge.setConsumed(true);
        otpRepository.save(challenge);

        if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
            user.setEmailVerified(true);
        } else {
            user.setPhoneVerified(true);
        }

        userRepository.save(user);

        return "OTP verified successfully.";
    }

    // Forgot password: request OTP

    public String requestPasswordResetOtp(String email) {

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        // Do not reveal whether an account exists.
        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null || !user.isEmailVerified()) {
            return "If a verified account exists for this email, a password reset OTP has been sent.";
        }

        Instant now = Instant.now();

        List<OtpChallenge> activeChallenges =
                otpRepository.findByUserIdAndPurposeAndConsumedFalse(
                        user.getId(), OtpPurpose.PASSWORD_RESET
                );

        for (OtpChallenge challenge : activeChallenges) {

            if (Duration.between(
                    challenge.getLastSentAt(), now).getSeconds()
                    < RESEND_COOLDOWN_SECONDS) {

                return "If a verified account exists for this email, a password reset OTP has been sent.";
            }

            challenge.setConsumed(true);
            otpRepository.save(challenge);
        }

        String otp = generateOtp();

        OtpChallenge challenge = new OtpChallenge(
                user.getId(),
                normalizedEmail,
                OtpPurpose.PASSWORD_RESET,
                passwordEncoder.encode(otp),
                now,
                now.plus(Duration.ofMinutes(OTP_EXPIRY_MINUTES)),
                now
        );

        otpRepository.save(challenge);

        otpDeliveryService.sendOtp(
                normalizedEmail,
                otp,
                OtpPurpose.PASSWORD_RESET
        );

        return "If a verified account exists for this email, a password reset OTP has been sent.";
    }

    // Tenant profile unlock: request OTP

    public String requestTenantProfileUnlockOtp(String userId) {

        // 1. Find authenticated user

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        // 2. Verify TENANT role

        if (user.getRole() != UserRole.TENANT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can unlock tenant profiles."
            );
        }

        // 3. Verify registered email

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Verify your email before unlocking your profile."
            );
        }

        String email = normalizeDestination(
                user.getEmail(),
                OtpPurpose.EMAIL_VERIFICATION
        );

        // 4. Find tenant profile

        TenantProfile profile = tenantProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant profile not found."
                ));

        // 5. Check profile lock status

        if (!profile.isProfileLocked()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tenant profile is already unlocked."
            );
        }

        Instant now = Instant.now();

        // 6. Check existing unlock challenges

        List<OtpChallenge> activeChallenges =
                otpRepository.findByUserIdAndPurposeAndConsumedFalse(
                        userId,
                        OtpPurpose.TENANT_PROFILE_UNLOCK
                );

        for (OtpChallenge challenge : activeChallenges) {

            if (Duration.between(
                    challenge.getLastSentAt(), now).getSeconds()
                    < RESEND_COOLDOWN_SECONDS) {

                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait 60 seconds before requesting another OTP."
                );
            }

            challenge.setConsumed(true);
            otpRepository.save(challenge);
        }

        // 7. Generate OTP

        String otp = generateOtp();

        String otpHash = passwordEncoder.encode(otp);

        // 8. Create challenge

        OtpChallenge challenge = new OtpChallenge(
                userId,
                email,
                OtpPurpose.TENANT_PROFILE_UNLOCK,
                otpHash,
                now,
                now.plus(Duration.ofMinutes(OTP_EXPIRY_MINUTES)),
                now
        );

        otpRepository.save(challenge);

        // 9. Deliver OTP

        otpDeliveryService.sendOtp(
                email,
                otp,
                OtpPurpose.TENANT_PROFILE_UNLOCK
        );

        return "Tenant profile unlock OTP sent successfully. It expires in 5 minutes.";
    }

    // Tenant profile unlock: verify OTP

    public String verifyTenantProfileUnlockOtp(
            String userId,
            String otp) {

        // 1. Validate OTP input

        if (otp == null || !otp.matches("\\d{6}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid six-digit OTP."
            );
        }

        // 2. Find user

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        // 3. Verify role

        if (user.getRole() != UserRole.TENANT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can unlock tenant profiles."
            );
        }

        // 4. Verify email

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Verified email is required."
            );
        }

        // 5. Verify profile exists and is locked

        TenantProfile profile = tenantProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant profile not found."
                ));

        if (!profile.isProfileLocked()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tenant profile is already unlocked."
            );
        }

        // 6. Find latest active unlock OTP

        OtpChallenge challenge = otpRepository
                .findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                        userId,
                        OtpPurpose.TENANT_PROFILE_UNLOCK
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No active unlock OTP found."
                ));

        String email = normalizeDestination(
                user.getEmail(),
                OtpPurpose.EMAIL_VERIFICATION
        );

        if (!challenge.getDestination().equals(email)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP destination."
            );
        }

        Instant now = Instant.now();

        // 7. Check expiry

        if (!now.isBefore(challenge.getExpiresAt())) {

            challenge.setConsumed(true);
            otpRepository.save(challenge);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP expired. Request a new OTP."
            );
        }

        // 8. Check maximum attempts

        if (challenge.getAttempts() >= MAX_ATTEMPTS) {

            challenge.setConsumed(true);
            otpRepository.save(challenge);

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum OTP attempts exceeded."
            );
        }

        // 9. Verify BCrypt hash

        boolean matches = passwordEncoder.matches(
                otp,
                challenge.getOtpHash()
        );

        if (!matches) {

            challenge.setAttempts(challenge.getAttempts() + 1);

            if (challenge.getAttempts() >= MAX_ATTEMPTS) {
                challenge.setConsumed(true);
            }

            otpRepository.save(challenge);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Incorrect OTP."
            );
        }

        // 10. Consume OTP

        challenge.setConsumed(true);
        otpRepository.save(challenge);

        return "Tenant profile unlock OTP verified successfully.";
    }

    // Forgot password: verify OTP and update password

    public String resetPassword(
            String email,
            String otp,
            String newPassword,
            String confirmPassword) {

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password and confirmation password do not match."
            );
        }

        int passwordBytes = newPassword.getBytes(
                StandardCharsets.UTF_8
        ).length;

        if (passwordBytes > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes."
            );
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid or expired password reset OTP."
                ));

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email must be verified before resetting the password."
            );
        }

        Instant now = Instant.now();

        OtpChallenge challenge = otpRepository
                .findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                        user.getId(),
                        OtpPurpose.PASSWORD_RESET
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid or expired password reset OTP."
                ));

        if (!challenge.getDestination().equals(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired password reset OTP."
            );
        }

        if (!now.isBefore(challenge.getExpiresAt())) {

            challenge.setConsumed(true);
            otpRepository.save(challenge);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP has expired. Please request a new one."
            );
        }

        if (challenge.getAttempts() >= MAX_ATTEMPTS) {

            challenge.setConsumed(true);
            otpRepository.save(challenge);

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum OTP attempts exceeded."
            );
        }

        boolean matches = passwordEncoder.matches(
                otp, challenge.getOtpHash()
        );

        if (!matches) {

            challenge.setAttempts(challenge.getAttempts() + 1);

            if (challenge.getAttempts() >= MAX_ATTEMPTS) {
                challenge.setConsumed(true);
            }

            otpRepository.save(challenge);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Incorrect OTP."
            );
        }

        // Consume the OTP before changing the password.
        challenge.setConsumed(true);
        otpRepository.save(challenge);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return "Password reset successfully. You can now log in.";
    }

    private String generateOtp() {

        int number = secureRandom.nextInt(1_000_000);

        return String.format("%06d", number);
    }

    private User findUser(String destination, OtpPurpose purpose) {

        return (purpose == OtpPurpose.EMAIL_VERIFICATION
                ? userRepository.findByEmail(destination)
                : userRepository.findByPhone(destination))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No registered user found."
                ));
    }

    private String normalizeDestination(
            String destination,
            OtpPurpose purpose) {

        String normalized = destination.trim();

        if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
            return normalized.toLowerCase(Locale.ROOT);
        }

        return normalized;
    }
}