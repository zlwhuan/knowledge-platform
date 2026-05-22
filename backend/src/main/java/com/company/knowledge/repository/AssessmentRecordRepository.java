package com.company.knowledge.repository;

import com.company.knowledge.entity.AssessmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentRecordRepository extends JpaRepository<AssessmentRecord, Long>, JpaSpecificationExecutor<AssessmentRecord> {

    List<AssessmentRecord> findByTitleContainingIgnoreCase(String title);

    List<AssessmentRecord> findByAssessmentType(String assessmentType);

    List<AssessmentRecord> findByGrade(String grade);

    List<AssessmentRecord> findByTrainingRecordId(Long trainingRecordId);

    List<AssessmentRecord> findByCreatedBy(String createdBy);
}
