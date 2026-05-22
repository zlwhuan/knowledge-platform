package com.company.knowledge.repository;

import com.company.knowledge.entity.ProjectProgressRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectProgressRecordRepository extends JpaRepository<ProjectProgressRecord, Long> {
    List<ProjectProgressRecord> findAllByProjectIdOrderByRecordTimeDesc(Long projectId);
    ProjectProgressRecord findFirstByProjectIdOrderByRecordTimeDescIdDesc(Long projectId);
    void deleteAllByProjectId(Long projectId);
}
