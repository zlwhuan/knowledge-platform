package com.company.knowledge.dto;

import com.company.knowledge.entity.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String displayName;
    private String password;
    @NotNull
    private RoleType role;
    private Boolean enabled = true;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public RoleType getRole() { return role; }
    public void setRole(RoleType role) { this.role = role; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
