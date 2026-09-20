package com.healthplatform.auth;

import com.healthplatform.auth.dto.AuthTokensResponse;
import com.healthplatform.auth.dto.RegisterRequest;
import com.healthplatform.auth.dto.UserResponse;
import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.auth.repository.UserRepository;
import com.healthplatform.auth.security.JwtService;
import com.healthplatform.auth.service.AuthService;
import com.healthplatform.auth.service.LoginRateLimiter;
import com.healthplatform.auth.service.MfaService;
import com.healthplatform.auth.service.RefreshTokenService;
import com.healthplatform.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the core auth flows described in docs/SRS.md FR-1.
 * Uses a real BCryptPasswordEncoder (fast enough at test scope) and mocks everything else
 * so these run without a database — see application-test.yml for the full integration setup.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private LoginRateLimiter rateLimiter;
    @Mock private MfaService mfaService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, rateLimiter, mfaService);
    }

    @Test
    void register_savesNewUserWithHashedPassword() {
        RegisterRequest request = new RegisterRequest("doctor@hospital.test", "Password123!", Role.DOCTOR);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = authService.register(request);

        assertEquals(request.email(), response.email());
        assertEquals(Role.DOCTOR, response.role());
        verify(userRepository).save(argThat(u -> passwordEncoder.matches("Password123!", u.getPasswordHash())));
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("doctor@hospital.test", "Password123!", Role.DOCTOR);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void login_issuesTokensForValidCredentialsWithoutMfa() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor@hospital.test")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.DOCTOR)
                .mfaEnabled(false)
                .build();

        when(rateLimiter.isLocked(user.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(user)).thenReturn("refresh-token");

        Object result = authService.login(new com.healthplatform.auth.dto.LoginRequest(user.getEmail(), "Password123!"));

        assertInstanceOf(AuthTokensResponse.class, result);
        AuthTokensResponse tokens = (AuthTokensResponse) result;
        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
        verify(rateLimiter).reset(user.getEmail());
    }

    @Test
    void login_recordsFailureOnWrongPassword() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("doctor@hospital.test")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.DOCTOR)
                .build();

        when(rateLimiter.isLocked(user.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () ->
                authService.login(new com.healthplatform.auth.dto.LoginRequest(user.getEmail(), "wrong-password")));

        verify(rateLimiter).recordFailure(user.getEmail());
    }

    @Test
    void login_blockedWhenRateLimited() {
        when(rateLimiter.isLocked("doctor@hospital.test")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login(new com.healthplatform.auth.dto.LoginRequest("doctor@hospital.test", "whatever")));
        assertEquals(429, ex.getStatus().value());
        verifyNoInteractions(userRepository);
    }
}
