package com.jobboard.api.dto.response;

public record FileUploadResponse(
        String objectKey,
        String url,
        String originalFilename,
        String contentType,
        long size
) {
}
