package com.jobboard.api.dto.response;

import com.jobboard.api.entity.enums.EmploymentType;
import com.jobboard.api.entity.enums.JobStatus;
import com.jobboard.api.entity.enums.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobPostingResponse(
        Long id,
        String title,
        String description,
        String requirements,
        String location,
        EmploymentType employmentType,
        WorkMode workMode,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        JobStatus status,
        Instant publishedAt,
        Instant expiresAt,
        Instant createdAt,
        CompanySummaryResponse company,
        CategoryResponse category,
        List<SkillResponse> skills,
        Long createdById
) {
}
