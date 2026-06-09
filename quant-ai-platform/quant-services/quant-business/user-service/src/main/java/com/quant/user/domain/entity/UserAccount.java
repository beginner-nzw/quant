package com.quant.user.domain.entity;

import java.util.LinkedHashSet;
import java.util.Set;

public class UserAccount {

    private final String userId;
    private final String username;
    private String displayName;
    private final Set<String> roles = new LinkedHashSet<>();

    public UserAccount(String userId, String username, String displayName, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        grantRoles(roles);
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<String> getRoles() {
        return new LinkedHashSet<>(roles);
    }

    public void grantRoles(Set<String> nextRoles) {
        if (nextRoles != null) {
            nextRoles.stream()
                    .map(role -> role == null ? "" : role.trim())
                    .filter(role -> !role.isEmpty())
                    .map(String::toUpperCase)
                    .forEach(roles::add);
        }
    }
}
