package com.onlyrooms.admin.config;

import com.onlyrooms.user.entity.User;
import com.onlyrooms.user.entity.UserRole;
import com.onlyrooms.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Configuration
public class AdminBootstrapRunner {

    @Bean
    @ConditionalOnProperty(
            name = "onlyrooms.admin.bootstrap.enabled",
            havingValue = "true"
    )
    public ApplicationRunner createInitialAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,

            @Value("${onlyrooms.admin.bootstrap.full-name:}")
            String fullName,

            @Value("${onlyrooms.admin.bootstrap.email:}")
            String email,

            @Value("${onlyrooms.admin.bootstrap.phone:}")
            String phone,

            @Value("${onlyrooms.admin.bootstrap.password:}")
            String password
    ) {

        return args -> {

            // Do not create another ADMIN if one already exists.
            if (userRepository.existsByRole(UserRole.ADMIN)) {
                System.out.println(
                        "OnlyRooms ADMIN already exists. Bootstrap skipped."
                );
                return;
            }

            // Validate configuration.
            if (fullName.isBlank()
                    || email.isBlank()
                    || phone.isBlank()
                    || password.isBlank()) {

                throw new IllegalStateException(
                        "ADMIN bootstrap configuration is incomplete."
                );
            }

            String normalizedEmail =
                    email.trim().toLowerCase(Locale.ROOT);

            String normalizedPhone = phone.trim();

            if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException(
                        "ADMIN password must not exceed 72 UTF-8 bytes."
                );
            }

            // Never convert an existing LANDLORD or TENANT into ADMIN.
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new IllegalStateException(
                        "ADMIN bootstrap email already belongs to another account."
                );
            }

            if (userRepository.existsByPhone(normalizedPhone)) {
                throw new IllegalStateException(
                        "ADMIN bootstrap phone already belongs to another account."
                );
            }

            // Hash password using the existing BCrypt encoder.
            String encodedPassword =
                    passwordEncoder.encode(password);

            User admin = new User(
                    fullName.trim(),
                    normalizedEmail,
                    normalizedPhone,
                    encodedPassword,
                    UserRole.ADMIN
            );

            // These flags indicate that the bootstrap credentials
            // were provisioned by the application operator.
            // They do not represent real-world identity verification.
            admin.setEmailVerified(true);
            admin.setPhoneVerified(true);

            userRepository.save(admin);

            System.out.println(
                    "Initial OnlyRooms ADMIN account created successfully."
            );
        };
    }
}