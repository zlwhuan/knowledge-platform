package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.RolePermissionRequest;
import com.company.knowledge.dto.RolePermissionResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RolePermissionController {

    private final AuthService authService;

    public RolePermissionController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<RolePermissionResponse>> list(@RequestHeader("X-Auth-Token") String token) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok(authService.listRolePermissions());
    }

    @GetMapping("/current")
    public ApiResponse<RolePermissionResponse> current(@RequestHeader("X-Auth-Token") String token) {
        UserAccount user = authService.requireUser(token);
        return ApiResponse.ok(authService.getRolePermissions(user.getRole()));
    }

    @PutMapping("/{role}")
    public ApiResponse<RolePermissionResponse> update(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable RoleType role,
            @Valid @RequestBody RolePermissionRequest request
    ) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok(authService.updateRolePermissions(role, request));
    }
}
