package com.jobboard.api.dto.response;

public record CategoryResponse(
        Long id,
        String name,
        String slug
) {
}
