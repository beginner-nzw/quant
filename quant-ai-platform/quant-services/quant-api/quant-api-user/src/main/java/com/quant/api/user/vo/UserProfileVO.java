package com.quant.api.user.vo;

import java.util.LinkedHashSet;
import java.util.Set;

public class UserProfileVO {

    private String userId;
    private String username;
    private String displayName;
    private Set<String> roles = new LinkedHashSet<>();
    private Set<String> permissions = new LinkedHashSet<>();

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roles);
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions == null ? new LinkedHashSet<>() : new LinkedHashSet<>(permissions);
    }
}
