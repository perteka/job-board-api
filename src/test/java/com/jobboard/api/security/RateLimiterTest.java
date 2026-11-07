package com.jobboard.api.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final RateLimiter rateLimiter = new RateLimiter();

    @Test
    void allowsUpToLimitThenRejects() {
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryConsume("key", 3, Duration.ofMinutes(1))).isTrue();
        }
        assertThat(rateLimiter.tryConsume("key", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void tracksKeysIndependently() {
        assertThat(rateLimiter.tryConsume("a", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiter.tryConsume("a", 1, Duration.ofMinutes(1))).isFalse();
        assertThat(rateLimiter.tryConsume("b", 1, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void resetsAfterWindowExpires() throws InterruptedException {
        assertThat(rateLimiter.tryConsume("expiring", 1, Duration.ofMillis(50))).isTrue();
        assertThat(rateLimiter.tryConsume("expiring", 1, Duration.ofMillis(50))).isFalse();
        Thread.sleep(60);
        assertThat(rateLimiter.tryConsume("expiring", 1, Duration.ofMillis(50))).isTrue();
    }
}
