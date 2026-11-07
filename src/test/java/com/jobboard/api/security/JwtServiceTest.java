package com.jobboard.api.security;

import com.jobboard.api.entity.User;
import com.jobboard.api.entity.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService(new JwtProperties(
            "test-secret-for-jwt-signing-must-be-at-least-32-bytes",
            "test-refresh-secret-must-be-at-least-32-bytes-too",
            60_000, 604_800_000));

    @Test
    void tokenPairContainsCorrectTokenTypesAndIdentity() {
        User user = user(true);
        TokenPair pair = jwtService.generateTokenPair(user);

        assertThat(jwtService.isAccessToken(pair.accessToken())).isTrue();
        assertThat(jwtService.isRefreshToken(pair.refreshToken())).isTrue();
        assertThat(jwtService.extractUsername(pair.accessToken())).isEqualTo(user.getEmail());
        assertThat(jwtService.isTokenValid(pair.accessToken(), new UserPrincipal(user))).isTrue();
        assertThat(pair.expiresIn()).isEqualTo(60);
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        User user = user(true);
        TokenPair pair = jwtService.generateTokenPair(user);
        assertThat(jwtService.isTokenValid(pair.refreshToken(), new UserPrincipal(user))).isFalse();
    }

    @Test
    void disabledUserCannotUseAccessToken() {
        User enabled = user(true);
        TokenPair pair = jwtService.generateTokenPair(enabled);
        User disabled = user(false);
        assertThat(jwtService.isTokenValid(pair.accessToken(), new UserPrincipal(disabled))).isFalse();
    }

    @Test
    void malformedOrWrongSecretTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not-a-token", new UserPrincipal(user(true)))).isFalse();
        JwtService otherService = new JwtService(new JwtProperties(
                "another-test-secret-at-least-32-characters-long",
                "another-test-refresh-secret-32-characters",
                60_000, 604_800_000));
        TokenPair pair = otherService.generateTokenPair(user(true));
        assertThat(jwtService.isTokenValid(pair.accessToken(), new UserPrincipal(user(true)))).isFalse();
    }

    private User user(boolean enabled) {
        User user = new User();
        user.setId(12L);
        user.setEmail("candidate@example.com");
        user.setPassword("encoded");
        user.setRole(Role.CANDIDATE);
        user.setEnabled(enabled);
        return user;
    }
}
