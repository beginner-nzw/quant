package com.quant.common.security;

import java.util.List;

public class UserContext {

    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> DISPLAY_NAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<UserProfileStatus> STATUS_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> ROLES_HOLDER = new ThreadLocal<>();

    public static void set(String userId, String userRole) {
        set(userId, userRole, UserProfile.unknown(userId));
    }

    public static void set(String userId, String userRole, UserProfile profile) {
        UserProfile resolvedProfile = profile == null ? UserProfile.unknown(userId) : profile;
        USER_ID_HOLDER.set(userId);
        USER_ROLE_HOLDER.set(userRole);
        DISPLAY_NAME_HOLDER.set(resolvedProfile.displayName());
        STATUS_HOLDER.set(resolvedProfile.status());
        ROLES_HOLDER.set(resolvedProfile.roles());
    }

    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static String getUserRole() {
        return USER_ROLE_HOLDER.get();
    }

    public static String getDisplayName() {
        return DISPLAY_NAME_HOLDER.get();
    }

    public static UserProfileStatus getStatus() {
        return STATUS_HOLDER.get();
    }

    public static List<String> getRoles() {
        List<String> roles = ROLES_HOLDER.get();
        return roles == null ? List.of() : roles;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USER_ROLE_HOLDER.remove();
        DISPLAY_NAME_HOLDER.remove();
        STATUS_HOLDER.remove();
        ROLES_HOLDER.remove();
    }

    private UserContext() {
    }
}
