package com.jobboard.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record JobSeekerProfileRequest(
        @Size(max = 150) String headline,
        @Size(max = 5000) String summary,
        @Min(0) @Max(60) Integer experienceYears,
        @Size(max = 500) String cvUrl
) {
}
