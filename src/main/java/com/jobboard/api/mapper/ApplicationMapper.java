package com.jobboard.api.mapper;

import com.jobboard.api.dto.response.ApplicationResponse;
import com.jobboard.api.entity.Application;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJobPosting().getId(),
                application.getJobPosting().getTitle(),
                application.getJobSeeker().getId(),
                application.getJobSeeker().getFullName(),
                application.getJobSeeker().getEmail(),
                application.getCoverLetter(),
                application.getCvUrl(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
