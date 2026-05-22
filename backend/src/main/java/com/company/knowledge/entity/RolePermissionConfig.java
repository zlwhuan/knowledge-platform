package com.company.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "kp_role_permission")
public class RolePermissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleType role;

    @Column(nullable = false)
    private Boolean canViewLibrary = true;

    @Column(nullable = false)
    private Boolean canCreateContent = false;

    @Column(nullable = false)
    private Boolean canEditContent = false;

    @Column(nullable = false)
    private Boolean canDeleteContent = false;

    @Column(nullable = false)
    private Boolean canManageCategories = false;

    @Column(nullable = false)
    private Boolean canManageUsers = false;

    @Column(nullable = false)
    private Boolean canManageRoles = false;

    @Column(nullable = false)
    private Boolean canPreviewOffice = true;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RoleType getRole() { return role; }
    public void setRole(RoleType role) { this.role = role; }
    public Boolean getCanViewLibrary() { return canViewLibrary; }
    public void setCanViewLibrary(Boolean canViewLibrary) { this.canViewLibrary = canViewLibrary; }
    public Boolean getCanCreateContent() { return canCreateContent; }
    public void setCanCreateContent(Boolean canCreateContent) { this.canCreateContent = canCreateContent; }
    public Boolean getCanEditContent() { return canEditContent; }
    public void setCanEditContent(Boolean canEditContent) { this.canEditContent = canEditContent; }
    public Boolean getCanDeleteContent() { return canDeleteContent; }
    public void setCanDeleteContent(Boolean canDeleteContent) { this.canDeleteContent = canDeleteContent; }
    public Boolean getCanManageCategories() { return canManageCategories; }
    public void setCanManageCategories(Boolean canManageCategories) { this.canManageCategories = canManageCategories; }
    public Boolean getCanManageUsers() { return canManageUsers; }
    public void setCanManageUsers(Boolean canManageUsers) { this.canManageUsers = canManageUsers; }
    public Boolean getCanManageRoles() { return canManageRoles; }
    public void setCanManageRoles(Boolean canManageRoles) { this.canManageRoles = canManageRoles; }
    public Boolean getCanPreviewOffice() { return canPreviewOffice; }
    public void setCanPreviewOffice(Boolean canPreviewOffice) { this.canPreviewOffice = canPreviewOffice; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
