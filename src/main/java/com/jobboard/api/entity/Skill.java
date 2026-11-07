package com.jobboard.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "skills", uniqueConstraints = {
        @UniqueConstraint(name = "uk_skills_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_skills_slug", columnNames = "slug")
})
public class Skill extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;
}
