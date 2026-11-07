package com.jobboard.api.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public abstract class AbstractIntegrationTest {

    static final String JWT_SECRET = "integration-test-jwt-secret-must-be-at-least-32-characters";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("jobboard")
            .withUsername("jobboard")
            .withPassword("jobboard");

    @Container
    static MinIOContainer minio = new MinIOContainer(
            DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z"))
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.secret", () -> JWT_SECRET);
        registry.add("app.jwt.refresh-secret", () -> JWT_SECRET);
        registry.add("app.minio.endpoint",
                () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("app.minio.access-key", () -> "minioadmin");
        registry.add("app.minio.secret-key", () -> "minioadmin");
        registry.add("app.minio.bucket", () -> "integration-test-cvs");
        registry.add("app.mail.enabled", () -> false);
        registry.add("app.security.email-verification.enabled", () -> false);
        registry.add("app.security.login-max-failed-attempts", () -> 1000);
        registry.add("app.security.rate-limit.login-limit", () -> 100000);
        registry.add("app.security.rate-limit.register-limit", () -> 100000);
        registry.add("app.security.rate-limit.refresh-limit", () -> 100000);
        registry.add("app.security.rate-limit.resend-verification-limit", () -> 100000);
        registry.add("app.security.rate-limit.cv-upload-limit", () -> 100000);
    }

    @LocalServerPort
    protected int port;

    protected TestRestTemplate rest;

    @BeforeEach
    void setUp() {
        rest = new TestRestTemplate(new RestTemplateBuilder());
    }

    protected String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
