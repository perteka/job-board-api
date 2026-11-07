package com.jobboard.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobboard.api.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String REFRESH_PATH = "/api/auth/refresh";
    private static final String RESEND_VERIFICATION_PATH = "/api/auth/resend-verification";
    private static final String FORGOT_PASSWORD_PATH = "/api/auth/forgot-password";
    private static final String RESET_PASSWORD_PATH = "/api/auth/reset-password";

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${app.security.rate-limit.login-limit:20}")
    private int loginLimit;

    @Value("${app.security.rate-limit.login-window-seconds:300}")
    private long loginWindowSeconds;

    @Value("${app.security.rate-limit.register-limit:10}")
    private int registerLimit;

    @Value("${app.security.rate-limit.register-window-seconds:3600}")
    private long registerWindowSeconds;

    @Value("${app.security.rate-limit.refresh-limit:60}")
    private int refreshLimit;

    @Value("${app.security.rate-limit.refresh-window-seconds:300}")
    private long refreshWindowSeconds;

    @Value("${app.security.rate-limit.resend-verification-limit:5}")
    private int resendVerificationLimit;

    @Value("${app.security.rate-limit.resend-verification-window-seconds:3600}")
    private long resendVerificationWindowSeconds;

    @Value("${app.security.rate-limit.forgot-password-limit:5}")
    private int forgotPasswordLimit;

    @Value("${app.security.rate-limit.forgot-password-window-seconds:3600}")
    private long forgotPasswordWindowSeconds;

    @Value("${app.security.rate-limit.reset-password-limit:10}")
    private int resetPasswordLimit;

    @Value("${app.security.rate-limit.reset-password-window-seconds:3600}")
    private long resetPasswordWindowSeconds;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!HttpMethod.POST.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key;
        int limit;
        long windowSeconds;

        if (LOGIN_PATH.equals(path)) {
            key = "rl:login:" + clientIp(request);
            limit = loginLimit;
            windowSeconds = loginWindowSeconds;
        } else if (REGISTER_PATH.equals(path)) {
            key = "rl:register:" + clientIp(request);
            limit = registerLimit;
            windowSeconds = registerWindowSeconds;
        } else if (REFRESH_PATH.equals(path)) {
            key = "rl:refresh:" + clientIp(request);
            limit = refreshLimit;
            windowSeconds = refreshWindowSeconds;
        } else if (RESEND_VERIFICATION_PATH.equals(path)) {
            key = "rl:resend-verification:" + clientIp(request);
            limit = resendVerificationLimit;
            windowSeconds = resendVerificationWindowSeconds;
        } else if (FORGOT_PASSWORD_PATH.equals(path)) {
            key = "rl:forgot-password:" + clientIp(request);
            limit = forgotPasswordLimit;
            windowSeconds = forgotPasswordWindowSeconds;
        } else if (RESET_PASSWORD_PATH.equals(path)) {
            key = "rl:reset-password:" + clientIp(request);
            limit = resetPasswordLimit;
            windowSeconds = resetPasswordWindowSeconds;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        if (rateLimiter.tryConsume(key, limit, Duration.ofSeconds(windowSeconds))) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit aşıldı: {} {} ip={}", request.getMethod(), path, clientIp(request));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Çok fazla istek. Lütfen daha sonra tekrar deneyin",
                path,
                Map.of()
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
