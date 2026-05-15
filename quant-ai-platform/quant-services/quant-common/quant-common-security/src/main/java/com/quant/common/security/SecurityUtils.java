package com.quant.common.security;

public final class SecurityUtils {

    public static String currentUserId() {
        return UserContext.getUserId();
    }

    public static String currentUserRole() {
        return UserContext.getUserRole();
    }

    public static boolean isAdmin() {
        return UserRoleEnum.ADMIN.matches(currentUserRole());
    }

    public static boolean isReviewer() {
        String role = currentUserRole();
        return UserRoleEnum.REVIEWER.matches(role)
                || UserRoleEnum.ADMIN.matches(role);
    }

    private SecurityUtils() {
    }
}
