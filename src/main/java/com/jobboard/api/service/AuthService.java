package com.jobboard.api.service;

import com.jobboard.api.dto.request.ChangePasswordRequest;
import com.jobboard.api.dto.request.LoginRequest;
import com.jobboard.api.dto.request.RefreshTokenRequest;
import com.jobboard.api.dto.request.RegisterRequest;
import com.jobboard.api.dto.request.ResetPasswordRequest;
import com.jobboard.api.dto.response.AuthResponse;
import com.jobboard.api.dto.response.RegisterResponse;
import com.jobboard.api.entity.JobSeekerProfile;
import com.jobboard.api.entity.RefreshToken;
import com.jobboard.api.entity.User;
import com.jobboard.api.entity.enums.Role;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.exception.UnauthorizedException;
import com.jobboard.api.mapper.UserMapper;
import com.jobboard.api.repository.JobSeekerProfileRepository;
import com.jobboard.api.repository.UserRepository;
import com.jobboard.api.security.JwtService;
import com.jobboard.api.security.TokenPair;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${app.security.login-max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.login-lockout-minutes:15}")
    private long lockoutMinutes;

    private final UserRepository userRepository;
    private final JobSeekerProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final PasswordPolicy passwordPolicy;

    @Value("${app.security.email-verification.enabled:true}")
    private boolean emailVerificationEnabled;

    private static final String REGISTER_MESSAGE =
            "Kayıt işlemi alındı. E-posta adresiniz kayıtlıysa doğrulama bağlantısı gönderildi";

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        User existing = userRepository.findByEmail(email).orElse(null);

        if (existing != null) {
            if (emailVerificationEnabled && !existing.isEmailVerified()) {
                emailVerificationService.sendVerificationEmail(existing);
            }
            log.info("Kayıt isteği mevcut e-posta için genel yanıtla karşılandı: {}", email);
            return new RegisterResponse(REGISTER_MESSAGE);
        }
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("ADMIN rolüyle kayıt yapılamaz");
        }
        passwordPolicy.validate(request.password());

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(!emailVerificationEnabled);
        user.setEmailVerified(!emailVerificationEnabled);
        userRepository.save(user);

        if (user.getRole() == Role.CANDIDATE) {
            JobSeekerProfile profile = new JobSeekerProfile();
            profile.setUser(user);
            profileRepository.save(profile);
        }
        if (emailVerificationEnabled) {
            emailVerificationService.sendVerificationEmail(user);
        }
        log.info("Yeni kullanıcı kaydı: {} ({})", email, user.getRole());

        return new RegisterResponse(REGISTER_MESSAGE);
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        User user = emailVerificationService.consume(token);
        log.info("E-posta doğrulandı: {}", user.getEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional
    public void resendVerification(String email) {
        if (!emailVerificationEnabled) {
            return;
        }
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                emailVerificationService.sendVerificationEmail(user);
            }
        });
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null && user.isLoginLocked()) {
            log.warn("Kilitli hesaba giriş denemesi: {}", email);
            throw new UnauthorizedException("Çok fazla başarısız giriş denemesi. Hesap geçici olarak kilitlendi");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException exception) {
            registerFailedLogin(user);
            log.warn("Başarısız giriş denemesi: {}", email);
            throw new UnauthorizedException("E-posta veya şifre hatalı");
        }

        if (user == null) {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
        } else {
            resetFailedLogins(user);
        }

        return buildResponse(user);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenService.findValid(request.refreshToken());

        try {
            if (!jwtService.isRefreshToken(request.refreshToken())) {
                throw new UnauthorizedException("Geçersiz yenileme tokenı");
            }
            String email = jwtService.extractRefreshUsername(request.refreshToken());
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("Geçersiz yenileme tokenı"));

            if (!user.getId().equals(storedToken.getUser().getId())) {
                throw new UnauthorizedException("Geçersiz yenileme tokenı");
            }
            if (!user.isEnabled()) {
                throw new UnauthorizedException("Hesap devre dışı");
            }

            refreshTokenService.revoke(storedToken);
            return buildResponse(user);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new UnauthorizedException("Geçersiz yenileme tokenı");
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Mevcut şifre hatalı");
        }
        passwordPolicy.validate(request.newPassword());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
        log.info("Şifre değiştirildi, oturumlar kapatıldı: userId={}", userId);
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            if (user.isEnabled()) {
                passwordResetService.sendResetEmail(user);
            }
        });
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = passwordResetService.consume(request.token());
        passwordPolicy.validate(request.newPassword());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
        log.info("Şifre sıfırlandı, oturumlar kapatıldı: {}", user.getEmail());

        return buildResponse(user);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthResponse buildResponse(User user) {
        TokenPair tokenPair = jwtService.generateTokenPair(user);
        refreshTokenService.save(user, tokenPair.refreshToken());

        return new AuthResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                "Bearer",
                tokenPair.expiresIn(),
                userMapper.toResponse(user)
        );
    }

    private void registerFailedLogin(User user) {
        if (user == null) {
            return;
        }
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(Instant.now().plusSeconds(lockoutMinutes * 60));
            user.setFailedLoginAttempts(0);
            log.warn("Hesap kilitlendi ({} başarısız deneme): {}", maxFailedAttempts, user.getEmail());
        }
        userRepository.save(user);
    }

    private void resetFailedLogins(User user) {
        if (user.getFailedLoginAttempts() == 0 && user.getLockedUntil() == null) {
            return;
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
