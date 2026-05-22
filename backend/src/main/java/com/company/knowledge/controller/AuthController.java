package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.LoginRequest;
import com.company.knowledge.dto.LoginResponse;
import com.company.knowledge.dto.UserResponse;
import com.company.knowledge.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@RequestHeader("X-Auth-Token") String token) {
        var user = authService.requireUser(token);
        return ApiResponse.ok(new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getEnabled(), user.getCreatedAt()));
    }
}
