package com.company.knowledge.repository;

import com.company.knowledge.entity.ExamRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamRecordRepository extends JpaRepository<ExamRecord, Long> {
    List<ExamRecord> findByUsernameOrderByCreatedAtDesc(String username);
    List<ExamRecord> findByPaperIdOrderByCreatedAtDesc(Long paperId);
    Optional<ExamRecord> findByPaperIdAndUsernameAndStatus(Long paperId, String username, String status);
    long countByUsernameAndPassed(String username, Boolean passed);
    long countByPaperIdAndPassed(Long paperId, Boolean passed);
}
