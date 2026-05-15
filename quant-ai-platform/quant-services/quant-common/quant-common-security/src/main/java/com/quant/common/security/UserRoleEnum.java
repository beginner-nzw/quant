package com.quant.common.security;

public enum UserRoleEnum {
    USER,
    REVIEWER,
    ADMIN,
    RESEARCHER,
    PM,
    RISK_MANAGER,
    COMPLIANCE_AUDITOR;

    public boolean matches(String role) {
        String normalizedRole = normalize(role);
        if (normalizedRole == null) {
            return false;
        }
        return switch (this) {
            case USER -> "USER".equals(normalizedRole)
                    || RESEARCHER.name().equals(normalizedRole)
                    || PM.name().equals(normalizedRole)
                    || RISK_MANAGER.name().equals(normalizedRole);
            case REVIEWER -> "REVIEWER".equals(normalizedRole)
                    || COMPLIANCE_AUDITOR.name().equals(normalizedRole);
            case ADMIN -> ADMIN.name().equals(normalizedRole);
            case RESEARCHER -> RESEARCHER.name().equals(normalizedRole);
            case PM -> PM.name().equals(normalizedRole);
            case RISK_MANAGER -> RISK_MANAGER.name().equals(normalizedRole);
            case COMPLIANCE_AUDITOR -> COMPLIANCE_AUDITOR.name().equals(normalizedRole)
                    || "REVIEWER".equals(normalizedRole);
        };
    }

    public static UserRoleEnum from(String role) {
        String normalizedRole = normalize(role);
        if (normalizedRole == null) {
            return USER;
        }
        for (UserRoleEnum value : values()) {
            if (value.name().equals(normalizedRole)) {
                return value;
            }
        }
        if ("REVIEWER".equals(normalizedRole)) {
            return COMPLIANCE_AUDITOR;
        }
        if ("USER".equals(normalizedRole)) {
            return USER;
        }
        return USER;
    }

    private static String normalize(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return role.trim().toUpperCase();
    }
}
