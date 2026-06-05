package com.quant.common.security;

import java.util.List;

public final class SecurityUtils {

    public static String currentUserId() {
        return UserContext.getUserId();
    }

    public static String currentUserRole() {
        return UserContext.getUserRole();
    }

    public static List<String> currentUserRoles() {
        return UserContext.getRoles();
    }

    public static UserProfileStatus currentUserStatus() {
        return UserContext.getStatus();
    }

    public static String currentDisplayName() {
        return UserContext.getDisplayName();
    }

    public static boolean isAdmin() {
        return currentUserRoles().stream().anyMatch(UserRoleEnum.ADMIN::matches);
    }

    public static boolean isReviewer() {
        return currentUserRoles().stream()
                .anyMatch(role -> UserRoleEnum.REVIEWER.matches(role) || UserRoleEnum.ADMIN.matches(role));
    }

    private SecurityUtils() {
    }
}
