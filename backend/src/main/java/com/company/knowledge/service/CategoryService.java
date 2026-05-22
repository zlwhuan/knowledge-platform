package com.company.knowledge.service;

import com.company.knowledge.dto.CategoryRequest;
import com.company.knowledge.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> list();
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    void delete(Long id);
}
