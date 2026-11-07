package com.jobboard.api.controller;

import com.jobboard.api.dto.request.JobSeekerProfileRequest;
import com.jobboard.api.dto.response.JobSeekerProfileResponse;
import com.jobboard.api.security.UserPrincipal;
import com.jobboard.api.service.JobSeekerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final JobSeekerProfileService profileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<JobSeekerProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(profileService.get(principal.getId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<JobSeekerProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JobSeekerProfileRequest request
    ) {
        return ResponseEntity.ok(profileService.update(principal.getId(), request));
    }
}
