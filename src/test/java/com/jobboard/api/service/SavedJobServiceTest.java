package com.jobboard.api.service;

import com.jobboard.api.dto.response.SavedJobResponse;
import com.jobboard.api.entity.JobPosting;
import com.jobboard.api.entity.SavedJob;
import com.jobboard.api.entity.User;
import com.jobboard.api.entity.enums.JobStatus;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.ConflictException;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.mapper.SavedJobMapper;
import com.jobboard.api.repository.JobPostingRepository;
import com.jobboard.api.repository.SavedJobRepository;
import com.jobboard.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SavedJobServiceTest {
    private final SavedJobRepository savedJobs = mock(SavedJobRepository.class);
    private final JobPostingRepository jobs = mock(JobPostingRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SavedJobMapper mapper = mock(SavedJobMapper.class);
    private final SavedJobService service = new SavedJobService(savedJobs, jobs, users, mapper);
    private JobPosting job;

    @BeforeEach
    void setUp() {
        job = new JobPosting();
        job.setId(3L);
        job.setStatus(JobStatus.PUBLISHED);
        job.setExpiresAt(Instant.now().plusSeconds(3600));
    }

    @Test
    void save_persistsPublishedNonExpiredJob() {
        User user = new User(); user.setId(8L);
        when(jobs.findById(3L)).thenReturn(Optional.of(job));
        when(savedJobs.existsById(any())).thenReturn(false);
        when(users.findById(8L)).thenReturn(Optional.of(user));
        when(savedJobs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SavedJobResponse response = new SavedJobResponse(3L, "Engineer", "Acme", "Remote", JobStatus.PUBLISHED, Instant.now());
        when(mapper.toResponse(any())).thenReturn(response);

        assertThat(service.save(3L, 8L)).isEqualTo(response);
        verify(savedJobs).save(argThat(saved -> saved.getId().getUserId().equals(8L)
                && saved.getId().getJobPostingId().equals(3L)));
    }

    @Test
    void save_rejectsExpiredJob() {
        job.setExpiresAt(Instant.now().minusSeconds(1));
        when(jobs.findById(3L)).thenReturn(Optional.of(job));
        assertThatThrownBy(() -> service.save(3L, 8L)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(savedJobs, users);
    }

    @Test
    void save_rejectsUnpublishedJob() {
        job.setStatus(JobStatus.DRAFT);
        when(jobs.findById(3L)).thenReturn(Optional.of(job));
        assertThatThrownBy(() -> service.save(3L, 8L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void save_rejectsDuplicate() {
        when(jobs.findById(3L)).thenReturn(Optional.of(job));
        when(savedJobs.existsById(any())).thenReturn(true);
        assertThatThrownBy(() -> service.save(3L, 8L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void remove_missingSavedJobReturnsNotFound() {
        when(savedJobs.existsById(any())).thenReturn(false);
        assertThatThrownBy(() -> service.remove(3L, 8L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
