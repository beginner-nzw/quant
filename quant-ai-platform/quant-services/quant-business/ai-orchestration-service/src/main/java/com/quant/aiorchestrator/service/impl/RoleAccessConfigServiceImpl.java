package com.quant.aiorchestrator.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.configstore.ConfigStoreKey;
import com.quant.aiorchestrator.configstore.GovernedConfigStore;
import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.RoleAccessAuthority;
import com.quant.common.security.SecurityUtils;
import com.quant.common.security.UserProfile;
import com.quant.common.security.UserProfileSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class RoleAccessConfigServiceImpl implements RoleAccessConfigService {

    public static final String PERMISSION_TASK_VIEW = "TASK_VIEW";
    public static final String PERMISSION_TASK_CREATE = "TASK_CREATE";
    public static final String PERMISSION_TASK_RETRY = "TASK_RETRY";
    public static final String PERMISSION_TASK_CANCEL = "TASK_CANCEL";
    public static final String PERMISSION_AUDIT_COMPLIANCE_VIEW = "AUDIT_COMPLIANCE_VIEW";
    public static final String PERMISSION_REPORT_REVIEW = "REPORT_REVIEW";
    public static final String PERMISSION_MODEL_AGENT_CONFIG_VIEW = "MODEL_AGENT_CONFIG_VIEW";
    public static final String PERMISSION_MODEL_AGENT_CONFIG_EDIT = "MODEL_AGENT_CONFIG_EDIT";

    private static final String ACCESS_ROLE_USER = "USER";
    private static final String ACCESS_ROLE_REVIEWER = "REVIEWER";
    private static final String ACCESS_ROLE_ADMIN = "ADMIN";
    private static final String ROLE_RESEARCHER = "RESEARCHER";
    private static final String ROLE_PM = "PM";
    private static final String ROLE_RISK_MANAGER = "RISK_MANAGER";
    private static final String ROLE_COMPLIANCE_AUDITOR = "COMPLIANCE_AUDITOR";
    private static final String ROLE_ADMIN = "ADMIN";

    private final ObjectMapper objectMapper;
    private final UserProfileSource userProfileSource;
    private final RoleAccessAuthority roleAccessAuthority;
    private final GovernedConfigStore governedConfigStore;

    public RoleAccessConfigServiceImpl(
            ObjectMapper objectMapper,
            UserProfileSource userProfileSource,
            RoleAccessAuthority roleAccessAuthority,
            GovernedConfigStore governedConfigStore
    ) {
        this.objectMapper = objectMapper;
        this.userProfileSource = userProfileSource;
        this.roleAccessAuthority = roleAccessAuthority;
        this.governedConfigStore = governedConfigStore;
    }

    public List<RoleAccessConfigItemVO> loadRoles() {
        List<RoleAccessConfigItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : readRoles()) {
            result.add(toRoleItem(item));
        }
        return result;
    }

    public boolean hasPermissionForCurrentRole(String permissionKey) {
        return currentProfile()
                .map(profile -> profile.roles().stream()
                        .anyMatch(role -> hasPermission(role, permissionKey)))
                .orElse(false);
    }

    public boolean hasPermission(String currentRole, String permissionKey) {
        String normalizedPermission = normalizeUpper(permissionKey);
        if (normalizedPermission == null) {
            return false;
        }
        LinkedHashSet<String> candidateRoleCodes = resolveRoleCodesByAccessRole(normalizeUpper(currentRole));
        if (candidateRoleCodes.isEmpty()) {
            return false;
        }
        for (Map<String, Object> item : readRoles()) {
            String roleCode = normalizeUpper(item.get("roleCode"));
            if (!candidateRoleCodes.contains(roleCode)) {
                continue;
            }
            if (sanitizeList(castList(item.get("permissionKeys"))).contains(normalizedPermission)
                    && roleAccessAuthority.permissionAllowedForRole(roleCode, normalizedPermission)) {
                return true;
            }
        }
        return false;
    }

    public void requirePermission(String permissionKey) {
        if (!hasPermissionForCurrentRole(permissionKey)) {
            throw new BizException("FORBIDDEN", "Current user cannot perform this operation");
        }
    }

    public void saveRole(String roleCode, RoleAccessConfigUpdateDTO dto) {
        if (dto == null) {
            throw new BizException("ROLE_ACCESS_EMPTY", "Role access config update cannot be empty");
        }
        if (!hasText(roleCode)) {
            throw new BizException("ROLE_ACCESS_CODE_EMPTY", "Role code cannot be empty");
        }
        if (!hasText(dto.getRoleName())) {
            throw new BizException("ROLE_ACCESS_NAME_EMPTY", "Role name cannot be empty");
        }
        if (!hasText(dto.getRoleDescription())) {
            throw new BizException("ROLE_ACCESS_DESC_EMPTY", "Role description cannot be empty");
        }

        List<String> menuKeys = sanitizeList(dto.getMenuKeys());
        if (menuKeys.isEmpty()) {
            throw new BizException("ROLE_ACCESS_MENUS_EMPTY", "Role menu access cannot be empty");
        }
        List<String> permissionKeys = sanitizeList(dto.getPermissionKeys());
        validateCompatibleRoleAccess(roleCode, menuKeys, permissionKeys);

        List<Map<String, Object>> roles = readRoles();
        boolean updated = false;
        List<String> changedFields = new ArrayList<>();

        for (Map<String, Object> item : roles) {
            if (Objects.equals(normalizeUpper(item.get("roleCode")), normalizeUpper(roleCode))) {
                Map<String, Object> before = new LinkedHashMap<>(item);
                item.put("roleName", dto.getRoleName().trim());
                item.put("roleDescription", dto.getRoleDescription().trim());
                item.put("menuKeys", menuKeys);
                item.put("permissionKeys", permissionKeys);
                item.put("remark", normalize(dto.getRemark()));
                changedFields = diffFields(before, item);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new BizException("ROLE_ACCESS_NOT_FOUND", "Role access config not found: " + roleCode);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("roles", roles);
        governedConfigStore.writeRoot(
                ConfigStoreKey.ROLE_ACCESS,
                root,
                roleCode,
                dto.getRoleName(),
                "UPDATE",
                "Update role access config",
                changedFields
        );
    }

    public String resolveConfigPathForDisplay() {
        return governedConfigStore.displayPath(ConfigStoreKey.ROLE_ACCESS);
    }

    private List<Map<String, Object>> readRoles() {
        try {
            Map<String, Object> emptyRoot = new LinkedHashMap<>();
            emptyRoot.put("roles", List.of());
            Map<String, Object> root = governedConfigStore.readRoot(ConfigStoreKey.ROLE_ACCESS, emptyRoot);
            Object roles = root.get("roles");
            if (!(roles instanceof List<?> roleList)) {
                return List.of();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : roleList) {
                if (item instanceof Map<?, ?> rawItem) {
                    result.add(new LinkedHashMap<>(objectMapper.convertValue(
                            rawItem,
                            new TypeReference<LinkedHashMap<String, Object>>() {}
                    )));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BizException("ROLE_ACCESS_READ_FAILED", "Failed to read governed role access config");
        }
    }

    private LinkedHashSet<String> resolveRoleCodesByAccessRole(String currentRole) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String normalizedRole = normalizeUpper(currentRole);
        if (normalizedRole == null) {
            return result;
        }

        result.add(normalizedRole);
        switch (normalizedRole) {
            case ACCESS_ROLE_ADMIN -> result.add(ROLE_ADMIN);
            case ACCESS_ROLE_REVIEWER -> result.add(ROLE_COMPLIANCE_AUDITOR);
            case ACCESS_ROLE_USER -> {
                result.add(ROLE_RESEARCHER);
                result.add(ROLE_PM);
                result.add(ROLE_RISK_MANAGER);
            }
            default -> {
            }
        }
        return result;
    }

    private RoleAccessConfigItemVO toRoleItem(Map<String, Object> item) {
        RoleAccessConfigItemVO vo = new RoleAccessConfigItemVO();
        vo.setRoleCode(normalizeUpper(item.get("roleCode")));
        vo.setRoleName(normalize(item.get("roleName")));
        vo.setRoleDescription(normalize(item.get("roleDescription")));
        vo.setMenuKeys(sanitizeList(castList(item.get("menuKeys"))));
        vo.setPermissionKeys(sanitizeList(castList(item.get("permissionKeys"))));
        vo.setRemark(normalize(item.get("remark")));
        return vo;
    }

    private Optional<UserProfile> currentProfile() {
        return userProfileSource.findByUserId(SecurityUtils.currentUserId())
                .filter(UserProfile::active);
    }

    private void validateCompatibleRoleAccess(String roleCode, List<String> menuKeys, List<String> permissionKeys) {
        if (!roleAccessAuthority.roleExists(roleCode)) {
            throw new BizException("ROLE_ACCESS_NOT_ALLOWED", "Role code is outside production role baseline");
        }
        for (String menuKey : menuKeys) {
            if (!roleAccessAuthority.menuAllowedForRole(roleCode, menuKey)) {
                throw new BizException("ROLE_ACCESS_MENU_WIDENED", "Menu access cannot be widened: " + menuKey);
            }
        }
        for (String permissionKey : permissionKeys) {
            if (!roleAccessAuthority.permissionAllowedForRole(roleCode, permissionKey)) {
                throw new BizException("ROLE_ACCESS_PERMISSION_WIDENED", "Permission cannot be widened: " + permissionKey);
            }
        }
    }

    private List<String> diffFields(Map<String, Object> before, Map<String, Object> after) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        fields.addAll(before.keySet());
        fields.addAll(after.keySet());
        List<String> result = new ArrayList<>();
        for (String field : fields) {
            if (!Objects.equals(before.get(field), after.get(field))) {
                result.add(field);
            }
        }
        return result;
    }

    private List<String> castList(Object value) {
        if (!(value instanceof List<?> items)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            String normalized = normalizeUpper(item);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String normalized = normalizeUpper(value);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeUpper(Object value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(java.util.Locale.ROOT);
    }
}
