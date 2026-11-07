package com.jobboard.api.mapper;

import com.jobboard.api.dto.response.JobPostingResponse;
import com.jobboard.api.dto.response.SkillResponse;
import com.jobboard.api.entity.JobPosting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JobPostingMapper {

    private final CompanyMapper companyMapper;
    private final TaxonomyMapper taxonomyMapper;

    public JobPostingResponse toResponse(JobPosting jobPosting) {
        List<SkillResponse> skills = jobPosting.getSkills().stream()
                .map(taxonomyMapper::toSkillResponse)
                .sorted(Comparator.comparing(SkillResponse::name))
                .toList();

        return new JobPostingResponse(
                jobPosting.getId(),
                jobPosting.getTitle(),
                jobPosting.getDescription(),
                jobPosting.getRequirements(),
                jobPosting.getLocation(),
                jobPosting.getEmploymentType(),
                jobPosting.getWorkMode(),
                jobPosting.getMinSalary(),
                jobPosting.getMaxSalary(),
                jobPosting.getStatus(),
                jobPosting.getPublishedAt(),
                jobPosting.getExpiresAt(),
                jobPosting.getCreatedAt(),
                companyMapper.toSummary(jobPosting.getCompany()),
                taxonomyMapper.toCategoryResponse(jobPosting.getCategory()),
                skills,
                jobPosting.getCreatedBy().getId()
        );
    }
}
