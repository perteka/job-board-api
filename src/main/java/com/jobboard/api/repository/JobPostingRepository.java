package com.jobboard.api.repository;

import com.jobboard.api.entity.JobPosting;
import com.jobboard.api.entity.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    @EntityGraph(attributePaths = {"company", "category"})
    Page<JobPosting> findByCreatedById(Long createdById, Pageable pageable);

    @Query("""
            select j from JobPosting j
            where j.status = :status
              and (j.expiresAt is null or j.expiresAt > :now)
              and (lower(j.title) like lower(concat('%', :keyword, '%'))
                   or lower(j.description) like lower(concat('%', :keyword, '%')))
              and lower(j.location) like lower(concat('%', :city, '%'))
            """)
    @EntityGraph(attributePaths = {"company", "category"})
    Page<JobPosting> search(
            @Param("status") JobStatus status,
            @Param("keyword") String keyword,
            @Param("city") String city,
            @Param("now") Instant now,
            Pageable pageable
    );
}
