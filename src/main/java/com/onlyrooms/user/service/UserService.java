package com.onlyrooms.user.service;

import com.onlyrooms.common.exception.EmailAlreadyRegisteredException;
import com.onlyrooms.common.exception.PhoneAlreadyRegisteredException;
import com.onlyrooms.common.exception.AdminRegistrationNotAllowedException;

import com.onlyrooms.user.dto.UserRegistrationRequest;
import com.onlyrooms.user.dto.UserResponse;
import com.onlyrooms.user.entity.User;
import com.onlyrooms.user.entity.UserRole;
import com.onlyrooms.user.repository.UserRepository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getMyProfile(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User profile not found."
                ));

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getCreatedAt()
        );
    }

    public UserResponse registerUser(UserRegistrationRequest request) {

        // 1. Prevent public ADMIN registration

        if (request.getRole() == UserRole.ADMIN) {
            throw new AdminRegistrationNotAllowedException(
                    "Admin registration is not permitted."
            );
        }

        // 2. Normalize email and phone

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        String phone = request.getPhone().trim();

        // 3. Validate BCrypt password length

        if (request.getPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new IllegalArgumentException(
                    "Password must not exceed 72 UTF-8 bytes."
            );
        }

        // 4. Check duplicate email

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(
                    "Email already registered."
            );
        }

        // 5. Check duplicate phone

        if (userRepository.existsByPhone(phone)) {
            throw new PhoneAlreadyRegisteredException(
                    "Phone number already registered."
            );
        }

        // 6. Hash password

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        // 7. Create user

        User user = new User(
                request.getFullName().trim(),
                email,
                phone,
                encodedPassword,
                request.getRole()
        );

        // 8. Save user and handle concurrent duplicates

        try {

            User savedUser = userRepository.save(user);

            // 9. Return safe response DTO

            return new UserResponse(
                    savedUser.getId(),
                    savedUser.getFullName(),
                    savedUser.getEmail(),
                    savedUser.getPhone(),
                    savedUser.getRole(),
                    savedUser.isEmailVerified(),
                    savedUser.isPhoneVerified(),
                    savedUser.getCreatedAt()
            );

        } catch (DuplicateKeyException ex) {

            String errorMessage = ex.getMessage() == null
                    ? ""
                    : ex.getMessage().toLowerCase(Locale.ROOT);

            if (errorMessage.contains("uk_users_email")
                    || errorMessage.contains("email_1")) {

                throw new EmailAlreadyRegisteredException(
                        "Email already registered."
                );
            }

            if (errorMessage.contains("uk_users_phone")
                    || errorMessage.contains("phone_1")) {

                throw new PhoneAlreadyRegisteredException(
                        "Phone number already registered."
                );
            }

            throw ex;
        }
    }
}