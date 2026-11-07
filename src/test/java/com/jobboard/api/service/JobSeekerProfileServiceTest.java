package com.jobboard.api.service;

import com.jobboard.api.dto.request.JobSeekerProfileRequest;
import com.jobboard.api.dto.response.JobSeekerProfileResponse;
import com.jobboard.api.entity.JobSeekerProfile;
import com.jobboard.api.entity.User;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.mapper.JobSeekerProfileMapper;
import com.jobboard.api.repository.JobSeekerProfileRepository;
import com.jobboard.api.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JobSeekerProfileServiceTest {
    private final JobSeekerProfileRepository profiles = mock(JobSeekerProfileRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final JobSeekerProfileMapper mapper = mock(JobSeekerProfileMapper.class);
    private final FileStorageService files = mock(FileStorageService.class);
    private final JobSeekerProfileService service = new JobSeekerProfileService(profiles, users, mapper, files);

    @Test
    void updateValidatesCvAndUpdatesExistingProfile() {
        User user = new User(); user.setId(7L);
        JobSeekerProfile profile = new JobSeekerProfile(); profile.setUser(user);
        when(profiles.findByUserId(7L)).thenReturn(Optional.of(profile));
        when(mapper.toResponse(profile)).thenReturn(new JobSeekerProfileResponse(
                1L, 7L, "Backend", "Summary", 4, "cvs/7/resume.pdf", Instant.now(), Instant.now()));

        JobSeekerProfileResponse result = service.update(7L,
                new JobSeekerProfileRequest("Backend", "Summary", 4, " cvs/7/resume.pdf "));

        assertThat(result.cvUrl()).isEqualTo("cvs/7/resume.pdf");
        assertThat(profile.getCvUrl()).isEqualTo("cvs/7/resume.pdf");
        verify(files).validateObjectKeyForUser("cvs/7/resume.pdf", 7L);
    }

    @Test
    void updateRejectsCvOwnedByAnotherUser() {
        doThrow(new BadRequestException("Geçersiz CV anahtarı"))
                .when(files).validateObjectKeyForUser("cvs/9/resume.pdf", 7L);

        assertThatThrownBy(() -> service.update(7L,
                new JobSeekerProfileRequest(null, null, null, "cvs/9/resume.pdf")))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(profiles, users);
    }

    @Test
    void getMissingProfileReturnsNotFound() {
        when(profiles.findByUserId(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
