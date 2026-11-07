package com.jobboard.api.dto.response;

import java.time.Instant;

public record CompanyResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String website,
        String location,
        String logoUrl,
        Instant createdAt
) {
}
