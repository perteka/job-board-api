package com.jobboard.api.dto.request;

import com.jobboard.api.entity.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusUpdateRequest(
        @NotNull ApplicationStatus status
) {
}
