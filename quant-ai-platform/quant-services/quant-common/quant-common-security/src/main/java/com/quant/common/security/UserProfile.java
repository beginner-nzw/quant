package com.quant.common.security;

import java.util.List;

public record UserProfile(
        String userId,
        String displayName,
        UserProfileStatus status,
        List<String> roles
) {

    public UserProfile {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public boolean active() {
        return status == UserProfileStatus.ACTIVE;
    }

    public static UserProfile unknown(String userId) {
        return new UserProfile(userId, "Unknown User", UserProfileStatus.UNKNOWN, List.of());
    }
}
