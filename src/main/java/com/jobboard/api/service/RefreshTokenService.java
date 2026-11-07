package com.jobboard.api.service;

import com.jobboard.api.entity.RefreshToken;
import com.jobboard.api.entity.User;
import com.jobboard.api.exception.UnauthorizedException;
import com.jobboard.api.repository.RefreshTokenRepository;
import com.jobboard.api.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public void save(User user, String token) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(token));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtProperties.refreshTokenExpiration()));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken findValid(String token) {
        String tokenHash = hash(token);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Geçersiz yenileme tokenı"));

        if (refreshToken.isRevoked()) {
            revokeAllForUser(refreshToken.getUser().getId());
            throw new UnauthorizedException("Yenileme tokenı tekrar kullanıldı; tüm oturumlar kapatıldı");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Süresi dolmuş yenileme tokenı");
        }

        return refreshToken;
    }

    @Transactional
    public void revoke(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
