package com.jobboard.api.repository;

import com.jobboard.api.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findAllByOrderByNameAsc();

    Optional<Skill> findBySlug(String slug);
}
