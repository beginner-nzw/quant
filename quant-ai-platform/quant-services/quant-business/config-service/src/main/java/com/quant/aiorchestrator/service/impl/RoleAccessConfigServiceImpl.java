package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.aiorchestrator.manager.RoleAccessConfigPolicyManager;
import com.quant.aiorchestrator.manager.RoleAccessConfigStoreManager;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityUtils;
import com.quant.common.security.UserProfile;
import com.quant.common.security.UserProfileSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleAccessConfigServiceImpl implements RoleAccessConfigService {

    private final UserProfileSource userProfileSource;
    private final RoleAccessConfigStoreManager configStoreManager;
    private final RoleAccessConfigPolicyManager policyManager;

    public List<RoleAccessConfigItemVO> loadRoles() {
        return configStoreManager.readRoles().stream()
                .map(policyManager::toRoleItem)
                .toList();
    }

    public boolean hasPermissionForCurrentRole(String permissionKey) {
        return currentProfile()
                .map(profile -> profile.roles().stream()
                        .anyMatch(role -> hasPermission(role, permissionKey)))
                .orElse(false);
    }

    public boolean hasPermission(String currentRole, String permissionKey) {
        return policyManager.hasPermission(currentRole, permissionKey, configStoreManager.readRoles());
    }

    public void requirePermission(String permissionKey) {
        if (!hasPermissionForCurrentRole(permissionKey)) {
            throw new BizException("FORBIDDEN", "Current user cannot perform this operation");
        }
    }

    public void saveRole(String roleCode, RoleAccessConfigUpdateDTO dto) {
        RoleAccessConfigPolicyManager.RoleAccessUpdatePlan plan = policyManager.buildUpdatePlan(
                roleCode,
                dto,
                configStoreManager.readRoles()
        );
        configStoreManager.writeRoles(
                plan.roleCode(),
                plan.roleName(),
                plan.changedFields(),
                plan.roles()
        );
    }

    public String resolveConfigPathForDisplay() {
        return configStoreManager.displayPath();
    }

    private Optional<UserProfile> currentProfile() {
        return userProfileSource.findByUserId(SecurityUtils.currentUserId())
                .filter(UserProfile::active);
    }
}
