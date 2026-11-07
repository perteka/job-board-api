package com.jobboard.api.service;

import com.jobboard.api.dto.request.JobSeekerProfileRequest;
import com.jobboard.api.dto.response.JobSeekerProfileResponse;
import com.jobboard.api.entity.JobSeekerProfile;
import com.jobboard.api.entity.User;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.mapper.JobSeekerProfileMapper;
import com.jobboard.api.repository.JobSeekerProfileRepository;
import com.jobboard.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobSeekerProfileService {

    private final JobSeekerProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final JobSeekerProfileMapper profileMapper;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public JobSeekerProfileResponse get(Long userId) {
        return profileMapper.toResponse(findByUserId(userId));
    }

    @Transactional
    public JobSeekerProfileResponse update(Long userId, JobSeekerProfileRequest request) {
        if (request.cvUrl() != null && !request.cvUrl().isBlank()) {
            fileStorageService.validateObjectKeyForUser(request.cvUrl().trim(), userId);
        }
        JobSeekerProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createProfile(userId));

        profile.setHeadline(request.headline());
        profile.setSummary(request.summary());
        profile.setExperienceYears(request.experienceYears());
        profile.setCvUrl(request.cvUrl() == null || request.cvUrl().isBlank()
                ? null : request.cvUrl().trim());

        return profileMapper.toResponse(profile);
    }

    private JobSeekerProfile findByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Aday profili bulunamadı"));
    }

    private JobSeekerProfile createProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
        JobSeekerProfile profile = new JobSeekerProfile();
        profile.setUser(user);
        return profileRepository.save(profile);
    }
}
