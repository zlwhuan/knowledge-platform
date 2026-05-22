package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.TrainingRecordRequest;
import com.company.knowledge.dto.TrainingRecordResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AuthService;
import com.company.knowledge.service.TrainingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-records")
public class TrainingController {

    private final TrainingService trainingService;
    private final AuthService authService;

    public TrainingController(TrainingService trainingService, AuthService authService) {
        this.trainingService = trainingService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<Page<TrainingRecordResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trainingType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(trainingService.list(keyword, trainingType, page, size));
    }

    @GetMapping("/all")
    public ApiResponse<List<TrainingRecordResponse>> listAll() {
        return ApiResponse.ok(trainingService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<TrainingRecordResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(trainingService.get(id));
    }

    @PostMapping
    public ApiResponse<TrainingRecordResponse> create(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody TrainingRecordRequest request
    ) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("创建成功", trainingService.create(request, operatorName));
    }

    @PutMapping("/{id}")
    public ApiResponse<TrainingRecordResponse> update(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody TrainingRecordRequest request
    ) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("更新成功", trainingService.update(id, request, operatorName));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id
    ) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        trainingService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
