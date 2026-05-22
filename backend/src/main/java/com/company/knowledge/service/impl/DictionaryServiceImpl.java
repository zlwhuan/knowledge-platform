package com.company.knowledge.service.impl;

import com.company.knowledge.dto.DictionaryGroupResponse;
import com.company.knowledge.dto.DictionaryItemRequest;
import com.company.knowledge.dto.DictionaryItemResponse;
import com.company.knowledge.entity.DictionaryItem;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.DictionaryItemRepository;
import com.company.knowledge.service.DictionaryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DictionaryServiceImpl implements DictionaryService {

    private final DictionaryItemRepository dictionaryItemRepository;

    public DictionaryServiceImpl(DictionaryItemRepository dictionaryItemRepository) {
        this.dictionaryItemRepository = dictionaryItemRepository;
    }

    @Override
    public List<DictionaryGroupResponse> listAll() {
        Map<String, List<DictionaryItemResponse>> grouped = dictionaryItemRepository.findAllByOrderByDictTypeAscSortOrderAscIdAsc().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(this::toResponse)
                .collect(Collectors.groupingBy(DictionaryItemResponse::dictType));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DictionaryGroupResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<DictionaryItemResponse> listByType(String dictType, boolean includeDisabled) {
        return dictionaryItemRepository.findByDictTypeOrderBySortOrderAscIdAsc(dictType).stream()
                .filter(item -> includeDisabled || Boolean.TRUE.equals(item.getEnabled()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DictionaryItemResponse create(DictionaryItemRequest request) {
        validateDuplicate(null, request.getDictType(), request.getItemValue());
        DictionaryItem item = new DictionaryItem();
        apply(item, request);
        return toResponse(dictionaryItemRepository.save(item));
    }

    @Override
    public DictionaryItemResponse update(Long id, DictionaryItemRequest request) {
        DictionaryItem item = dictionaryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("字典项不存在"));
        validateDuplicate(id, request.getDictType(), request.getItemValue());
        apply(item, request);
        return toResponse(dictionaryItemRepository.save(item));
    }

    @Override
    public void delete(Long id) {
        DictionaryItem item = dictionaryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("字典项不存在"));
        dictionaryItemRepository.delete(item);
    }

    private void validateDuplicate(Long id, String dictType, String itemValue) {
        boolean exists = id == null
                ? dictionaryItemRepository.existsByDictTypeAndItemValue(dictType, itemValue)
                : dictionaryItemRepository.existsByDictTypeAndItemValueAndIdNot(dictType, itemValue, id);
        if (exists) throw new IllegalArgumentException("同一字典类型下的字典值不能重复");
    }

    private void apply(DictionaryItem item, DictionaryItemRequest request) {
        item.setDictType(request.getDictType().trim());
        item.setItemValue(request.getItemValue().trim());
        item.setItemLabel(request.getItemLabel().trim());
        item.setRemark(request.getRemark() == null ? null : request.getRemark().trim());
        item.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        item.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
        item.setUpdatedAt(LocalDateTime.now());
    }

    private DictionaryItemResponse toResponse(DictionaryItem item) {
        return new DictionaryItemResponse(
                item.getId(),
                item.getDictType(),
                item.getItemValue(),
                item.getItemLabel(),
                item.getRemark(),
                item.getSortOrder(),
                item.getEnabled()
        );
    }
}
