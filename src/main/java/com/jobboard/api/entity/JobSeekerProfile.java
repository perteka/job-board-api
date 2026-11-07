package com.jobboard.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_seeker_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_seeker_profiles_user", columnNames = "user_id")
})
public class JobSeekerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "headline", length = 150)
    private String headline;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "cv_url", length = 500)
    private String cvUrl;
}
