package com.jobboard.api.controller;

import com.jobboard.api.dto.response.FileUploadResponse;
import com.jobboard.api.dto.response.FileUrlResponse;
import com.jobboard.api.exception.TooManyRequestsException;
import com.jobboard.api.security.RateLimiter;
import com.jobboard.api.security.UserPrincipal;
import com.jobboard.api.service.FileStorageService;
import com.jobboard.api.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final ApplicationService applicationService;
    private final RateLimiter rateLimiter;

    @Value("${app.security.rate-limit.cv-upload-limit:20}")
    private int cvUploadLimit;

    @Value("${app.security.rate-limit.cv-upload-window-seconds:3600}")
    private long cvUploadWindowSeconds;

    @PostMapping(value = "/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<FileUploadResponse> uploadCv(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (!rateLimiter.tryConsume(
                "rl:cv-upload:" + principal.getId(), cvUploadLimit, Duration.ofSeconds(cvUploadWindowSeconds))) {
            throw new TooManyRequestsException("Çok fazla dosya yükleme isteği. Lütfen daha sonra tekrar deneyin");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileStorageService.uploadCv(file, principal.getId()));
    }

    @GetMapping("/presign")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<FileUrlResponse> presignedUrl(
            @RequestParam String objectKey,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(new FileUrlResponse(
                fileStorageService.presignedUrlForUser(objectKey, principal.getId())));
    }

    @GetMapping("/applications/{applicationId}/cv-url")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<FileUrlResponse> applicationCvUrl(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String objectKey = applicationService.getCvObjectKeyForEmployer(applicationId, principal.getId());
        return ResponseEntity.ok(new FileUrlResponse(fileStorageService.presignedUrl(objectKey)));
    }
}
