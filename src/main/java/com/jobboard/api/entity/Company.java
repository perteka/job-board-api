package com.jobboard.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "companies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_companies_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_companies_owner", columnNames = "owner_user_id")
})
public class Company extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;
}
