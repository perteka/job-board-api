package com.jobboard.api.dto.response;

public record CompanySummaryResponse(
        Long id,
        String name,
        String location
) {
}
