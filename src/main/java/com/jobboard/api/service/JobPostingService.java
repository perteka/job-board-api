package com.jobboard.api.service;

import com.jobboard.api.dto.request.JobPostingRequest;
import com.jobboard.api.dto.response.JobPostingResponse;
import com.jobboard.api.dto.response.PageResponse;
import com.jobboard.api.entity.Category;
import com.jobboard.api.entity.Company;
import com.jobboard.api.entity.JobPosting;
import com.jobboard.api.entity.Skill;
import com.jobboard.api.entity.enums.JobStatus;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.ForbiddenException;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.mapper.JobPostingMapper;
import com.jobboard.api.repository.CategoryRepository;
import com.jobboard.api.repository.CompanyRepository;
import com.jobboard.api.repository.JobPostingRepository;
import com.jobboard.api.repository.SkillRepository;
import com.jobboard.api.util.PageableSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final JobPostingMapper jobPostingMapper;

    @Transactional
    public JobPostingResponse create(Long employerId, JobPostingRequest request) {
        Company company = companyRepository.findByOwnerId(employerId)
                .orElseThrow(() -> new BadRequestException("İlan oluşturmadan önce firma oluşturmalısınız"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı"));

        validateSalary(request);

        JobPosting jobPosting = new JobPosting();
        jobPosting.setCompany(company);
        jobPosting.setCreatedBy(company.getOwner());
        jobPosting.setCategory(category);
        jobPosting.setStatus(JobStatus.DRAFT);
        apply(jobPosting, request);

        return jobPostingMapper.toResponse(jobPostingRepository.save(jobPosting));
    }

    @Transactional
    public JobPostingResponse update(Long jobId, Long employerId, JobPostingRequest request) {
        JobPosting jobPosting = findOwned(jobId, employerId);

        if (jobPosting.getStatus() == JobStatus.CLOSED || jobPosting.getStatus() == JobStatus.ARCHIVED) {
            throw new BadRequestException("Kapatılmış veya arşivlenmiş ilan düzenlenemez");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı"));

        validateSalary(request);
        jobPosting.setCategory(category);
        apply(jobPosting, request);

        return jobPostingMapper.toResponse(jobPosting);
    }

    @Transactional
    public JobPostingResponse publish(Long jobId, Long employerId) {
        JobPosting jobPosting = findOwned(jobId, employerId);

        if (jobPosting.getStatus() != JobStatus.DRAFT && jobPosting.getStatus() != JobStatus.CLOSED) {
            throw new BadRequestException("Yalnızca taslak veya kapatılmış ilan yayınlanabilir");
        }
        if (jobPosting.getExpiresAt() != null && !jobPosting.getExpiresAt().isAfter(Instant.now())) {
            throw new BadRequestException("Süresi dolmuş ilan yayınlanamaz");
        }

        jobPosting.setStatus(JobStatus.PUBLISHED);
        jobPosting.setPublishedAt(Instant.now());
        return jobPostingMapper.toResponse(jobPosting);
    }

    @Transactional
    public JobPostingResponse close(Long jobId, Long employerId) {
        JobPosting jobPosting = findOwned(jobId, employerId);

        if (jobPosting.getStatus() != JobStatus.PUBLISHED) {
            throw new BadRequestException("Yalnızca yayındaki ilan kapatılabilir");
        }

        jobPosting.setStatus(JobStatus.CLOSED);
        return jobPostingMapper.toResponse(jobPosting);
    }

    @Transactional
    public JobPostingResponse archive(Long jobId, Long employerId) {
        JobPosting jobPosting = findOwned(jobId, employerId);

        if (jobPosting.getStatus() == JobStatus.ARCHIVED) {
            throw new BadRequestException("İlan zaten arşivlenmiş");
        }

        jobPosting.setStatus(JobStatus.ARCHIVED);
        return jobPostingMapper.toResponse(jobPosting);
    }

    @Transactional(readOnly = true)
    public JobPostingResponse getVisible(Long jobId, Long viewerId) {
        JobPosting jobPosting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("İlan bulunamadı"));

        boolean owner = viewerId != null && jobPosting.getCreatedBy().getId().equals(viewerId);
        boolean expired = jobPosting.getExpiresAt() != null
                && jobPosting.getExpiresAt().isBefore(Instant.now());

        if ((jobPosting.getStatus() != JobStatus.PUBLISHED || expired) && !owner) {
            throw new ResourceNotFoundException("İlan bulunamadı");
        }

        return jobPostingMapper.toResponse(jobPosting);
    }

    private static final Set<String> JOB_SORT_PROPERTIES = Set.of(
            "createdAt", "updatedAt", "title", "publishedAt", "expiresAt", "minSalary", "maxSalary");

    @Transactional(readOnly = true)
    public PageResponse<JobPostingResponse> search(String keyword, String city, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : escapeLike(keyword.trim());
        String normalizedCity = city == null ? "" : escapeLike(city.trim());
        Pageable safePageable = PageableSupport.requireAllowedSort(pageable, JOB_SORT_PROPERTIES);

        return PageResponse.from(jobPostingRepository
                .search(JobStatus.PUBLISHED, normalizedKeyword, normalizedCity, Instant.now(), safePageable)
                .map(jobPostingMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobPostingResponse> getMyJobs(Long employerId, Pageable pageable) {
        Pageable safePageable = PageableSupport.requireAllowedSort(pageable, JOB_SORT_PROPERTIES);
        return PageResponse.from(jobPostingRepository
                .findByCreatedById(employerId, safePageable)
                .map(jobPostingMapper::toResponse));
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private JobPosting findOwned(Long jobId, Long employerId) {
        JobPosting jobPosting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("İlan bulunamadı"));

        if (!jobPosting.getCreatedBy().getId().equals(employerId)) {
            throw new ForbiddenException("Bu ilan üzerinde işlem yapma yetkiniz yok");
        }

        return jobPosting;
    }

    private void apply(JobPosting jobPosting, JobPostingRequest request) {
        jobPosting.setTitle(request.title().trim());
        jobPosting.setDescription(request.description());
        jobPosting.setRequirements(request.requirements());
        jobPosting.setLocation(request.location().trim());
        jobPosting.setEmploymentType(request.employmentType());
        jobPosting.setWorkMode(request.workMode());
        jobPosting.setMinSalary(request.minSalary());
        jobPosting.setMaxSalary(request.maxSalary());
        jobPosting.setExpiresAt(request.expiresAt());
        jobPosting.setSkills(resolveSkills(request.skillIds()));
    }

    private Set<Skill> resolveSkills(Set<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Skill> skills = new HashSet<>(skillRepository.findAllById(skillIds));
        if (skills.size() != skillIds.size()) {
            throw new BadRequestException("Geçersiz yetenek seçimi");
        }
        return skills;
    }

    private void validateSalary(JobPostingRequest request) {
        if (request.minSalary() != null
                && request.maxSalary() != null
                && request.minSalary().compareTo(request.maxSalary()) > 0) {
            throw new BadRequestException("Minimum maaş maksimum maaştan büyük olamaz");
        }
    }
}
