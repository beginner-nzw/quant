package com.quant.api.user.dto;

import java.util.LinkedHashSet;
import java.util.Set;

public class UserCreateDTO {

    private String username;
    private String displayName;
    private Set<String> roles = new LinkedHashSet<>();

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
}
