package com.company.knowledge.repository;

import com.company.knowledge.entity.ProjectActivity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectActivityRepository extends JpaRepository<ProjectActivity, Long> {
    List<ProjectActivity> findAllByProjectIdOrderByRecordTimeDesc(Long projectId);
    void deleteAllByProjectId(Long projectId);
}
