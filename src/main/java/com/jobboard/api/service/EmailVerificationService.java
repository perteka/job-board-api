package com.jobboard.api.service;

import com.jobboard.api.entity.EmailVerificationToken;
import com.jobboard.api.entity.User;
import com.jobboard.api.exception.UnauthorizedException;
import com.jobboard.api.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final MailService mailService;

    @Value("${app.security.email-verification.token-ttl-minutes:1440}")
    private long tokenTtlMinutes;

    @Value("${app.web.base-url:http://localhost:3000}")
    private String webBaseUrl;

    @Transactional
    public void sendVerificationEmail(User user) {
        String rawToken = generateToken();

        tokenRepository.deleteByUserId(user.getId());
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(tokenTtlMinutes * 60));
        tokenRepository.save(token);

        String link = webBaseUrl + "/verify-email?token=" + rawToken;
        mailService.sendVerificationEmail(user.getEmail(), user.getFullName(), link);
    }

    @Transactional
    public User consume(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Geçersiz veya süresi dolmuş doğrulama tokenı"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            throw new UnauthorizedException("Doğrulama tokenının süresi dolmuş");
        }

        User user = token.getUser();
        tokenRepository.deleteByUserId(user.getId());
        return user;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
