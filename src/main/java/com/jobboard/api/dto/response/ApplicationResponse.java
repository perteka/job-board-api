package com.jobboard.api.dto.response;

import com.jobboard.api.entity.enums.ApplicationStatus;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        String coverLetter,
        String cvUrl,
        ApplicationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
