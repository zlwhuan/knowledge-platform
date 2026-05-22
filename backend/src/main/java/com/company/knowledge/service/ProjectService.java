package com.company.knowledge.service;

import com.company.knowledge.dto.ProjectActivityRequest;
import com.company.knowledge.dto.ProjectActivityResponse;
import com.company.knowledge.dto.ProjectDashboardResponse;
import com.company.knowledge.dto.ProjectProgressRecordRequest;
import com.company.knowledge.dto.ProjectProgressRecordResponse;
import com.company.knowledge.dto.ProjectRequest;
import com.company.knowledge.dto.ProjectResponse;
import java.util.List;

public interface ProjectService {
    List<ProjectResponse> list(String keyword, String customerName, String stage, String status, String owner, String riskLevel);
    ProjectDashboardResponse dashboard();
    ProjectResponse get(Long id);
    ProjectResponse create(ProjectRequest request, String operatorName);
    ProjectResponse update(Long id, ProjectRequest request, String operatorName);
    void delete(Long id);
    ProjectProgressRecordResponse addProgressRecord(Long projectId, ProjectProgressRecordRequest request, String operatorName);
    ProjectProgressRecordResponse updateProgressRecord(Long recordId, ProjectProgressRecordRequest request, String operatorName);
    void deleteProgressRecord(Long recordId);
    ProjectActivityResponse addActivity(Long projectId, ProjectActivityRequest request, String operatorName);
    ProjectActivityResponse updateActivity(Long activityId, ProjectActivityRequest request, String operatorName);
    void deleteActivity(Long activityId);
}
