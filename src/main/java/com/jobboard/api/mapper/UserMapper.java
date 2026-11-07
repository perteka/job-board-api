package com.jobboard.api.mapper;

import com.jobboard.api.dto.response.UserResponse;
import com.jobboard.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
