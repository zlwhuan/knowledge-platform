package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.BulkIdsRequest;
import com.company.knowledge.dto.DashboardStatsResponse;
import com.company.knowledge.dto.KnowledgeItemRequest;
import com.company.knowledge.dto.KnowledgeItemResponse;
import com.company.knowledge.dto.KnowledgeItemVersionResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AuthService;
import com.company.knowledge.service.KnowledgeItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/items")
public class KnowledgeItemController {

    private final KnowledgeItemService knowledgeItemService;
    private final AuthService authService;

    public KnowledgeItemController(KnowledgeItemService knowledgeItemService, AuthService authService) {
        this.knowledgeItemService = knowledgeItemService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long projectId
    ) {
        return ApiResponse.ok(knowledgeItemService.list(keyword, categoryId, type, projectId));
    }

    @GetMapping("/paged")
    public ApiResponse<?> listPaged(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(knowledgeItemService.listPaged(keyword, categoryId, type, projectId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeItemResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeItemService.get(id));
    }

    @PostMapping
    public ApiResponse<KnowledgeItemResponse> create(@RequestHeader("X-Auth-Token") String token, @Valid @RequestBody KnowledgeItemRequest request) {
        UserAccount user = authService.requireUser(token);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("创建成功", knowledgeItemService.create(request, operatorName));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeItemResponse> update(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @Valid @RequestBody KnowledgeItemRequest request) {
        UserAccount user = authService.requireUser(token);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("更新成功", knowledgeItemService.update(id, request, operatorName));
    }

    @PostMapping("/bulk/delete")
    public ApiResponse<Void> bulkDelete(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody BulkIdsRequest request
    ) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS);
        knowledgeItemService.bulkDelete(request.getIds());
        return ApiResponse.ok("批量删除成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS);
        knowledgeItemService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importItems(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam("file") MultipartFile file
    ) {
        UserAccount user = authService.requireUser(token);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("导入完成", knowledgeItemService.importFromExcel(file, operatorName));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<KnowledgeItemVersionResponse>> getVersions(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeItemService.getVersions(id));
    }

    @GetMapping("/dashboard/stats")
    public ApiResponse<DashboardStatsResponse> dashboard() {
        return ApiResponse.ok(knowledgeItemService.dashboard());
    }
}
