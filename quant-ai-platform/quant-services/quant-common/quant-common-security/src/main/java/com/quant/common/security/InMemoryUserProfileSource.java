package com.quant.common.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserProfileSource implements UserProfileSource {

    private final Map<String, UserProfile> profilesByUserId;

    public InMemoryUserProfileSource() {
        this(defaultProfiles());
    }

    public InMemoryUserProfileSource(List<UserProfile> profiles) {
        LinkedHashMap<String, UserProfile> result = new LinkedHashMap<>();
        for (UserProfile profile : profiles == null ? List.<UserProfile>of() : profiles) {
            String userId = normalizeUserId(profile.userId());
            if (userId != null) {
                result.put(userId, profile);
            }
        }
        this.profilesByUserId = Map.copyOf(result);
    }

    @Override
    public Optional<UserProfile> findByUserId(String userId) {
        String normalizedUserId = normalizeUserId(userId);
        if (normalizedUserId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(profilesByUserId.get(normalizedUserId));
    }

    private static List<UserProfile> defaultProfiles() {
        return List.of(
                active("researcher", "Researcher", "RESEARCHER"),
                active("pm", "Portfolio Manager", "PM"),
                active("risk_manager", "Risk Manager", "RISK_MANAGER"),
                active("compliance_auditor", "Compliance Auditor", "COMPLIANCE_AUDITOR"),
                active("admin", "Platform Admin", "ADMIN"),
                active("system", "System Automation", "ADMIN"),
                new UserProfile("disabled_user", "Disabled User", UserProfileStatus.DISABLED, List.of("RESEARCHER"))
        );
    }

    private static UserProfile active(String userId, String displayName, String role) {
        return new UserProfile(userId, displayName, UserProfileStatus.ACTIVE, List.of(role));
    }

    private static String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return userId.trim().toLowerCase(Locale.ROOT);
    }
}
