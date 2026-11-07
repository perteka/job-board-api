package com.jobboard.api.service;

import com.jobboard.api.dto.request.JobPostingRequest;
import com.jobboard.api.entity.*;
import com.jobboard.api.entity.enums.*;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.ForbiddenException;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.mapper.JobPostingMapper;
import com.jobboard.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobPostingServiceTest {
    private final JobPostingRepository jobs = mock(JobPostingRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final SkillRepository skills = mock(SkillRepository.class);
    private final JobPostingMapper mapper = mock(JobPostingMapper.class);
    private final JobPostingService service = new JobPostingService(jobs, companies, categories, skills, mapper);
    private User employer;
    private JobPosting job;

    @BeforeEach
    void setUp() {
        employer = new User(); employer.setId(5L); employer.setRole(Role.EMPLOYER);
        Company company = new Company(); company.setId(2L); company.setOwner(employer);
        job = new JobPosting(); job.setId(3L); job.setCompany(company); job.setCreatedBy(employer);
        job.setStatus(JobStatus.DRAFT); job.setExpiresAt(Instant.now().plusSeconds(3600));
        when(jobs.findById(3L)).thenReturn(Optional.of(job));
    }

    @Test
    void publishTransitionsOwnedDraftAndSetsPublishedAt() {
        when(mapper.toResponse(job)).thenReturn(null);
        service.publish(3L, 5L);
        assertThat(job.getStatus()).isEqualTo(JobStatus.PUBLISHED);
        assertThat(job.getPublishedAt()).isNotNull();
    }

    @Test
    void publishRejectsExpiredPosting() {
        job.setExpiresAt(Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> service.publish(3L, 5L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void publishRejectsPostingNotInPublishableState() {
        job.setStatus(JobStatus.PUBLISHED);
        assertThatThrownBy(() -> service.publish(3L, 5L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void operationsRejectNonOwner() {
        assertThatThrownBy(() -> service.publish(3L, 99L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateRejectsArchivedPosting() {
        job.setStatus(JobStatus.ARCHIVED);
        assertThatThrownBy(() -> service.update(3L, 5L, request(null, null, Set.of())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createRejectsMinSalaryAboveMaxSalary() {
        when(companies.findByOwnerId(5L)).thenReturn(Optional.of(job.getCompany()));
        when(categories.findById(1L)).thenReturn(Optional.of(new Category()));
        assertThatThrownBy(() -> service.create(5L, request(new BigDecimal("90000"),
                new BigDecimal("80000"), Set.of())))
                .isInstanceOf(BadRequestException.class);
        verify(jobs, never()).save(any());
    }

    @Test
    void createRejectsUnknownSkillIds() {
        when(companies.findByOwnerId(5L)).thenReturn(Optional.of(job.getCompany()));
        when(categories.findById(1L)).thenReturn(Optional.of(new Category()));
        when(skills.findAllById(Set.of(10L, 11L))).thenReturn(List.of(new Skill()));
        assertThatThrownBy(() -> service.create(5L, request(null, null, Set.of(10L, 11L))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getVisibleHidesDraftFromNonOwner() {
        assertThatThrownBy(() -> service.getVisible(3L, 7L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getVisibleAllowsOwnerToSeeDraft() {
        when(mapper.toResponse(job)).thenReturn(null);
        assertThat(service.getVisible(3L, 5L)).isNull();
    }

    private JobPostingRequest request(BigDecimal min, BigDecimal max, Set<Long> skillIds) {
        return new JobPostingRequest(" Java Engineer ", "Description", null, " Remote ",
                EmploymentType.FULL_TIME, WorkMode.REMOTE, min, max, 1L, skillIds,
                Instant.now().plusSeconds(3600));
    }
}
