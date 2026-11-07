package com.jobboard.api.mapper;

import com.jobboard.api.dto.response.SavedJobResponse;
import com.jobboard.api.entity.SavedJob;
import org.springframework.stereotype.Component;

@Component
public class SavedJobMapper {

    public SavedJobResponse toResponse(SavedJob savedJob) {
        return new SavedJobResponse(
                savedJob.getJobPosting().getId(),
                savedJob.getJobPosting().getTitle(),
                savedJob.getJobPosting().getCompany().getName(),
                savedJob.getJobPosting().getLocation(),
                savedJob.getJobPosting().getStatus(),
                savedJob.getCreatedAt()
        );
    }
}
