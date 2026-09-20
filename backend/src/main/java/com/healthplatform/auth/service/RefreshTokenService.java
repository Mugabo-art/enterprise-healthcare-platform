package com.healthplatform.auth.service;

import com.healthplatform.auth.model.RefreshToken;
import com.healthplatform.auth.model.User;
import com.healthplatform.auth.repository.RefreshTokenRepository;
import com.healthplatform.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Refresh tokens are stored hashed (never raw) and rotated on every use — see
 * docs/THREAT_MODEL.md #2. A stolen DB row alone cannot be replayed as a token.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenTtlDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public String issue(User user) {
        String rawToken = generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    public UUID validateAndRotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return stored.getUserId();
    }

    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // A deterministic one-way hash is required here (unlike password storage) because we
    // need to look the token up by its hash. The raw token already carries 512 bits of
    // entropy from SecureRandom, so SHA-256 without a per-row salt is appropriate — the
    // threat being defended against is DB read access, not brute-force guessing.
    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
