package com.jobboard.api.integration;

import com.jobboard.api.dto.request.ApplicationRequest;
import com.jobboard.api.dto.request.ApplicationStatusUpdateRequest;
import com.jobboard.api.entity.enums.ApplicationStatus;
import com.jobboard.api.entity.enums.EmploymentType;
import com.jobboard.api.entity.enums.JobStatus;
import com.jobboard.api.entity.enums.Role;
import com.jobboard.api.entity.enums.WorkMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class ApplicationIntegrationTest extends AbstractIntegrationTest {

    private String employerToken;
    private String candidateToken;
    private Long jobId;

    private HttpHeaders authHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private void setupEmployerWithPublishedJob() {
        rest.postForEntity(baseUrl("/api/auth/register"),
                new com.jobboard.api.dto.request.RegisterRequest(
                        "app-employer@example.com", "Password123!", "App Employer", Role.EMPLOYER), Map.class);
        ResponseEntity<Map> login = rest.postForEntity(baseUrl("/api/auth/login"),
                new com.jobboard.api.dto.request.LoginRequest("app-employer@example.com", "Password123!"), Map.class);
        employerToken = (String) login.getBody().get("accessToken");

        rest.exchange(baseUrl("/api/companies"), HttpMethod.POST,
                new HttpEntity<>(new com.jobboard.api.dto.request.CompanyRequest(
                        "App Company", null, null, null, null), authHeader(employerToken)), Map.class);

        var jobRequest = new com.jobboard.api.dto.request.JobPostingRequest(
                "Java Developer", "Description", "Requirements", "Remote",
                EmploymentType.FULL_TIME, WorkMode.REMOTE,
                new BigDecimal("50000"), new BigDecimal("80000"),
                1L, java.util.Set.of(), Instant.now().plusSeconds(3600));
        ResponseEntity<Map> created = rest.exchange(baseUrl("/api/jobs"),
                HttpMethod.POST, new HttpEntity<>(jobRequest, authHeader(employerToken)), Map.class);
        jobId = Long.valueOf(created.getBody().get("id").toString());
        rest.exchange(baseUrl("/api/jobs/" + jobId + "/publish"),
                HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);
    }

    private void registerCandidate() {
        rest.postForEntity(baseUrl("/api/auth/register"),
                new com.jobboard.api.dto.request.RegisterRequest(
                        "app-candidate@example.com", "Password123!", "App Candidate", Role.CANDIDATE), Map.class);
        ResponseEntity<Map> login = rest.postForEntity(baseUrl("/api/auth/login"),
                new com.jobboard.api.dto.request.LoginRequest("app-candidate@example.com", "Password123!"), Map.class);
        candidateToken = (String) login.getBody().get("accessToken");
    }

    private String uploadCv(String filename, String contentType) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource("testfiles/" + filename));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = rest.exchange(baseUrl("/api/files/cv"),
                HttpMethod.POST, request, Map.class, authHeader(candidateToken));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("objectKey");
    }

    @Nested
    class Application {
        @Test
        void appliesWithOwnCvObjectKey() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();
            String objectKey = uploadCv("cv.pdf", "application/pdf");

            ResponseEntity<Map> response = rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", objectKey),
                            authHeader(candidateToken)), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().get("status")).isEqualTo(ApplicationStatus.APPLIED.name());
        }

        @Test
        void rejectsDuplicateApplication() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();
            String objectKey = uploadCv("cv.pdf", "application/pdf");

            rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", objectKey),
                            authHeader(candidateToken)), Map.class);

            ResponseEntity<Map> response = rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", objectKey),
                            authHeader(candidateToken)), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void rejectsApplicationToUnpublishedJob() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();
            // Close the job
            rest.exchange(baseUrl("/api/jobs/" + jobId + "/close"),
                    HttpMethod.PATCH, new HttpEntity<>(authHeader(employerToken)), Map.class);

            ResponseEntity<Map> response = rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", "cvs/1/cv.pdf"),
                            authHeader(candidateToken)), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void rejectsCvOwnedByAnotherUser() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();

            // Try to use another user's CV object key
            ResponseEntity<Map> response = rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", "cvs/999/other.pdf"),
                            authHeader(candidateToken)), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void employerSeesApplicationsForOwnJob() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();
            String objectKey = uploadCv("cv.pdf", "application/pdf");
            rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", objectKey),
                            authHeader(candidateToken)), Map.class);

            ResponseEntity<Map> response = rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.GET, new HttpEntity<>(authHeader(employerToken)), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("content")).isNotNull();
        }

        @Test
        void employerUpdatesApplicationStatus() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();
            String objectKey = uploadCv("cv.pdf", "application/pdf");
            ResponseEntity<Map> apply = rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", objectKey),
                            authHeader(candidateToken)), Map.class);
            Long applicationId = Long.valueOf(apply.getBody().get("id").toString());

            ResponseEntity<Map> update = rest.exchange(baseUrl("/api/applications/" + applicationId + "/status"),
                    HttpMethod.PATCH, new HttpEntity<>(new ApplicationStatusUpdateRequest(ApplicationStatus.REVIEWED),
                            authHeader(employerToken)), Map.class);
            assertThat(update.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(update.getBody().get("status")).isEqualTo(ApplicationStatus.REVIEWED.name());
        }

        @Test
        void rejectsStatusUpdateByNonOwner() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();
            String objectKey = uploadCv("cv.pdf", "application/pdf");
            ResponseEntity<Map> apply = rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", objectKey),
                            authHeader(candidateToken)), Map.class);
            Long applicationId = Long.valueOf(apply.getBody().get("id").toString());

            // Register another employer and try to update
            rest.postForEntity(baseUrl("/api/auth/register"),
                    new com.jobboard.api.dto.request.RegisterRequest(
                            "other-employer@example.com", "Password123!", "Other Employer", Role.EMPLOYER), Map.class);
            ResponseEntity<Map> otherLogin = rest.postForEntity(baseUrl("/api/auth/login"),
                    new com.jobboard.api.dto.request.LoginRequest("other-employer@example.com", "Password123!"), Map.class);
            String otherToken = (String) otherLogin.getBody().get("accessToken");

            ResponseEntity<Map> update = rest.exchange(baseUrl("/api/applications/" + applicationId + "/status"),
                    HttpMethod.PATCH, new HttpEntity<>(new ApplicationStatusUpdateRequest(ApplicationStatus.REVIEWED),
                            authHeader(otherToken)), Map.class);
            assertThat(update.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    class CvPresign {
        @Test
        void candidateGetsPresignedUrlForOwnCv() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();
            String objectKey = uploadCv("cv.pdf", "application/pdf");

            ResponseEntity<Map> response = rest.exchange(baseUrl("/api/files/presign?objectKey=" + objectKey),
                    HttpMethod.GET, new HttpEntity<>(authHeader(candidateToken)), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("url")).isNotNull();
            assertThat(response.getBody().get("url").toString()).contains(objectKey);
        }

        @Test
        void candidateCannotGetPresignedUrlForAnotherUsersCv() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();

            ResponseEntity<Map> response = rest.exchange(baseUrl("/api/files/presign?objectKey=cvs/999/other.pdf"),
                    HttpMethod.GET, new HttpEntity<>(authHeader(candidateToken)), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void employerGetsCvUrlForOwnJobApplication() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();
            String objectKey = uploadCv("cv.pdf", "application/pdf");
            ResponseEntity<Map> apply = rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", objectKey),
                            authHeader(candidateToken)), Map.class);
            Long applicationId = Long.valueOf(apply.getBody().get("id").toString());

            ResponseEntity<Map> cvUrl = rest.exchange(baseUrl("/api/files/applications/" + applicationId + "/cv-url"),
                    HttpMethod.GET, new HttpEntity<>(authHeader(employerToken)), Map.class);
            assertThat(cvUrl.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(cvUrl.getBody().get("url")).isNotNull();
        }

        @Test
        void employerCannotGetCvUrlForAnotherEmployersApplication() throws Exception {
            setupEmployerWithPublishedJob();
            registerCandidate();
            String objectKey = uploadCv("cv.pdf", "application/pdf");
            ResponseEntity<Map> apply = rest.exchange(baseUrl("/api/jobs/" + jobId + "/applications"),
                    HttpMethod.POST, new HttpEntity<>(new ApplicationRequest("Cover letter", objectKey),
                            authHeader(candidateToken)), Map.class);
            Long applicationId = Long.valueOf(apply.getBody().get("id").toString());

            // Another employer tries
            rest.postForEntity(baseUrl("/api/auth/register"),
                    new com.jobboard.api.dto.request.RegisterRequest(
                            "cv-other@example.com", "Password123!", "CV Other", Role.EMPLOYER), Map.class);
            ResponseEntity<Map> otherLogin = rest.postForEntity(baseUrl("/api/auth/login"),
                    new com.jobboard.api.dto.request.LoginRequest("cv-other@example.com", "Password123!"), Map.class);
            String otherToken = (String) otherLogin.getBody().get("accessToken");

            ResponseEntity<Map> cvUrl = rest.exchange(baseUrl("/api/files/applications/" + applicationId + "/cv-url"),
                    HttpMethod.GET, new HttpEntity<>(authHeader(otherToken)), Map.class);
            assertThat(cvUrl.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
