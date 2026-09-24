package com.healthplatform.auth;

import com.healthplatform.audit.service.AuditService;
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
    @Mock private AuditService auditService;

    private static final User ADMIN_CALLER = User.builder().id(UUID.randomUUID()).email("admin@hospital.test").role(Role.ADMIN).build();

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, rateLimiter, mfaService, auditService);
    }

    @Test
    void register_savesNewUserWithHashedPassword() {
        RegisterRequest request = new RegisterRequest("doctor@hospital.test", "Password123!", Role.DOCTOR);
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = authService.register(request, ADMIN_CALLER);

        assertEquals(request.email(), response.email());
        assertEquals(Role.DOCTOR, response.role());
        verify(userRepository).save(argThat(u -> passwordEncoder.matches("Password123!", u.getPasswordHash())));
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("doctor@hospital.test", "Password123!", Role.DOCTOR);
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request, ADMIN_CALLER));
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
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
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
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

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

    @Test
    void register_rejectsPrivilegedRoleFromAnonymousCaller() {
        RegisterRequest request = new RegisterRequest("evil@x.test", "Password123!", Role.ADMIN);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request, null));

        assertEquals(403, ex.getStatus().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rejectsPrivilegedRoleFromNonAdminStaff() {
        User doctor = User.builder().id(UUID.randomUUID()).email("d@x.test").role(Role.DOCTOR).build();
        RegisterRequest request = new RegisterRequest("evil@x.test", "Password123!", Role.ADMIN);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request, doctor));

        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void register_allowsAnonymousPatientSelfRegistration() {
        RegisterRequest request = new RegisterRequest("Pat@X.test", "Password123!", Role.PATIENT);
        when(userRepository.existsByEmailIgnoreCase("pat@x.test")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = authService.register(request, null);

        assertEquals("pat@x.test", response.email()); // normalised to lower case
        assertEquals(Role.PATIENT, response.role());
    }

    @Test
    void login_rejectsUnknownEmailWithSameErrorAsWrongPassword() {
        when(rateLimiter.isLocked("nobody@x.test")).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("nobody@x.test")).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () ->
                authService.login(new com.healthplatform.auth.dto.LoginRequest("nobody@x.test", "Password123!")));
        verify(rateLimiter).recordFailure("nobody@x.test");
    }

    @Test
    void login_returnsSignedChallengeNotUserIdWhenMfaEnabled() {
        User user = User.builder().id(UUID.randomUUID()).email("m@x.test")
                .passwordHash(passwordEncoder.encode("Password123!")).role(Role.DOCTOR).mfaEnabled(true).build();
        when(userRepository.findByEmailIgnoreCase("m@x.test")).thenReturn(Optional.of(user));
        when(jwtService.generateMfaChallengeToken(user)).thenReturn("signed-challenge");

        Object result = authService.login(new com.healthplatform.auth.dto.LoginRequest("m@x.test", "Password123!"));

        assertEquals("signed-challenge", ((com.healthplatform.auth.dto.MfaChallengeResponse) result).challengeId());
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    void verifyMfa_rejectsForgedChallenge() {
        when(jwtService.parseMfaChallenge("forged")).thenThrow(new io.jsonwebtoken.JwtException("bad"));

        ApiException ex = assertThrows(ApiException.class, () -> authService.verifyMfaAndIssueTokens("forged", "123456"));

        assertEquals(401, ex.getStatus().value());
        verifyNoInteractions(userRepository, mfaService);
    }

    @Test
    void verifyMfa_locksOutAfterRepeatedFailures() {
        UUID id = UUID.randomUUID();
        when(jwtService.parseMfaChallenge("tok")).thenReturn(id);
        when(rateLimiter.isLocked("mfa:" + id)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.verifyMfaAndIssueTokens("tok", "000000"));

        assertEquals(429, ex.getStatus().value());
        verifyNoInteractions(mfaService);
    }

    @Test
    void login_deniesDisabledAccount() {
        User user = User.builder().id(UUID.randomUUID()).email("l@x.test")
                .passwordHash(passwordEncoder.encode("Password123!")).role(Role.DOCTOR).accountLocked(true).build();
        when(userRepository.findByEmailIgnoreCase("l@x.test")).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login(new com.healthplatform.auth.dto.LoginRequest("l@x.test", "Password123!")));

        assertEquals(403, ex.getStatus().value());
        verify(refreshTokenService, never()).issue(any());
    }
}
