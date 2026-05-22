package com.company.knowledge.dto;

import jakarta.validation.constraints.NotNull;

public class RolePermissionRequest {
    @NotNull
    private Boolean canViewLibrary;
    @NotNull
    private Boolean canCreateContent;
    @NotNull
    private Boolean canEditContent;
    @NotNull
    private Boolean canDeleteContent;
    @NotNull
    private Boolean canManageCategories;
    @NotNull
    private Boolean canManageUsers;
    @NotNull
    private Boolean canManageRoles;
    @NotNull
    private Boolean canPreviewOffice;

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
}
