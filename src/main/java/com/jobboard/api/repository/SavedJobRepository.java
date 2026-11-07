package com.jobboard.api.repository;

import com.jobboard.api.entity.SavedJob;
import com.jobboard.api.entity.SavedJobId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface SavedJobRepository extends JpaRepository<SavedJob, SavedJobId> {

    @EntityGraph(attributePaths = {"jobPosting", "jobPosting.company"})
    Page<SavedJob> findByIdUserId(Long userId, Pageable pageable);

    boolean existsByIdUserIdAndIdJobPostingId(Long userId, Long jobPostingId);
}
