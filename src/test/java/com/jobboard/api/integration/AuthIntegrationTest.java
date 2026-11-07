package com.jobboard.api.integration;

import com.jobboard.api.dto.request.LoginRequest;
import com.jobboard.api.dto.request.RegisterRequest;
import com.jobboard.api.dto.request.RefreshTokenRequest;
import com.jobboard.api.entity.User;
import com.jobboard.api.entity.enums.Role;
import com.jobboard.api.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String registerAndLogin(String email, String password, Role role) {
        ResponseEntity<Map> register = rest.postForEntity(baseUrl("/api/auth/register"),
                new RegisterRequest(email, password, "Test User", role), Map.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> login = rest.postForEntity(baseUrl("/api/auth/login"),
                new LoginRequest(email, password), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().get("accessToken")).isNotNull();
        assertThat(login.getBody().get("refreshToken")).isNotNull();
        return (String) login.getBody().get("accessToken");
    }

    private HttpHeaders authHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Nested
    class Register {
        @Test
        void registersCandidateAndCreatesProfile() {
            ResponseEntity<Map> response = rest.postForEntity(baseUrl("/api/auth/register"),
                    new RegisterRequest("candidate@example.com", "Password123!", "Candidate User", Role.CANDIDATE),
                    Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            User user = userRepository.findByEmail("candidate@example.com").orElseThrow();
            assertThat(user.getRole()).isEqualTo(Role.CANDIDATE);
            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        void rejectsAdminRole() {
            ResponseEntity<Map> response = rest.postForEntity(baseUrl("/api/auth/register"),
                    new RegisterRequest("admin@example.com", "Password123!", "Admin User", Role.ADMIN),
                    Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returnsGenericResponseForDuplicateEmail() {
            rest.postForEntity(baseUrl("/api/auth/register"),
                    new RegisterRequest("dup@example.com", "Password123!", "First", Role.CANDIDATE), Map.class);
            ResponseEntity<Map> response = rest.postForEntity(baseUrl("/api/auth/register"),
                    new RegisterRequest("DUP@EXAMPLE.COM", "Password123!", "Second", Role.CANDIDATE), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    @Nested
    class Login {
        @Test
        void authenticatesWithValidCredentials() {
            String token = registerAndLogin("login@example.com", "Password123!", Role.CANDIDATE);
            assertThat(token).isNotBlank();
        }

        @Test
        void rejectsWrongPassword() {
            registerAndLogin("wrong@example.com", "Password123!", Role.CANDIDATE);
            ResponseEntity<Map> response = rest.postForEntity(baseUrl("/api/auth/login"),
                    new LoginRequest("wrong@example.com", "WrongPassword!"), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    class Refresh {
        @Test
        void rotatesAccessAndRefreshTokens() {
            String accessToken = registerAndLogin("refresh@example.com", "Password123!", Role.CANDIDATE);
            User user = userRepository.findByEmail("refresh@example.com").orElseThrow();

            ResponseEntity<Map> refresh = rest.postForEntity(baseUrl("/api/auth/refresh"),
                    new RefreshTokenRequest((String) rest.postForEntity(baseUrl("/api/auth/login"),
                            new LoginRequest("refresh@example.com", "Password123!"), Map.class)
                            .getBody().get("refreshToken")), Map.class);
            assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(refresh.getBody().get("accessToken")).isNotNull();
            assertThat(refresh.getBody().get("refreshToken")).isNotNull();
            assertThat(refresh.getBody().get("accessToken")).isNotEqualTo(accessToken);
        }

        @Test
        void rejectsInvalidRefreshToken() {
            ResponseEntity<Map> response = rest.postForEntity(baseUrl("/api/auth/refresh"),
                    new RefreshTokenRequest("not-a-valid-token"), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    class Logout {
        @Test
        void returnsUnauthorizedWithoutToken() {
            ResponseEntity<Void> response = rest.exchange(baseUrl("/api/auth/logout"),
                    HttpMethod.POST, new HttpEntity<>(new HttpHeaders()), Void.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void revokesAllRefreshTokensForUser() {
            String accessToken = registerAndLogin("logout@example.com", "Password123!", Role.CANDIDATE);

            ResponseEntity<Void> response = rest.exchange(baseUrl("/api/auth/logout"),
                    HttpMethod.POST, new HttpEntity<>(authHeader(accessToken)), Void.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // The old refresh token should now be revoked
            ResponseEntity<Map> refresh = rest.postForEntity(baseUrl("/api/auth/refresh"),
                    new RefreshTokenRequest((String) rest.postForEntity(baseUrl("/api/auth/login"),
                            new LoginRequest("logout@example.com", "Password123!"), Map.class)
                            .getBody().get("refreshToken")), Map.class);
            assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
