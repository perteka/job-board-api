package com.jobboard.api.dto.request;

import com.jobboard.api.entity.enums.EmploymentType;
import com.jobboard.api.entity.enums.WorkMode;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record JobPostingRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 50000) String description,
        @Size(max = 10000) String requirements,
        @NotBlank @Size(max = 150) String location,
        @NotNull EmploymentType employmentType,
        @NotNull WorkMode workMode,
        @PositiveOrZero BigDecimal minSalary,
        @PositiveOrZero BigDecimal maxSalary,
        @NotNull Long categoryId,
        @Size(max = 20) Set<Long> skillIds,
        @Future Instant expiresAt
) {
}
