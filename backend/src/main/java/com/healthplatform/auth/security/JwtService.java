package com.healthplatform.auth.security;

import com.healthplatform.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates signed, stateless JWTs. Access tokens are short-lived (see
 * application.yml `app.jwt.access-token-ttl-ms`); refresh tokens are handled separately
 * via RefreshTokenService since they need to be revocable server-side.
 *
 * Every token carries a `typ` claim ("access" or "mfa") so a token minted for one purpose can
 * never be replayed as another — in particular an MFA challenge is not a usable access token.
 */
@Service
public class JwtService {

    private static final String TYP = "typ";
    private static final String TYP_ACCESS = "access";
    private static final String TYP_MFA = "mfa";
    private static final long MFA_CHALLENGE_TTL_MS = 5 * 60 * 1000L;

    private final SecretKey signingKey;
    private final long accessTokenTtlMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-ms}") long accessTokenTtlMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMs = accessTokenTtlMs;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenTtlMs);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim(TYP, TYP_ACCESS)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /** Short-lived proof that the password step succeeded; exchanged with a TOTP code for real tokens. */
    public String generateMfaChallengeToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(TYP, TYP_MFA)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + MFA_CHALLENGE_TTL_MS))
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlMs / 1000;
    }

    /** Parses an ACCESS token; MFA challenge tokens are rejected. */
    public Claims validateAndParse(String token) {
        return parseTyped(token, TYP_ACCESS);
    }

    /** Returns the user id from a valid, unexpired MFA challenge token. */
    public UUID parseMfaChallenge(String token) {
        return UUID.fromString(parseTyped(token, TYP_MFA).getSubject());
    }

    private Claims parseTyped(String token, String expectedTyp) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!expectedTyp.equals(claims.get(TYP, String.class))) {
            throw new JwtException("Unexpected token type");
        }
        return claims;
    }
}
