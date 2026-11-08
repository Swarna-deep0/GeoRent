package com.georent.auth.service;

import com.georent.auth.delivery.OtpDeliveryService;
import com.georent.auth.entity.OtpChallenge;
import com.georent.auth.entity.OtpPurpose;
import com.georent.auth.repository.OtpRepository;

import com.georent.user.entity.User;
import com.georent.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

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

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            OtpRepository otpRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpDeliveryService otpDeliveryService) {

        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpDeliveryService = otpDeliveryService;
    }

    public String requestOtp(String destination, OtpPurpose purpose) {

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
                    challenge.getLastSentAt(), now
            ).getSeconds() < RESEND_COOLDOWN_SECONDS) {

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

        // Consume OTP after successful verification.

        challenge.setConsumed(true);
        otpRepository.save(challenge);

        // Update the corresponding User document.

        if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
            user.setEmailVerified(true);
        } else {
            user.setPhoneVerified(true);
        }

        userRepository.save(user);

        return "OTP verified successfully.";
    }

    private String generateOtp() {

        int number = secureRandom.nextInt(1_000_000);

        return String.format("%06d", number);
    }

    private User findUser(
            String destination,
            OtpPurpose purpose) {

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