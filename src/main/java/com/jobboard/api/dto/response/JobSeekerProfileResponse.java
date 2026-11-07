package com.jobboard.api.dto.response;

import java.time.Instant;

public record JobSeekerProfileResponse(
        Long id,
        Long userId,
        String headline,
        String summary,
        Integer experienceYears,
        String cvUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
