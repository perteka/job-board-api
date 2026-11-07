package com.jobboard.api.service;

import com.jobboard.api.dto.request.ApplicationRequest;
import com.jobboard.api.dto.request.ApplicationStatusUpdateRequest;
import com.jobboard.api.dto.response.ApplicationResponse;
import com.jobboard.api.entity.Application;
import com.jobboard.api.entity.Company;
import com.jobboard.api.entity.JobPosting;
import com.jobboard.api.entity.JobSeekerProfile;
import com.jobboard.api.entity.User;
import com.jobboard.api.entity.enums.ApplicationStatus;
import com.jobboard.api.entity.enums.JobStatus;
import com.jobboard.api.entity.enums.Role;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.ConflictException;
import com.jobboard.api.exception.ForbiddenException;
import com.jobboard.api.mapper.ApplicationMapper;
import com.jobboard.api.repository.ApplicationRepository;
import com.jobboard.api.repository.JobPostingRepository;
import com.jobboard.api.repository.JobSeekerProfileRepository;
import com.jobboard.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobSeekerProfileRepository profileRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private MailService mailService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ApplicationService applicationService;

    private User candidate;
    private User employer;
    private JobPosting jobPosting;
    private JobSeekerProfile profile;

    @BeforeEach
    void setUp() {
        employer = new User();
        employer.setId(1L);
        employer.setEmail("isveren@example.com");
        employer.setFullName("İşveren");
        employer.setRole(Role.EMPLOYER);

        candidate = new User();
        candidate.setId(2L);
        candidate.setEmail("aday@example.com");
        candidate.setFullName("Aday Kullanıcı");
        candidate.setRole(Role.CANDIDATE);

        Company company = new Company();
        company.setId(1L);
        company.setOwner(employer);
        company.setName("Örnek Firma");

        jobPosting = new JobPosting();
        jobPosting.setId(1L);
        jobPosting.setTitle("Java Developer");
        jobPosting.setCompany(company);
        jobPosting.setCreatedBy(employer);
        jobPosting.setStatus(JobStatus.PUBLISHED);
        jobPosting.setExpiresAt(Instant.now().plusSeconds(3600));

        profile = new JobSeekerProfile();
        profile.setId(1L);
        profile.setUser(candidate);
        profile.setCvUrl("cvs/2/cv.pdf");
    }

    @Test
    void apply_savesApplicationAndSendsMail() {
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(jobPosting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.existsByJobPostingIdAndJobSeekerId(1L, 2L)).thenReturn(false);
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(response(ApplicationStatus.APPLIED));

        ApplicationResponse result = applicationService.apply(
                1L,
                2L,
                new ApplicationRequest("Kısa bir ön yazı", null)
        );

        assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
        verify(applicationRepository).save(any(Application.class));
        verify(mailService).sendApplicationReceived(
                "aday@example.com",
                "Aday Kullanıcı",
                "Java Developer"
        );
    }

    @Test
    void apply_sendsMailOnlyAfterTransactionCommit() {
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(jobPosting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.existsByJobPostingIdAndJobSeekerId(1L, 2L)).thenReturn(false);
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(response(ApplicationStatus.APPLIED));

        TransactionSynchronizationManager.initSynchronization();
        try {
            applicationService.apply(1L, 2L, new ApplicationRequest("Cover letter", null));
            verify(mailService, never()).sendApplicationReceived(any(), any(), any());
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(mailService).sendApplicationReceived("aday@example.com", "Aday Kullanıcı", "Java Developer");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void apply_duplicateApplication_throwsConflict() {
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(jobPosting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.existsByJobPostingIdAndJobSeekerId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(
                1L,
                2L,
                new ApplicationRequest("Ön yazı", "cvs/2/cv.pdf")
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void apply_unpublishedJob_throwsBadRequest() {
        jobPosting.setStatus(JobStatus.DRAFT);
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(jobPosting));

        assertThatThrownBy(() -> applicationService.apply(
                1L,
                2L,
                new ApplicationRequest(null, "cvs/2/cv.pdf")
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_validTransition_updatesAndSendsMail() {
        Application application = application(ApplicationStatus.APPLIED);
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(applicationMapper.toResponse(application)).thenReturn(response(ApplicationStatus.REVIEWED));

        ApplicationResponse result = applicationService.updateStatus(
                10L,
                1L,
                new ApplicationStatusUpdateRequest(ApplicationStatus.REVIEWED)
        );

        assertThat(result.status()).isEqualTo(ApplicationStatus.REVIEWED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REVIEWED);
        verify(mailService).sendApplicationStatusChanged(
                "aday@example.com",
                "Aday Kullanıcı",
                "Java Developer",
                ApplicationStatus.REVIEWED
        );
    }

    @Test
    void updateStatus_invalidTransition_throwsBadRequest() {
        Application application = application(ApplicationStatus.APPLIED);
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.updateStatus(
                10L,
                1L,
                new ApplicationStatusUpdateRequest(ApplicationStatus.HIRED)
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_notOwner_throwsForbidden() {
        Application application = application(ApplicationStatus.APPLIED);
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.updateStatus(
                10L,
                99L,
                new ApplicationStatusUpdateRequest(ApplicationStatus.REVIEWED)
        )).isInstanceOf(ForbiddenException.class);
    }

    private Application application(ApplicationStatus status) {
        Application application = new Application();
        application.setId(10L);
        application.setJobPosting(jobPosting);
        application.setJobSeeker(candidate);
        application.setStatus(status);
        return application;
    }

    private ApplicationResponse response(ApplicationStatus status) {
        return new ApplicationResponse(
                10L,
                1L,
                "Java Developer",
                2L,
                "Aday Kullanıcı",
                "aday@example.com",
                "Ön yazı",
                "cvs/2/cv.pdf",
                status,
                Instant.now(),
                Instant.now()
        );
    }
}
