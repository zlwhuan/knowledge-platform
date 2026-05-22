package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.CategoryRequest;
import com.company.knowledge.dto.CategoryResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AuthService;
import com.company.knowledge.service.CategoryService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthService authService;

    public CategoryController(CategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.ok(categoryService.list());
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@RequestHeader("X-Auth-Token") String token, @Valid @RequestBody CategoryRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok("创建成功", categoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok("更新成功", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        categoryService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
