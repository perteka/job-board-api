package com.jobboard.api.entity;

import com.jobboard.api.entity.enums.EmploymentType;
import com.jobboard.api.entity.enums.JobStatus;
import com.jobboard.api.entity.enums.WorkMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_postings", indexes = {
        @Index(name = "idx_job_postings_status", columnList = "status"),
        @Index(name = "idx_job_postings_location", columnList = "location"),
        @Index(name = "idx_job_postings_company", columnList = "company_id")
})
public class JobPosting extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "location", nullable = false, length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 20)
    private WorkMode workMode;

    @Column(name = "min_salary", precision = 12, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "max_salary", precision = 12, scale = 2)
    private BigDecimal maxSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @BatchSize(size = 50)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "job_posting_skills",
            joinColumns = @JoinColumn(name = "job_posting_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new LinkedHashSet<>();
}
