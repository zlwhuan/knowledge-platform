package com.company.knowledge.service;

import com.company.knowledge.dto.DictionaryGroupResponse;
import com.company.knowledge.dto.DictionaryItemRequest;
import com.company.knowledge.dto.DictionaryItemResponse;
import java.util.List;

public interface DictionaryService {
    List<DictionaryGroupResponse> listAll();
    List<DictionaryItemResponse> listByType(String dictType, boolean includeDisabled);
    DictionaryItemResponse create(DictionaryItemRequest request);
    DictionaryItemResponse update(Long id, DictionaryItemRequest request);
    void delete(Long id);
}
