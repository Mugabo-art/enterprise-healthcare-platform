package com.healthplatform.auth;

import com.healthplatform.auth.model.RefreshToken;
import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.auth.repository.RefreshTokenRepository;
import com.healthplatform.auth.service.RefreshTokenService;
import com.healthplatform.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repository;

    private RefreshTokenService service() {
        return new RefreshTokenService(repository, 7);
    }

    @Test
    void issue_storesOnlyAHashNeverTheRawToken() {
        User user = User.builder().id(UUID.randomUUID()).email("d@x.test").role(Role.DOCTOR).build();

        String raw = service().issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertNotEquals(raw, saved.getValue().getTokenHash());
        assertFalse(saved.getValue().getTokenHash().contains(raw));
    }

    @Test
    void validateAndRotate_revokesUsedTokenAndReturnsUser() {
        UUID userId = UUID.randomUUID();
        RefreshToken stored = RefreshToken.builder().userId(userId).tokenHash("h")
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        assertEquals(userId, service().validateAndRotate("raw"));
        assertTrue(stored.isRevoked());
    }

    @Test
    void validateAndRotate_reuseOfRevokedTokenRevokesEverySession() {
        UUID userId = UUID.randomUUID();
        RefreshToken stored = RefreshToken.builder().userId(userId).tokenHash("h").revoked(true)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        ApiException ex = assertThrows(ApiException.class, () -> service().validateAndRotate("raw"));

        assertEquals(401, ex.getStatus().value());
        verify(repository).deleteByUserId(userId);
    }

    @Test
    void validateAndRotate_rejectsExpiredToken() {
        RefreshToken stored = RefreshToken.builder().userId(UUID.randomUUID()).tokenHash("h")
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)).build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        assertEquals(401, assertThrows(ApiException.class, () -> service().validateAndRotate("raw")).getStatus().value());
        verify(repository, never()).deleteByUserId(any());
    }

    @Test
    void validateAndRotate_rejectsUnknownToken() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertEquals(401, assertThrows(ApiException.class, () -> service().validateAndRotate("nope")).getStatus().value());
    }

    @Test
    void purgeExpired_deletesRowsPastTheirExpiry() {
        when(repository.deleteExpiredBefore(any())).thenReturn(3);

        service().purgeExpired();

        verify(repository).deleteExpiredBefore(any(Instant.class));
    }
}
