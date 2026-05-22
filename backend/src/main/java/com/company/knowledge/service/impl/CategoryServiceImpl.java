package com.company.knowledge.service.impl;

import com.company.knowledge.dto.CategoryRequest;
import com.company.knowledge.dto.CategoryResponse;
import com.company.knowledge.entity.Category;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.CategoryRepository;
import com.company.knowledge.service.CategoryService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryResponse> list() {
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        Map<Long, List<Category>> childrenMap = new HashMap<>();
        List<Category> roots = new ArrayList<>();
        for (Category category : categories) {
            Long parentId = category.getParent() == null ? null : category.getParent().getId();
            if (parentId == null) {
                roots.add(category);
            } else {
                childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(category);
            }
        }
        Comparator<Category> comparator = Comparator.comparing(Category::getSortOrder).thenComparing(Category::getName);
        roots.sort(comparator);
        childrenMap.values().forEach(list -> list.sort(comparator));
        return roots.stream().map(category -> toResponse(category, childrenMap)).toList();
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        apply(category, request);
        return toResponse(categoryRepository.save(category), new HashMap<>());
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));
        apply(category, request);
        return toResponse(categoryRepository.save(category), new HashMap<>());
    }

    @Override
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));
        if (categoryRepository.existsByParentId(id)) {
            throw new IllegalArgumentException("请先删除下级分类");
        }
        categoryRepository.delete(category);
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setParent(request.getParentId() == null ? null : categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("上级分类不存在")));
        category.setUpdatedAt(LocalDateTime.now());
    }

    private CategoryResponse toResponse(Category category, Map<Long, List<Category>> childrenMap) {
        List<CategoryResponse> children = childrenMap.getOrDefault(category.getId(), List.of()).stream()
                .map(child -> toResponse(child, childrenMap))
                .toList();
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getSortOrder(),
                children
        );
    }
}
