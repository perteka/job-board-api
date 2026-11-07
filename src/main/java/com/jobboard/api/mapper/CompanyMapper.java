package com.jobboard.api.mapper;

import com.jobboard.api.dto.response.CompanyResponse;
import com.jobboard.api.dto.response.CompanySummaryResponse;
import com.jobboard.api.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getOwner().getId(),
                company.getName(),
                company.getDescription(),
                company.getWebsite(),
                company.getLocation(),
                company.getLogoUrl(),
                company.getCreatedAt()
        );
    }

    public CompanySummaryResponse toSummary(Company company) {
        return new CompanySummaryResponse(company.getId(), company.getName(), company.getLocation());
    }
}
