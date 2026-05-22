package com.company.knowledge.dto;

import com.company.knowledge.entity.RoleType;
import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String displayName, RoleType role, Boolean enabled, LocalDateTime createdAt) {
}
