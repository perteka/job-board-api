package com.jobboard.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotBlank @Size(min = 32) String refreshSecret,
        @Positive
        long accessTokenExpiration,
        @Positive
        long refreshTokenExpiration
) {
}
