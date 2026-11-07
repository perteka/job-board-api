package com.jobboard.api.mapper;

import com.jobboard.api.dto.response.JobSeekerProfileResponse;
import com.jobboard.api.entity.JobSeekerProfile;
import org.springframework.stereotype.Component;

@Component
public class JobSeekerProfileMapper {

    public JobSeekerProfileResponse toResponse(JobSeekerProfile profile) {
        return new JobSeekerProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getHeadline(),
                profile.getSummary(),
                profile.getExperienceYears(),
                profile.getCvUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
