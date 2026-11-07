package com.jobboard.api.controller;

import com.jobboard.api.dto.request.JobPostingRequest;
import com.jobboard.api.dto.response.JobPostingResponse;
import com.jobboard.api.dto.response.PageResponse;
import com.jobboard.api.security.UserPrincipal;
import com.jobboard.api.service.JobPostingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Validated
public class JobPostingController {

    private final JobPostingService jobPostingService;

    @GetMapping
    public ResponseEntity<PageResponse<JobPostingResponse>> search(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 100) String city,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(jobPostingService.search(keyword, city, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<PageResponse<JobPostingResponse>> getMyJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(jobPostingService.getMyJobs(principal.getId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostingResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long viewerId = principal == null ? null : principal.getId();
        return ResponseEntity.ok(jobPostingService.getVisible(id, viewerId));
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobPostingResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JobPostingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobPostingService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobPostingResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JobPostingRequest request
    ) {
        return ResponseEntity.ok(jobPostingService.update(id, principal.getId(), request));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobPostingResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(jobPostingService.publish(id, principal.getId()));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobPostingResponse> close(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(jobPostingService.close(id, principal.getId()));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobPostingResponse> archive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(jobPostingService.archive(id, principal.getId()));
    }
}
