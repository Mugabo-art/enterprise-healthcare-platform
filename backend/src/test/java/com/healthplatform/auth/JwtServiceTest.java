package com.healthplatform.auth;

import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.auth.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-bytes-long!!";

    private final JwtService jwtService = new JwtService(SECRET, 900_000);
    private final User user = User.builder().id(UUID.randomUUID()).email("d@x.test").role(Role.DOCTOR).build();

    @Test
    void accessToken_roundTripsSubjectAndRole() {
        var claims = jwtService.validateAndParse(jwtService.generateAccessToken(user));

        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals("DOCTOR", claims.get("role", String.class));
    }

    @Test
    void mfaChallengeToken_cannotBeUsedAsAccessToken() {
        String challenge = jwtService.generateMfaChallengeToken(user);

        assertThrows(JwtException.class, () -> jwtService.validateAndParse(challenge));
    }

    @Test
    void accessToken_cannotBeUsedAsMfaChallenge() {
        String access = jwtService.generateAccessToken(user);

        assertThrows(JwtException.class, () -> jwtService.parseMfaChallenge(access));
    }

    @Test
    void mfaChallengeToken_resolvesToUserId() {
        assertEquals(user.getId(), jwtService.parseMfaChallenge(jwtService.generateMfaChallengeToken(user)));
    }

    @Test
    void tokenSignedWithDifferentKey_isRejected() {
        String foreign = new JwtService("another-secret-key-at-least-32-bytes-long!!!!", 900_000).generateAccessToken(user);

        assertThrows(JwtException.class, () -> jwtService.validateAndParse(foreign));
    }

    @Test
    void expiredToken_isRejected() {
        String expired = new JwtService(SECRET, -1000).generateAccessToken(user);

        assertThrows(JwtException.class, () -> jwtService.validateAndParse(expired));
    }
}
