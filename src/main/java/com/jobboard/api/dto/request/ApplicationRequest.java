package com.jobboard.api.dto.request;

import jakarta.validation.constraints.Size;

public record ApplicationRequest(
        @Size(max = 5000) String coverLetter,
        @Size(max = 500) String cvUrl
) {
}
