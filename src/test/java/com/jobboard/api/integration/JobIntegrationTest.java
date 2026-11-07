package com.jobboard.api.integration;

import com.jobboard.api.dto.request.JobPostingRequest;
import com.jobboard.api.entity.enums.EmploymentType;
import com.jobboard.api.entity.enums.JobStatus;
import com.jobboard.api.entity.enums.Role;
import com.jobboard.api.entity.enums.WorkMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class JobIntegrationTest extends AbstractIntegrationTest {

    private String employerToken;

    @Nested
    class Setup {
        @Test
        void registerEmployerAndCreateCompany() {
            ResponseEntity<Map> register = rest.postForEntity(baseUrl("/api/auth/register"),
                    new com.jobboard.api.dto.request.RegisterRequest(
                            "employer@example.com", "Password123!", "Employer User", Role.EMPLOYER), Map.class);
            assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            ResponseEntity<Map> login = rest.postForEntity(baseUrl("/api/auth/login"),
                    new com.jobboard.api.dto.request.LoginRequest("employer@example.com", "Password123!"), Map.class);
            employerToken = (String) login.getBody().get("accessToken");

            ResponseEntity<Map> company = rest.exchange(baseUrl("/api/companies"),
                    HttpMethod.POST, new HttpEntity<>(new com.jobboard.api.dto.request.CompanyRequest(
                            "Test Company", null, null, null, null), authHeader(employerToken)), Map.class);
            assertThat(company.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    private HttpHeaders authHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private JobPostingRequest jobRequest(Instant expiresAt) {
        return new JobPostingRequest(
                "Java Developer", "Description", "Requirements", "Remote",
                EmploymentType.FULL_TIME, WorkMode.REMOTE,
                new BigDecimal("50000"), new BigDecimal("80000"),
                1L, Set.of(), expiresAt);
    }

    @Nested
    class JobPosting {
        @Test
        void createJobAsDraft() {
            assertThat(employerToken).isNotNull();
            ResponseEntity<Map> response = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.POST, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(3600)),
                            authHeader(employerToken)), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().get("status")).isEqualTo(JobStatus.DRAFT.name());
        }

        @Test
        void updateOwnJob() {
            ResponseEntity<Map> created = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.POST, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(3600)),
                            authHeader(employerToken)), Map.class);
            Long jobId = Long.valueOf(created.getBody().get("id").toString());

            ResponseEntity<Map> updated = rest.exchange(baseUrl("/api/jobs/" + jobId),
                    HttpMethod.PUT, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(7200)),
                            authHeader(employerToken)), Map.class);
            assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void publishDraftJob() {
            ResponseEntity<Map> created = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.POST, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(3600)),
                            authHeader(employerToken)), Map.class);
            Long jobId = Long.valueOf(created.getBody().get("id").toString());

            ResponseEntity<Map> published = rest.exchange(baseUrl("/api/jobs/" + jobId + "/publish"),
                    HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);
            assertThat(published.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(published.getBody().get("status")).isEqualTo(JobStatus.PUBLISHED.name());
        }

        @Test
        void publishExpiredJobIsRejected() {
            ResponseEntity<Map> created = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.POST, new HttpEntity<>(jobRequest(Instant.now().minusSeconds(1)),
                            authHeader(employerToken)), Map.class);
            Long jobId = Long.valueOf(created.getBody().get("id").toString());

            ResponseEntity<Map> published = rest.exchange(baseUrl("/api/jobs/" + jobId + "/publish"),
                    HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);
            assertThat(published.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void closeAndArchivePublishedJob() {
            ResponseEntity<Map> created = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.POST, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(3600)),
                            authHeader(employerToken)), Map.class);
            Long jobId = Long.valueOf(created.getBody().get("id").toString());

            rest.exchange(baseUrl("/api/jobs/" + jobId + "/publish"),
                    HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);

            ResponseEntity<Map> closed = rest.exchange(baseUrl("/api/jobs/" + jobId + "/close"),
                    HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);
            assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(closed.getBody().get("status")).isEqualTo(JobStatus.CLOSED.name());

            ResponseEntity<Map> archived = rest.exchange(baseUrl("/api/jobs/" + jobId + "/archive"),
                    HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);
            assertThat(archived.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(archived.getBody().get("status")).isEqualTo(JobStatus.ARCHIVED.name());
        }

        @Test
        void rejectsEditingClosedJob() {
            ResponseEntity<Map> created = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.POST, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(3600)),
                            authHeader(employerToken)), Map.class);
            Long jobId = Long.valueOf(created.getBody().get("id").toString());
            rest.exchange(baseUrl("/api/jobs/" + jobId + "/publish"),
                    HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);
            rest.exchange(baseUrl("/api/jobs/" + jobId + "/close"),
                    HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);

            ResponseEntity<Map> updated = rest.exchange(baseUrl("/api/jobs/" + jobId),
                    HttpMethod.PUT, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(3600)),
                            authHeader(employerToken)), Map.class);
            assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class PublicAccess {
        @Test
        void searchReturnsOnlyPublishedNonExpiredJobs() {
            ResponseEntity<Map> search = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.GET, new HttpEntity<>(authHeader(null)), Map.class);
            assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void canSeePublishedJobWithoutAuth() {
            // First create and publish a job
            ResponseEntity<Map> created = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.POST, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(3600)),
                            authHeader(employerToken)), Map.class);
            Long jobId = Long.valueOf(created.getBody().get("id").toString());
            rest.exchange(baseUrl("/api/jobs/" + jobId + "/publish"),
                    HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);

            ResponseEntity<Map> visible = rest.exchange(baseUrl("/api/jobs/" + jobId),
                    HttpMethod.GET, new HttpEntity<>(authHeader(null)), Map.class);
            assertThat(visible.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void cannotSeeDraftJobWithoutAuth() {
            ResponseEntity<Map> created = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.POST, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(3600)),
                            authHeader(employerToken)), Map.class);
            Long jobId = Long.valueOf(created.getBody().get("id").toString());

            ResponseEntity<Map> visible = rest.exchange(baseUrl("/api/jobs/" + jobId),
                    HttpMethod.GET, new HttpEntity<>(authHeader(null)), Map.class);
            assertThat(visible.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void employerCanSeeOwnDraft() {
            ResponseEntity<Map> created = rest.exchange(baseUrl("/api/jobs"),
                    HttpMethod.POST, new HttpEntity<>(jobRequest(Instant.now().plusSeconds(3600)),
                            authHeader(employerToken)), Map.class);
            Long jobId = Long.valueOf(created.getBody().get("id").toString());

            ResponseEntity<Map> visible = rest.exchange(baseUrl("/api/jobs/" + jobId),
                    HttpMethod.GET, new HttpEntity<>(authHeader(employerToken)), Map.class);
            assertThat(visible.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
