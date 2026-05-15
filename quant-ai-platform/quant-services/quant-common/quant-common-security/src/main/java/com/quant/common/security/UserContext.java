package com.quant.common.security;

public class UserContext {

    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE_HOLDER = new ThreadLocal<>();

    public static void set(String userId, String userRole) {
        USER_ID_HOLDER.set(userId);
        USER_ROLE_HOLDER.set(userRole);
    }

    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static String getUserRole() {
        return USER_ROLE_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USER_ROLE_HOLDER.remove();
    }

    private UserContext() {
    }
}