package com.quant.api.user.dto;

import java.util.LinkedHashSet;
import java.util.Set;

public class UserRoleGrantDTO {

    private Set<String> roles = new LinkedHashSet<>();

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roles);
    }
}
