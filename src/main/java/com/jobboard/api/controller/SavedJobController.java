package com.jobboard.api.controller;

import com.jobboard.api.dto.response.PageResponse;
import com.jobboard.api.dto.response.SavedJobResponse;
import com.jobboard.api.security.UserPrincipal;
import com.jobboard.api.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SavedJobController {

    private final SavedJobService savedJobService;

    @PostMapping("/jobs/{jobId}/save")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<SavedJobResponse> save(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedJobService.save(jobId, principal.getId()));
    }

    @DeleteMapping("/jobs/{jobId}/save")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Void> remove(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        savedJobService.remove(jobId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/saved-jobs")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<PageResponse<SavedJobResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(savedJobService.list(principal.getId(), pageable));
    }
}
