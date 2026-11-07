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
@Table(name = "categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categories_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug")
})
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;
}
