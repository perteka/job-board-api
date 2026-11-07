package com.jobboard.api.repository;

import com.jobboard.api.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobPostingIdAndJobSeekerId(Long jobPostingId, Long jobSeekerId);

    @EntityGraph(attributePaths = {"jobPosting", "jobSeeker"})
    Page<Application> findByJobSeekerId(Long jobSeekerId, Pageable pageable);

    @EntityGraph(attributePaths = {"jobPosting", "jobSeeker"})
    Page<Application> findByJobPostingId(Long jobPostingId, Pageable pageable);
}
