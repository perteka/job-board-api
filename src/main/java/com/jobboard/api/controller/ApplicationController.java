package com.jobboard.api.controller;

import com.jobboard.api.dto.request.ApplicationRequest;
import com.jobboard.api.dto.request.ApplicationStatusUpdateRequest;
import com.jobboard.api.dto.response.ApplicationResponse;
import com.jobboard.api.dto.response.PageResponse;
import com.jobboard.api.security.UserPrincipal;
import com.jobboard.api.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApplicationResponse> apply(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.apply(jobId, principal.getId(), request));
    }

    @GetMapping("/applications/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<PageResponse<ApplicationResponse>> myApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.listMyApplications(principal.getId(), pageable));
    }

    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<PageResponse<ApplicationResponse>> applicationsForJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.listByJob(jobId, principal.getId(), pageable));
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplicationStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(applicationService.updateStatus(id, principal.getId(), request));
    }
}
