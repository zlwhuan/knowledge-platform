package com.company.knowledge.repository;

import com.company.knowledge.entity.Attachment;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByItemIdOrderByUploadedAtDesc(Long itemId);
    long countByItemId(Long itemId);
    
    @Query("SELECT a FROM Attachment a LEFT JOIN FETCH a.item i LEFT JOIN FETCH i.category LEFT JOIN FETCH i.project WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(a.originalFileName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (:contentType IS NULL OR :contentType = '' OR LOWER(a.contentType) LIKE LOWER(CONCAT('%', :contentType, '%'))) ORDER BY a.uploadedAt DESC")
    Page<Attachment> findAll(@Param("keyword") String keyword, @Param("contentType") String contentType, Pageable pageable);
}
