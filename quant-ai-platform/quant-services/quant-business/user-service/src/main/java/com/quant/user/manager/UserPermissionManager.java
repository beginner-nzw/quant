package com.quant.user.manager;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class UserPermissionManager {

    private final Map<String, Set<String>> rolePermissions = new LinkedHashMap<>();

    public UserPermissionManager() {
        rolePermissions.put("ADMIN", Set.of("USER_MANAGE", "ROLE_MANAGE", "REPORT_READ", "RISK_READ"));
        rolePermissions.put("RESEARCHER", Set.of("TASK_CREATE", "REPORT_READ", "MARKET_EVENT_READ"));
        rolePermissions.put("RISK_MANAGER", Set.of("RISK_READ", "RISK_REVIEW", "REPORT_READ"));
        rolePermissions.put("COMPLIANCE", Set.of("AUDIT_READ", "HUMAN_REVIEW"));
    }

    public Set<String> resolvePermissions(Set<String> roles) {
        Set<String> permissions = new LinkedHashSet<>();
        if (roles == null) {
            return permissions;
        }
        for (String role : roles) {
            permissions.addAll(rolePermissions.getOrDefault(normalize(role), Set.of()));
        }
        return permissions;
    }

    private String normalize(String role) {
        return role == null ? "" : role.trim().toUpperCase();
    }
}
