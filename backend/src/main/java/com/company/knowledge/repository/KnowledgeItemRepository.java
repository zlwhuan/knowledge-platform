package com.company.knowledge.repository;

import com.company.knowledge.entity.KnowledgeItem;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long>, JpaSpecificationExecutor<KnowledgeItem> {
    long countByContentMarkdownIsNotNullAndContentMarkdownNot(String empty);
    long countBySummaryIsNull();
    long countByUpdatedAtAfter(LocalDateTime threshold);
    @Query("SELECT COUNT(k) FROM KnowledgeItem k WHERE k.id NOT IN (SELECT DISTINCT a.item.id FROM Attachment a)")
    long countItemsWithoutAttachment();
}
