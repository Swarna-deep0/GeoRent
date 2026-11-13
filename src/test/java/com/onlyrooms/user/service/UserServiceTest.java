package com.onlyrooms.user.service;

import com.onlyrooms.common.exception.AdminRegistrationNotAllowedException;
import com.onlyrooms.common.exception.EmailAlreadyRegisteredException;
import com.onlyrooms.common.exception.PhoneAlreadyRegisteredException;

import com.onlyrooms.user.dto.UserRegistrationRequest;
import com.onlyrooms.user.dto.UserResponse;
import com.onlyrooms.user.entity.User;
import com.onlyrooms.user.entity.UserRole;
import com.onlyrooms.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest request;

    @BeforeEach
    void setUp() {

        request = new UserRegistrationRequest();

        request.setFullName("Rahul Sharma");
        request.setEmail("rahul@example.com");
        request.setPhone("9876543210");
        request.setPassword("Password123");
        request.setRole(UserRole.TENANT);
    }

    // TEST 1: Successful registration

    @Test
    void shouldRegisterUserSuccessfully() {

        when(userRepository.existsByEmail("rahul@example.com"))
                .thenReturn(false);

        when(userRepository.existsByPhone("9876543210"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password123"))
                .thenReturn("$2a$12$hashedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    user.setId("user123");

                    return user;
                });

        UserResponse response =
                userService.registerUser(request);

        assertNotNull(response);

        assertEquals("user123", response.getId());

        assertEquals("Rahul Sharma", response.getFullName());

        assertEquals("rahul@example.com", response.getEmail());

        assertEquals(UserRole.TENANT, response.getRole());

        assertFalse(response.isEmailVerified());

        assertFalse(response.isPhoneVerified());

        verify(passwordEncoder).encode("Password123");

        verify(userRepository).save(any(User.class));
    }

    // TEST 2: Duplicate email

    @Test
    void shouldRejectDuplicateEmail() {

        when(userRepository.existsByEmail("rahul@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> userService.registerUser(request)
        );

        verify(userRepository, never()).save(any(User.class));

        verify(passwordEncoder, never()).encode(anyString());
    }

    // TEST 3: Duplicate phone

    @Test
    void shouldRejectDuplicatePhone() {

        when(userRepository.existsByEmail("rahul@example.com"))
                .thenReturn(false);

        when(userRepository.existsByPhone("9876543210"))
                .thenReturn(true);

        assertThrows(
                PhoneAlreadyRegisteredException.class,
                () -> userService.registerUser(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    // TEST 4: ADMIN registration prohibited

    @Test
    void shouldRejectAdminRegistration() {

        request.setRole(UserRole.ADMIN);

        assertThrows(
                AdminRegistrationNotAllowedException.class,
                () -> userService.registerUser(request)
        );

        verifyNoInteractions(userRepository);

        verifyNoInteractions(passwordEncoder);
    }

    // TEST 5: Password exceeding 72 UTF-8 bytes

    @Test
    void shouldRejectPasswordExceeding72Utf8Bytes() {

        request.setPassword("a".repeat(73));

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(request)
        );

        verifyNoInteractions(userRepository);

        verifyNoInteractions(passwordEncoder);
    }

    // TEST 6: Email and phone normalization

    @Test
    void shouldNormalizeEmailAndPhoneBeforeSaving() {

        request.setFullName(" Rahul Sharma ");
        request.setEmail("  RAHUL@EXAMPLE.COM  ");
        request.setPhone(" 9876543210 ");

        when(userRepository.existsByEmail("rahul@example.com"))
                .thenReturn(false);

        when(userRepository.existsByPhone("9876543210"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password123"))
                .thenReturn("$2a$12$hashedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    user.setId("user123");

                    return user;
                });

        userService.registerUser(request);

        verify(userRepository).save(argThat(user ->

                user.getFullName().equals("Rahul Sharma")
                        && user.getEmail().equals("rahul@example.com")
                        && user.getPhone().equals("9876543210")
        ));
    }
}