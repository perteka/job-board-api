package com.jobboard.api.dto.response;

import com.jobboard.api.entity.enums.Role;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role
) {
}
