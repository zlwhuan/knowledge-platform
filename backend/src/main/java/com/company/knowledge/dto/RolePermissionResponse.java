package com.company.knowledge.dto;

import com.company.knowledge.entity.RoleType;
import java.time.LocalDateTime;

public record RolePermissionResponse(
        Long id,
        RoleType role,
        Boolean canViewLibrary,
        Boolean canCreateContent,
        Boolean canEditContent,
        Boolean canDeleteContent,
        Boolean canManageCategories,
        Boolean canManageUsers,
        Boolean canManageRoles,
        Boolean canPreviewOffice,
        LocalDateTime updatedAt
) {
}
