package com.quant.common.security;

import java.util.List;

public record RoleAccessDefinition(
        String roleCode,
        List<String> menuKeys,
        List<String> permissionKeys
) {

    public RoleAccessDefinition {
        menuKeys = menuKeys == null ? List.of() : List.copyOf(menuKeys);
        permissionKeys = permissionKeys == null ? List.of() : List.copyOf(permissionKeys);
    }
}
