package com.healthplatform.auth.service;

import com.healthplatform.auth.dto.*;
import com.healthplatform.auth.model.RefreshToken;
import com.healthplatform.auth.model.User;
import com.healthplatform.auth.repository.UserRepository;
import com.healthplatform.auth.security.JwtService;
import com.healthplatform.common.exception.ApiException;
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

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            LoginRateLimiter rateLimiter,
            MfaService mfaService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimiter = rateLimiter;
        this.mfaService = mfaService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Returns either AuthTokensResponse (no MFA / MFA already verified) or
     * MfaChallengeResponse (MFA required) — caller distinguishes on type.
     */
    @Transactional
    public Object login(LoginRequest request) {
        if (rateLimiter.isLocked(request.email())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again later.");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(request.email());
            throw new BadCredentialsException("Invalid credentials");
        }

        rateLimiter.reset(request.email());

        if (user.isMfaEnabled()) {
            // In a full implementation, the challengeId would be persisted with a short TTL
            // and tied to this user; kept minimal here since the AI/notification modules
            // aren't in v1 scope for this scaffold.
            return new MfaChallengeResponse(true, user.getId());
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthTokensResponse verifyMfaAndIssueTokens(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MFA is not enabled for this user");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthTokensResponse refresh(String rawRefreshToken) {
        UUID userId = refreshTokenService.validateAndRotate(rawRefreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthTokensResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthTokensResponse(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds());
    }
}
