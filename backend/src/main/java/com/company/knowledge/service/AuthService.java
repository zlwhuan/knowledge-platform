package com.company.knowledge.service;

import com.company.knowledge.dto.LoginResponse;
import com.company.knowledge.dto.RolePermissionRequest;
import com.company.knowledge.dto.RolePermissionResponse;
import com.company.knowledge.dto.UserRequest;
import com.company.knowledge.dto.UserResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import java.util.List;

public interface AuthService {
    LoginResponse login(String username, String password);
    UserAccount requireUser(String token);
    void requireAnyRole(UserAccount user, RoleType... roles);
    List<UserResponse> listUsers();
    UserResponse createUser(UserRequest request);
    UserResponse updateUser(Long id, UserRequest request);
    void deleteUser(Long id);
    List<RolePermissionResponse> listRolePermissions();
    RolePermissionResponse getRolePermissions(RoleType role);
    RolePermissionResponse updateRolePermissions(RoleType role, RolePermissionRequest request);
}
