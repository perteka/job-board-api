package com.jobboard.api.security;

import com.jobboard.api.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ISSUER = "job-board-api";

    private final JwtProperties properties;

    public TokenPair generateTokenPair(User user) {
        Instant now = Instant.now();
        String accessToken = Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(properties.accessTokenExpiration())))
                .signWith(accessKey())
                .compact();

        String refreshToken = Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(properties.refreshTokenExpiration())))
                .signWith(refreshKey())
                .compact();

        return new TokenPair(accessToken, refreshToken, properties.accessTokenExpiration() / 1000);
    }

    public String extractUsername(String token) {
        return parseClaims(token, accessKey()).getSubject();
    }

    public String extractRefreshUsername(String token) {
        return parseClaims(token, refreshKey()).getSubject();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseClaims(token, accessKey()).get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token, refreshKey()).get("type", String.class));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token, accessKey());
            return userDetails.isEnabled()
                    && "access".equals(claims.get("type", String.class))
                    && claims.getSubject().equals(userDetails.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims parseClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey accessKey() {
        return Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey refreshKey() {
        return Keys.hmacShaKeyFor(properties.refreshSecret().getBytes(StandardCharsets.UTF_8));
    }
}
