package com.company.knowledge.dto;

public record DictionaryItemResponse(
        Long id,
        String dictType,
        String itemValue,
        String itemLabel,
        String remark,
        Integer sortOrder,
        Boolean enabled
) {
}
