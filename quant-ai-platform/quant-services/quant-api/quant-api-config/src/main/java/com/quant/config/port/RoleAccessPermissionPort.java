package com.quant.config.port;

public interface RoleAccessPermissionPort {

    boolean hasPermissionForCurrentRole(String permissionKey);

    boolean hasPermission(String currentRole, String permissionKey);

    void requirePermission(String permissionKey);
}
