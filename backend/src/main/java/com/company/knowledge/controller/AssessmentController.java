package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.AssessmentRecordRequest;
import com.company.knowledge.dto.AssessmentRecordResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AssessmentService;
import com.company.knowledge.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessment-records")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AuthService authService;

    public AssessmentController(AssessmentService assessmentService, AuthService authService) {
        this.assessmentService = assessmentService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<Page<AssessmentRecordResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String assessmentType,
            @RequestParam(required = false) String grade,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(assessmentService.list(keyword, assessmentType, grade, page, size));
    }

    @GetMapping("/all")
    public ApiResponse<List<AssessmentRecordResponse>> listAll() {
        return ApiResponse.ok(assessmentService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<AssessmentRecordResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(assessmentService.get(id));
    }

    @GetMapping("/by-training/{trainingRecordId}")
    public ApiResponse<List<AssessmentRecordResponse>> listByTrainingRecordId(@PathVariable Long trainingRecordId) {
        return ApiResponse.ok(assessmentService.listByTrainingRecordId(trainingRecordId));
    }

    @PostMapping
    public ApiResponse<AssessmentRecordResponse> create(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody AssessmentRecordRequest request
    ) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("创建成功", assessmentService.create(request, operatorName));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssessmentRecordResponse> update(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody AssessmentRecordRequest request
    ) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("更新成功", assessmentService.update(id, request, operatorName));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id
    ) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        assessmentService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
