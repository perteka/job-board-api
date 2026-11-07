package com.jobboard.api.mapper;

import com.jobboard.api.dto.response.CategoryResponse;
import com.jobboard.api.dto.response.SkillResponse;
import com.jobboard.api.entity.Category;
import com.jobboard.api.entity.Skill;
import org.springframework.stereotype.Component;

@Component
public class TaxonomyMapper {

    public CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }

    public SkillResponse toSkillResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getSlug());
    }
}
