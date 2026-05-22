package com.company.knowledge.repository;

import com.company.knowledge.entity.ExamQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    Page<ExamQuestion> findByCategoryAndTypeAndEnabledTrue(String category, String type, Pageable pageable);
    Page<ExamQuestion> findByCategoryAndEnabledTrue(String category, Pageable pageable);
    List<ExamQuestion> findByCategoryAndTypeAndDifficultyAndEnabledTrue(String category, String type, String difficulty);
    
    @Query("SELECT q FROM ExamQuestion q WHERE q.enabled = true AND (:category IS NULL OR q.category = :category) AND (:type IS NULL OR q.type = :type)")
    Page<ExamQuestion> findAll(@Param("category") String category, @Param("type") String type, Pageable pageable);
    
    long countByCategory(String category);
    long countByEnabledTrue();
}
