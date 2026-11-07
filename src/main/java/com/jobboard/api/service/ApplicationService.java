package com.jobboard.api.service;

import com.jobboard.api.dto.request.ApplicationRequest;
import com.jobboard.api.dto.request.ApplicationStatusUpdateRequest;
import com.jobboard.api.dto.response.ApplicationResponse;
import com.jobboard.api.dto.response.PageResponse;
import com.jobboard.api.entity.Application;
import com.jobboard.api.entity.JobPosting;
import com.jobboard.api.entity.JobSeekerProfile;
import com.jobboard.api.entity.User;
import com.jobboard.api.entity.enums.ApplicationStatus;
import com.jobboard.api.entity.enums.JobStatus;
import com.jobboard.api.entity.enums.Role;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.ConflictException;
import com.jobboard.api.exception.ForbiddenException;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.mapper.ApplicationMapper;
import com.jobboard.api.repository.ApplicationRepository;
import com.jobboard.api.repository.JobPostingRepository;
import com.jobboard.api.repository.JobSeekerProfileRepository;
import com.jobboard.api.repository.UserRepository;
import com.jobboard.api.util.PageableSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final Set<String> APPLICATION_SORT_PROPERTIES = Set.of(
            "createdAt", "updatedAt", "status");

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final JobSeekerProfileRepository profileRepository;
    private final ApplicationMapper applicationMapper;
    private final MailService mailService;
    private final FileStorageService fileStorageService;

    @Transactional
    public ApplicationResponse apply(Long jobId, Long candidateId, ApplicationRequest request) {
        JobPosting jobPosting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("İlan bulunamadı"));

        if (jobPosting.getStatus() != JobStatus.PUBLISHED) {
            throw new BadRequestException("Yalnızca yayındaki ilanlara başvuru yapılabilir");
        }
        if (jobPosting.getExpiresAt() != null && jobPosting.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("İlanın başvuru süresi dolmuş");
        }

        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Aday bulunamadı"));

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new ForbiddenException("Yalnızca adaylar başvuru yapabilir");
        }
        if (applicationRepository.existsByJobPostingIdAndJobSeekerId(jobId, candidateId)) {
            throw new ConflictException("Bu ilana zaten başvurdunuz");
        }

        String cvUrl = normalize(request.cvUrl());
        if (cvUrl == null) {
            cvUrl = profileRepository.findByUserId(candidateId)
                    .map(JobSeekerProfile::getCvUrl)
                    .orElse(null);
        }
        if (cvUrl == null) {
            throw new BadRequestException("Başvuru için CV gereklidir");
        }
        fileStorageService.validateObjectKeyForUser(cvUrl, candidateId);

        Application application = new Application();
        application.setJobPosting(jobPosting);
        application.setJobSeeker(candidate);
        application.setCoverLetter(request.coverLetter());
        application.setCvUrl(cvUrl);
        application.setStatus(ApplicationStatus.APPLIED);

        Application saved = applicationRepository.save(application);
        String candidateEmail = candidate.getEmail();
        String candidateName = candidate.getFullName();
        String jobTitle = jobPosting.getTitle();
        afterCommit(() -> mailService.sendApplicationReceived(candidateEmail, candidateName, jobTitle));

        return applicationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listMyApplications(Long candidateId, Pageable pageable) {
        Pageable safePageable = PageableSupport.requireAllowedSort(pageable, APPLICATION_SORT_PROPERTIES);
        return PageResponse.from(applicationRepository
                .findByJobSeekerId(candidateId, safePageable)
                .map(applicationMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listByJob(Long jobId, Long employerId, Pageable pageable) {
        JobPosting jobPosting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("İlan bulunamadı"));

        if (!jobPosting.getCompany().getOwner().getId().equals(employerId)) {
            throw new ForbiddenException("Bu ilanın başvurularını görme yetkiniz yok");
        }

        Pageable safePageable = PageableSupport.requireAllowedSort(pageable, APPLICATION_SORT_PROPERTIES);
        return PageResponse.from(applicationRepository
                .findByJobPostingId(jobId, safePageable)
                .map(applicationMapper::toResponse));
    }

    @Transactional
    public ApplicationResponse updateStatus(
            Long applicationId,
            Long employerId,
            ApplicationStatusUpdateRequest request
    ) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Başvuru bulunamadı"));

        if (!application.getJobPosting().getCompany().getOwner().getId().equals(employerId)) {
            throw new ForbiddenException("Bu başvuruyu değerlendirme yetkiniz yok");
        }

        ApplicationStatus target = request.status();
        if (application.getStatus() == target) {
            return applicationMapper.toResponse(application);
        }
        if (!application.getStatus().canTransitionTo(target)) {
            throw new BadRequestException(
                    "Geçersiz durum geçişi: " + application.getStatus() + " -> " + target
            );
        }

        application.setStatus(target);
        String candidateEmail = application.getJobSeeker().getEmail();
        String candidateName = application.getJobSeeker().getFullName();
        String jobTitle = application.getJobPosting().getTitle();
        afterCommit(() -> mailService.sendApplicationStatusChanged(
                candidateEmail, candidateName, jobTitle, target));

        return applicationMapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public String getCvObjectKeyForEmployer(Long applicationId, Long employerId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Başvuru bulunamadı"));
        if (!application.getJobPosting().getCompany().getOwner().getId().equals(employerId)) {
            throw new ForbiddenException("Bu başvurunun CV'sine erişim yetkiniz yok");
        }
        if (application.getCvUrl() == null || application.getCvUrl().isBlank()) {
            throw new ResourceNotFoundException("Başvuruya ait CV bulunamadı");
        }
        return application.getCvUrl();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
