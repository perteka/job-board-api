package com.jobboard.api.dto.response;

import com.jobboard.api.entity.enums.JobStatus;

import java.time.Instant;

public record SavedJobResponse(
        Long jobId,
        String title,
        String companyName,
        String location,
        JobStatus status,
        Instant savedAt
) {
}
