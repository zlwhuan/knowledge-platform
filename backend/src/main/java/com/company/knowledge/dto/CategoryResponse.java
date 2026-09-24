package com.company.knowledge.dto;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        String code,
        String description,
        Long parentId,
        Integer sortOrder,
        String path,
        List<CategoryResponse> children
) {
}
