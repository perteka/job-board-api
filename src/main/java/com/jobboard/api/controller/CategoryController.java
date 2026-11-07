package com.jobboard.api.controller;

import com.jobboard.api.dto.response.CategoryResponse;
import com.jobboard.api.service.TaxonomyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final TaxonomyService taxonomyService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(taxonomyService.listCategories());
    }
}
