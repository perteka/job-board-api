package com.jobboard.api.service;

import com.jobboard.api.entity.Category;
import com.jobboard.api.entity.Skill;
import com.jobboard.api.mapper.TaxonomyMapper;
import com.jobboard.api.repository.CategoryRepository;
import com.jobboard.api.repository.SkillRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TaxonomyServiceTest {
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final SkillRepository skills = mock(SkillRepository.class);
    private final TaxonomyMapper mapper = mock(TaxonomyMapper.class);
    private final TaxonomyService service = new TaxonomyService(categories, skills, mapper);

    @Test
    void listCategoriesUsesOrderedRepositoryAndMapsItems() {
        Category category = new Category();
        when(categories.findAllByOrderByNameAsc()).thenReturn(List.of(category));
        when(mapper.toCategoryResponse(category)).thenReturn(null);
        assertThat(service.listCategories()).containsExactly((com.jobboard.api.dto.response.CategoryResponse) null);
        verify(mapper).toCategoryResponse(category);
    }

    @Test
    void listSkillsUsesOrderedRepositoryAndMapsItems() {
        Skill skill = new Skill();
        when(skills.findAllByOrderByNameAsc()).thenReturn(List.of(skill));
        when(mapper.toSkillResponse(skill)).thenReturn(null);
        assertThat(service.listSkills()).containsExactly((com.jobboard.api.dto.response.SkillResponse) null);
        verify(mapper).toSkillResponse(skill);
    }
}
