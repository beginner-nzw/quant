package com.quant.user.manager;

import com.quant.api.user.vo.UserProfileVO;
import com.quant.common.security.SecurityUtils;
import com.quant.user.domain.entity.UserAccount;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class CurrentUserManager {

    private final UserAccountManager userAccountManager;
    private final UserPermissionManager userPermissionManager;

    public CurrentUserManager(UserAccountManager userAccountManager,
                              UserPermissionManager userPermissionManager) {
        this.userAccountManager = userAccountManager;
        this.userPermissionManager = userPermissionManager;
    }

    public UserProfileVO currentUserProfile() {
        String currentUserId = SecurityUtils.currentUserId();
        UserAccount account = userAccountManager.findUser(currentUserId).orElse(null);
        if (account != null) {
            return toProfile(account.getUserId(), account.getUsername(), account.getDisplayName(), account.getRoles());
        }

        Set<String> roles = new LinkedHashSet<>(SecurityUtils.currentUserRoles());
        return toProfile(
                currentUserId,
                currentUserId,
                defaultIfBlank(SecurityUtils.currentDisplayName(), currentUserId),
                roles
        );
    }

    private UserProfileVO toProfile(String userId, String username, String displayName, Set<String> roles) {
        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(defaultIfBlank(userId, "anonymous"));
        profile.setUsername(defaultIfBlank(username, profile.getUserId()));
        profile.setDisplayName(defaultIfBlank(displayName, profile.getUsername()));
        profile.setRoles(roles == null ? Set.of() : roles);
        profile.setPermissions(userPermissionManager.resolvePermissions(profile.getRoles()));
        return profile;
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
