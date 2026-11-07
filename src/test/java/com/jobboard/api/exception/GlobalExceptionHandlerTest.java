package com.jobboard.api.exception;

import com.jobboard.api.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionUsesItsDeclaredStatusAndMessage() {
        HttpServletRequest request = request("/api/jobs/1");
        var response = handler.handleApiException(new BadRequestException("Invalid job"), request);
        ErrorResponse body = response.getBody();

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Invalid job");
        assertThat(body.path()).isEqualTo("/api/jobs/1");
    }

    @Test
    void uploadLimitErrorMapsToBadRequest() {
        var response = handler.handleUploadSize(request("/api/files/cv"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("Dosya boyutu");
    }

    @Test
    void unexpectedExceptionDoesNotExposeInternalMessage() {
        var response = handler.handleUnexpected(new IllegalStateException("secret internal detail"), request("/api/jobs"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).doesNotContain("secret internal detail");
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
