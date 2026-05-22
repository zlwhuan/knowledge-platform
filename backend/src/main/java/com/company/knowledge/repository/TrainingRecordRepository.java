package com.company.knowledge.repository;

import com.company.knowledge.entity.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long>, JpaSpecificationExecutor<TrainingRecord> {

    List<TrainingRecord> findByTitleContainingIgnoreCase(String title);

    List<TrainingRecord> findByTrainingType(String trainingType);

    List<TrainingRecord> findByCreatedBy(String createdBy);
}
