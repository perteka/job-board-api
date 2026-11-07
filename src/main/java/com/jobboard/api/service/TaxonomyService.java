package com.jobboard.api.service;

import com.jobboard.api.dto.response.CategoryResponse;
import com.jobboard.api.dto.response.SkillResponse;
import com.jobboard.api.mapper.TaxonomyMapper;
import com.jobboard.api.repository.CategoryRepository;
import com.jobboard.api.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxonomyService {

    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final TaxonomyMapper taxonomyMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(taxonomyMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listSkills() {
        return skillRepository.findAllByOrderByNameAsc().stream()
                .map(taxonomyMapper::toSkillResponse)
                .toList();
    }
}
