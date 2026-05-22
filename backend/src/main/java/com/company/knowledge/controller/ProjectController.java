package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.ProjectActivityRequest;
import com.company.knowledge.dto.ProjectActivityResponse;
import com.company.knowledge.dto.ProjectDashboardResponse;
import com.company.knowledge.dto.ProjectProgressRecordRequest;
import com.company.knowledge.dto.ProjectProgressRecordResponse;
import com.company.knowledge.dto.ProjectRequest;
import com.company.knowledge.dto.ProjectResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AuthService;
import com.company.knowledge.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final AuthService authService;

    public ProjectController(ProjectService projectService, AuthService authService) {
        this.projectService = projectService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String riskLevel
    ) {
        return ApiResponse.ok(projectService.list(keyword, customerName, stage, status, owner, riskLevel));
    }

    @GetMapping("/dashboard")
    public ApiResponse<ProjectDashboardResponse> dashboard() {
        return ApiResponse.ok(projectService.dashboard());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(projectService.get(id));
    }

    @PostMapping
    public ApiResponse<ProjectResponse> create(@RequestHeader("X-Auth-Token") String token, @Valid @RequestBody ProjectRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("项目已创建", projectService.create(request, operatorName));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectResponse> update(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("项目已更新", projectService.update(id, request, operatorName));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        projectService.delete(id);
        return ApiResponse.ok("项目已删除", null);
    }

    @PostMapping("/{id}/progress-records")
    public ApiResponse<ProjectProgressRecordResponse> addProgressRecord(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @Valid @RequestBody ProjectProgressRecordRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("项目进度已记录", projectService.addProgressRecord(id, request, operatorName));
    }

    @PutMapping("/progress-records/{recordId}")
    public ApiResponse<ProjectProgressRecordResponse> updateProgressRecord(@RequestHeader("X-Auth-Token") String token, @PathVariable Long recordId, @Valid @RequestBody ProjectProgressRecordRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("项目进度已更新", projectService.updateProgressRecord(recordId, request, operatorName));
    }

    @DeleteMapping("/progress-records/{recordId}")
    public ApiResponse<Void> deleteProgressRecord(@RequestHeader("X-Auth-Token") String token, @PathVariable Long recordId) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        projectService.deleteProgressRecord(recordId);
        return ApiResponse.ok("项目进度已删除", null);
    }

    @PostMapping("/{id}/activities")
    public ApiResponse<ProjectActivityResponse> addActivity(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @Valid @RequestBody ProjectActivityRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("项目记录已添加", projectService.addActivity(id, request, operatorName));
    }

    @PutMapping("/activities/{activityId}")
    public ApiResponse<ProjectActivityResponse> updateActivity(@RequestHeader("X-Auth-Token") String token, @PathVariable Long activityId, @Valid @RequestBody ProjectActivityRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("项目记录已更新", projectService.updateActivity(activityId, request, operatorName));
    }

    @DeleteMapping("/activities/{activityId}")
    public ApiResponse<Void> deleteActivity(@RequestHeader("X-Auth-Token") String token, @PathVariable Long activityId) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        projectService.deleteActivity(activityId);
        return ApiResponse.ok("项目记录已删除", null);
    }
}
