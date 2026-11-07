package com.jobboard.api.service;

import com.jobboard.api.dto.request.LoginRequest;
import com.jobboard.api.dto.request.RefreshTokenRequest;
import com.jobboard.api.dto.request.RegisterRequest;
import com.jobboard.api.dto.response.UserResponse;
import com.jobboard.api.entity.RefreshToken;
import com.jobboard.api.entity.User;
import com.jobboard.api.entity.enums.Role;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.UnauthorizedException;
import com.jobboard.api.mapper.UserMapper;
import com.jobboard.api.repository.JobSeekerProfileRepository;
import com.jobboard.api.repository.UserRepository;
import com.jobboard.api.security.JwtService;
import com.jobboard.api.security.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final JobSeekerProfileRepository profiles = mock(JobSeekerProfileRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwt = mock(JwtService.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final UserMapper mapper = mock(UserMapper.class);
    private final EmailVerificationService emailVerification = mock(EmailVerificationService.class);
    private final PasswordResetService passwordReset = mock(PasswordResetService.class);
    private final PasswordPolicy passwordPolicy = mock(PasswordPolicy.class);
    private final AuthService service = new AuthService(
            users, profiles, encoder, authenticationManager, jwt, refreshTokens, mapper,
            emailVerification, passwordReset, passwordPolicy);

    @BeforeEach
    void injectConfiguration() {
        ReflectionTestUtils.setField(service, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(service, "lockoutMinutes", 15);
        ReflectionTestUtils.setField(service, "emailVerificationEnabled", false);
    }

    @Test
    void registerCandidateNormalizesEmailAndCreatesProfile() {
        when(encoder.encode("password123")).thenReturn("hashed");
        when(users.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0); user.setId(1L); return user;
        });

        var response = service.register(new RegisterRequest(" CANDIDATE@EXAMPLE.COM ",
                "password123", " Aday ", Role.CANDIDATE));

        assertThat(response.message()).isNotBlank();
        verify(users).save(argThat(user ->
                user.getEmail().equals("candidate@example.com") && user.isEnabled() && user.isEmailVerified()));
        verify(profiles).save(argThat(profile -> profile.getUser().getRole() == Role.CANDIDATE));
        verify(emailVerification, never()).sendVerificationEmail(any());
    }

    @Test
    void registerRejectsAdminRole() {
        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "admin@example.com", "password123", "Admin", Role.ADMIN)))
                .isInstanceOf(BadRequestException.class);
        verify(users, never()).save(any());
    }

    @Test
    void registerReturnsGenericResponseForDuplicateEmail() {
        User existing = user(true);
        when(users.findByEmail("candidate@example.com")).thenReturn(Optional.of(existing));

        var response = service.register(new RegisterRequest(
                "Candidate@Example.com", "password123", "Aday", Role.CANDIDATE));

        assertThat(response.message()).isNotBlank();
        verify(users, never()).save(any());
        verify(profiles, never()).save(any());
    }

    @Test
    void loginNormalizesEmailAndCreatesTokenPair() {
        User user = user(true);
        when(users.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
        when(jwt.generateTokenPair(user)).thenReturn(new TokenPair("access", "refresh", 900));
        when(mapper.toResponse(user)).thenReturn(new UserResponse(1L, user.getEmail(), "Aday", Role.CANDIDATE));

        var response = service.login(new LoginRequest(" CANDIDATE@EXAMPLE.COM ", "password123"));

        assertThat(response.accessToken()).isEqualTo("access");
        verify(authenticationManager).authenticate(argThat(token ->
                token.getPrincipal().equals("candidate@example.com") && token.getCredentials().equals("password123")));
    }

    @Test
    void loginMapsAuthenticationFailureToUnauthorized() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));
        assertThatThrownBy(() -> service.login(new LoginRequest("a@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginIncrementsFailedAttemptsAndLocksAfterThreshold() {
        User user = user(true);
        when(users.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login(new LoginRequest("candidate@example.com", "wrong")))
                    .isInstanceOf(UnauthorizedException.class);
        }

        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(users, atLeast(5)).save(user);
    }

    @Test
    void loginRejectsLockedAccountBeforeAuthentication() {
        User user = user(true);
        user.setLockedUntil(Instant.now().plusSeconds(600));
        when(users.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("candidate@example.com", "Sup3rSecret99")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("kilitlendi");
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void successfulLoginResetsFailedAttempts() {
        User user = user(true);
        user.setFailedLoginAttempts(3);
        when(users.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
        when(jwt.generateTokenPair(user)).thenReturn(new TokenPair("access", "refresh", 900));
        when(mapper.toResponse(user)).thenReturn(new UserResponse(1L, user.getEmail(), "Aday", Role.CANDIDATE));

        service.login(new LoginRequest("candidate@example.com", "Sup3rSecret99"));

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void refreshRejectsDisabledUser() {
        User disabled = user(false);
        RefreshToken stored = new RefreshToken();
        stored.setUser(disabled);
        when(refreshTokens.findValid("refresh-token")).thenReturn(stored);
        when(jwt.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwt.extractUsername("refresh-token")).thenReturn(disabled.getEmail());
        when(users.findByEmail(disabled.getEmail())).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("refresh-token")))
                .isInstanceOf(UnauthorizedException.class);
        verify(refreshTokens, never()).revoke(any());
    }

    private User user(boolean enabled) {
        User user = new User(); user.setId(1L); user.setEmail("candidate@example.com");
        user.setFullName("Aday"); user.setRole(Role.CANDIDATE); user.setEnabled(enabled);
        return user;
    }
}
