package com.company.knowledge.repository;

import com.company.knowledge.entity.DictionaryItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryItemRepository extends JpaRepository<DictionaryItem, Long> {
    List<DictionaryItem> findAllByOrderByDictTypeAscSortOrderAscIdAsc();
    List<DictionaryItem> findByDictTypeOrderBySortOrderAscIdAsc(String dictType);
    long countByDictType(String dictType);
    boolean existsByDictTypeAndItemValueAndIdNot(String dictType, String itemValue, Long id);
    boolean existsByDictTypeAndItemValue(String dictType, String itemValue);
}
