package com.company.knowledge.repository;

import com.company.knowledge.entity.KnowledgeItemVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface KnowledgeItemVersionRepository extends JpaRepository<KnowledgeItemVersion, Long> {
    List<KnowledgeItemVersion> findByItemIdOrderByCreatedAtDesc(Long itemId);
    
    @Transactional
    void deleteByItemId(Long itemId);
}
