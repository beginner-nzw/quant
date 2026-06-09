package com.quant.user.service.impl;

import com.quant.api.user.UserPermissionPort;
import com.quant.api.user.dto.UserCreateDTO;
import com.quant.api.user.dto.UserRoleGrantDTO;
import com.quant.api.user.vo.UserProfileVO;
import com.quant.user.domain.entity.UserAccount;
import com.quant.user.manager.UserAccountManager;
import com.quant.user.manager.UserPermissionManager;
import org.springframework.stereotype.Service;

@Service
public class UserPermissionServiceImpl implements UserPermissionPort {

    private final UserAccountManager userAccountManager;
    private final UserPermissionManager userPermissionManager;

    public UserPermissionServiceImpl(UserAccountManager userAccountManager,
                                     UserPermissionManager userPermissionManager) {
        this.userAccountManager = userAccountManager;
        this.userPermissionManager = userPermissionManager;
    }

    @Override
    public UserProfileVO createUser(UserCreateDTO dto) {
        return toProfile(userAccountManager.createUser(dto));
    }

    @Override
    public UserProfileVO grantRoles(String userId, UserRoleGrantDTO dto) {
        UserAccount account = userAccountManager.requireUser(userId);
        account.grantRoles(dto == null ? null : dto.getRoles());
        return toProfile(account);
    }

    @Override
    public UserProfileVO getUserProfile(String userId) {
        return toProfile(userAccountManager.requireUser(userId));
    }

    private UserProfileVO toProfile(UserAccount account) {
        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(account.getUserId());
        profile.setUsername(account.getUsername());
        profile.setDisplayName(account.getDisplayName());
        profile.setRoles(account.getRoles());
        profile.setPermissions(userPermissionManager.resolvePermissions(account.getRoles()));
        return profile;
    }
}
