package com.quant.user.security;

import com.quant.common.security.UserProfile;
import com.quant.common.security.UserProfileSource;
import com.quant.common.security.UserProfileStatus;
import com.quant.user.manager.UserAccountManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserServiceProfileSource implements UserProfileSource {

    private final UserAccountManager userAccountManager;

    public UserServiceProfileSource(UserAccountManager userAccountManager) {
        this.userAccountManager = userAccountManager;
    }

    @Override
    public Optional<UserProfile> findByUserId(String userId) {
        return userAccountManager.findUser(userId)
                .map(account -> new UserProfile(
                        account.getUserId(),
                        account.getDisplayName(),
                        UserProfileStatus.ACTIVE,
                        account.getRoles().stream().toList()
                ));
    }
}
