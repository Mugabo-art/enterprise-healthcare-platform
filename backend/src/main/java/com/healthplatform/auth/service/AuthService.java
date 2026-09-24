package com.healthplatform.auth.service;

import com.healthplatform.audit.service.AuditService;
import com.healthplatform.auth.dto.*;
import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.auth.repository.UserRepository;
import com.healthplatform.auth.security.JwtService;
import com.healthplatform.common.exception.ApiException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter rateLimiter;
    private final MfaService mfaService;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            LoginRateLimiter rateLimiter,
            MfaService mfaService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimiter = rateLimiter;
        this.mfaService = mfaService;
        this.auditService = auditService;
    }

    /**
     * Anyone may self-register a PATIENT account (a staff member must still link it to a patient
     * record). Every other role is privileged and can only be granted by an authenticated ADMIN —
     * otherwise the public endpoint would let anybody mint themselves an ADMIN.
     */
    @Transactional
    public UserResponse register(RegisterRequest request, User caller) {
        if (request.role() != Role.PATIENT && (caller == null || caller.getRole() != Role.ADMIN)) {
            auditService.recordEvent("REGISTER", caller, request.email(), AuditService.OUTCOME_DENIED,
                    "attempted to create role " + request.role(), null);
            throw new ApiException(HttpStatus.FORBIDDEN, "Only an administrator can create staff accounts");
        }
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        User saved = userRepository.save(user);
        auditService.recordEvent("REGISTER", caller, email, AuditService.OUTCOME_SUCCESS,
                "created " + request.role() + " account " + saved.getId(), null);
        return UserResponse.from(saved);
    }

    /**
     * Returns either AuthTokensResponse (no MFA) or MfaChallengeResponse (MFA required) —
     * caller distinguishes on type. noRollbackFor keeps nothing half-applied on expected failures.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Object login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        if (rateLimiter.isLocked(email)) {
            auditService.recordEvent("LOGIN", null, email, AuditService.OUTCOME_DENIED, "account temporarily locked", null);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again later.");
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        // Compare against a real hash even for unknown emails so response time does not reveal
        // which accounts exist.
        boolean passwordOk;
        if (user != null) {
            passwordOk = passwordEncoder.matches(request.password(), user.getPasswordHash());
        } else {
            passwordEncoder.matches(request.password(), DUMMY_HASH); // result deliberately ignored
            passwordOk = false;
        }

        if (!passwordOk) {
            rateLimiter.recordFailure(email);
            auditService.recordEvent("LOGIN", user, email, AuditService.OUTCOME_FAILURE, "invalid credentials", null);
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!user.isAccountNonLocked()) {
            auditService.recordEvent("LOGIN", user, email, AuditService.OUTCOME_DENIED, "account disabled", null);
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been disabled");
        }

        rateLimiter.reset(email);

        if (user.isMfaEnabled()) {
            auditService.recordEvent("LOGIN_MFA_CHALLENGE", user, email, AuditService.OUTCOME_SUCCESS, null, null);
            return new MfaChallengeResponse(true, jwtService.generateMfaChallengeToken(user));
        }

        auditService.recordEvent("LOGIN", user, email, AuditService.OUTCOME_SUCCESS, null, null);
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthTokensResponse verifyMfaAndIssueTokens(String challengeToken, String code) {
        UUID userId;
        try {
            userId = jwtService.parseMfaChallenge(challengeToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA challenge expired or invalid. Log in again.");
        }
        String limiterKey = "mfa:" + userId;
        if (rateLimiter.isLocked(limiterKey)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again later.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "MFA challenge expired or invalid. Log in again."));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MFA is not enabled for this user");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            rateLimiter.recordFailure(limiterKey);
            auditService.recordEvent("LOGIN_MFA", user, user.getEmail(), AuditService.OUTCOME_FAILURE, "invalid MFA code", null);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }
        rateLimiter.reset(limiterKey);
        auditService.recordEvent("LOGIN", user, user.getEmail(), AuditService.OUTCOME_SUCCESS, "mfa verified", null);
        return issueTokens(user);
    }

    // noRollbackFor: refresh-token reuse detection revokes all sessions and THEN throws 401 —
    // that revocation must be committed, not rolled back with the exception.
    @Transactional(noRollbackFor = ApiException.class)
    public AuthTokensResponse refresh(String rawRefreshToken) {
        UUID userId = refreshTokenService.validateAndRotate(rawRefreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (!user.isAccountNonLocked()) {
            refreshTokenService.revokeAllForUser(userId);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(User user) {
        refreshTokenService.revokeAllForUser(user.getId());
        auditService.recordEvent("LOGOUT", user, user.getEmail(), AuditService.OUTCOME_SUCCESS, null, null);
    }

    /**
     * Links a PATIENT-role account to an existing Patient record. Called from
     * PatientController (via PatientService) rather than exposed directly, so
     * the "is this patient real" check stays in the patient module and this
     * stays a same-request service-to-service call, not a repository reach.
     */
    @Transactional
    public void linkPatient(UUID userId, UUID patientId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.PATIENT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PATIENT-role accounts can be linked to a patient record");
        }
        user.setPatientId(patientId);
        userRepository.save(user);
    }

    private AuthTokensResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthTokensResponse(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    // A valid bcrypt hash, only ever compared against (result discarded) to equalise timing for unknown emails.
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5b2z0JGSeGAJZbgpNqOZ1hrQpG9Wu";
}
