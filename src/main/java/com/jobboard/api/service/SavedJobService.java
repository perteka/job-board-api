package com.jobboard.api.service;

import com.jobboard.api.dto.response.PageResponse;
import com.jobboard.api.dto.response.SavedJobResponse;
import com.jobboard.api.entity.JobPosting;
import com.jobboard.api.entity.SavedJob;
import com.jobboard.api.entity.SavedJobId;
import com.jobboard.api.entity.User;
import com.jobboard.api.entity.enums.JobStatus;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.ConflictException;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.mapper.SavedJobMapper;
import com.jobboard.api.repository.JobPostingRepository;
import com.jobboard.api.repository.SavedJobRepository;
import com.jobboard.api.repository.UserRepository;
import com.jobboard.api.util.PageableSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final SavedJobMapper savedJobMapper;

    @Transactional
    public SavedJobResponse save(Long jobId, Long userId) {
        JobPosting jobPosting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("İlan bulunamadı"));

        if (jobPosting.getStatus() != JobStatus.PUBLISHED) {
            throw new BadRequestException("Yalnızca yayındaki ilanlar kaydedilebilir");
        }
        if (jobPosting.getExpiresAt() != null && !jobPosting.getExpiresAt().isAfter(java.time.Instant.now())) {
            throw new BadRequestException("Süresi dolmuş ilan kaydedilemez");
        }

        SavedJobId id = new SavedJobId(userId, jobId);
        if (savedJobRepository.existsById(id)) {
            throw new ConflictException("Bu ilan zaten kaydedilmiş");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        SavedJob savedJob = new SavedJob();
        savedJob.setId(id);
        savedJob.setUser(user);
        savedJob.setJobPosting(jobPosting);

        return savedJobMapper.toResponse(savedJobRepository.save(savedJob));
    }

    @Transactional
    public void remove(Long jobId, Long userId) {
        SavedJobId id = new SavedJobId(userId, jobId);
        if (!savedJobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Kaydedilen ilan bulunamadı");
        }
        savedJobRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<SavedJobResponse> list(Long userId, Pageable pageable) {
        Pageable safePageable = PageableSupport.requireAllowedSort(
                pageable, java.util.Set.of("createdAt"));
        return PageResponse.from(savedJobRepository
                .findByIdUserId(userId, safePageable)
                .map(savedJobMapper::toResponse));
    }
}
