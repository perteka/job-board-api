package com.jobboard.api.service;

import com.jobboard.api.entity.RefreshToken;
import com.jobboard.api.entity.User;
import com.jobboard.api.exception.UnauthorizedException;
import com.jobboard.api.repository.RefreshTokenRepository;
import com.jobboard.api.security.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(
            repository, new JwtProperties(
                    "test-jwt-secret-with-at-least-32-characters",
                    "test-jwt-refresh-secret-with-32-characters",
                    60_000, 86_400_000));

    @Test
    void saveStoresSha256HashRatherThanRawToken() {
        User user = new User(); user.setId(6L);
        service.save(user, "raw-refresh-token");

        verify(repository).save(argThat(token -> token.getTokenHash() != null
                && token.getTokenHash().length() == 64
                && !token.getTokenHash().equals("raw-refresh-token")
                && token.getUser() == user));
    }

    @Test
    void findValidLooksUpByHashAndReturnsUnexpiredToken() {
        RefreshToken token = token(false, Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThat(service.findValid("raw-refresh-token")).isSameAs(token);
        verify(repository).findByTokenHash(argThat(hash -> hash.length() == 64));
    }

    @Test
    void findValidRejectsUnknownToken() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findValid("unknown"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void findValidRejectsExpiredToken() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token(false, Instant.now().minusSeconds(1))));
        assertThatThrownBy(() -> service.findValid("expired"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void findValidRevokedTokenDeletesAllUserTokensForReuseResponse() {
        RefreshToken revoked = token(true, Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.findValid("reused"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("tekrar kullanıldı");
        verify(repository).deleteAllByUserId(6L);
    }

    private RefreshToken token(boolean revoked, Instant expiresAt) {
        User user = new User(); user.setId(6L);
        RefreshToken token = new RefreshToken(); token.setUser(user);
        token.setRevoked(revoked); token.setExpiresAt(expiresAt);
        return token;
    }
}
