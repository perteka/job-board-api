package com.jobboard.api.controller;

import com.jobboard.api.dto.request.CompanyRequest;
import com.jobboard.api.dto.response.CompanyResponse;
import com.jobboard.api.security.UserPrincipal;
import com.jobboard.api.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<CompanyResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompanyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.create(principal.getId(), request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<CompanyResponse> getMyCompany(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyService.getMyCompany(principal.getId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<CompanyResponse> updateMyCompany(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompanyRequest request
    ) {
        return ResponseEntity.ok(companyService.update(principal.getId(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getById(id));
    }
}
