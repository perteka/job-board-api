package com.jobboard.api.service;

import com.jobboard.api.exception.ForbiddenException;
import com.jobboard.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    private final FileStorageService fileStorageService = new FileStorageService(null, null);

    @Test
    void presignedUrlForUser_rejectsOtherUsersObjectKey() {
        assertThatThrownBy(() -> fileStorageService.presignedUrlForUser("cvs/8/private.pdf", 7L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void validateObjectKeyForUser_rejectsExternalUrl() {
        assertThatThrownBy(() -> fileStorageService.validateObjectKeyForUser("https://example.com/cv.pdf", 7L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateObjectKeyForUser_acceptsOwnObjectKey() {
        assertThatCode(() -> fileStorageService.validateObjectKeyForUser("cvs/7/cv.pdf", 7L))
                .doesNotThrowAnyException();
    }
}
