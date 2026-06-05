package com.quant.task.service.impl;

import com.quant.common.core.exception.BizException;
import com.quant.common.security.RoleAccessAuthority;
import com.quant.common.security.SecurityUtils;
import com.quant.common.security.UserProfile;
import com.quant.common.security.UserProfileSource;
import com.quant.task.service.TaskRoleAccessService;
import org.springframework.stereotype.Service;

@Service
public class TaskRoleAccessServiceImpl implements TaskRoleAccessService {

    public static final String PERMISSION_TASK_CREATE = "TASK_CREATE";

    private final UserProfileSource userProfileSource;
    private final RoleAccessAuthority roleAccessAuthority;

    public TaskRoleAccessServiceImpl(
            UserProfileSource userProfileSource,
            RoleAccessAuthority roleAccessAuthority
    ) {
        this.userProfileSource = userProfileSource;
        this.roleAccessAuthority = roleAccessAuthority;
    }

    public void requirePermission(String permissionKey) {
        if (!userProfileSource.findByUserId(SecurityUtils.currentUserId())
                .filter(UserProfile::active)
                .map(profile -> roleAccessAuthority.hasPermission(profile, permissionKey))
                .orElse(false)) {
            throw new BizException("FORBIDDEN", "Current user cannot perform this operation");
        }
    }
}
