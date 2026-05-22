package com.company.knowledge.repository;

import com.company.knowledge.entity.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {
    List<ExamPaper> findByEnabledTrueOrderByCreatedAtDesc();
    List<ExamPaper> findByCategoryAndEnabledTrueOrderByCreatedAtDesc(String category);
    long countByEnabledTrue();
}
