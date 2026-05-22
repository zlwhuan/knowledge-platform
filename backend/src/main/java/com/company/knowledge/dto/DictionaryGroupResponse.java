package com.company.knowledge.dto;

import java.util.List;

public record DictionaryGroupResponse(
        String dictType,
        List<DictionaryItemResponse> items
) {
}
