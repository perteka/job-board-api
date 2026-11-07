package com.jobboard.api.security;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
