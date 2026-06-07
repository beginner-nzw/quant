package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.RoleAccessAuthority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RoleAccessConfigPolicyManager {

    private static final String ACCESS_ROLE_USER = "USER";
    private static final String ACCESS_ROLE_REVIEWER = "REVIEWER";
    private static final String ACCESS_ROLE_ADMIN = "ADMIN";
    private static final String ROLE_RESEARCHER = "RESEARCHER";
    private static final String ROLE_PM = "PM";
    private static final String ROLE_RISK_MANAGER = "RISK_MANAGER";
    private static final String ROLE_COMPLIANCE_AUDITOR = "COMPLIANCE_AUDITOR";
    private static final String ROLE_ADMIN = "ADMIN";

    private final RoleAccessAuthority roleAccessAuthority;

    public RoleAccessConfigItemVO toRoleItem(Map<String, Object> item) {
        RoleAccessConfigItemVO vo = new RoleAccessConfigItemVO();
        vo.setRoleCode(normalizeUpper(item.get("roleCode")));
        vo.setRoleName(normalize(item.get("roleName")));
        vo.setRoleDescription(normalize(item.get("roleDescription")));
        vo.setMenuKeys(sanitizeList(castList(item.get("menuKeys"))));
        vo.setPermissionKeys(sanitizeList(castList(item.get("permissionKeys"))));
        vo.setRemark(normalize(item.get("remark")));
        return vo;
    }

    public boolean hasPermission(String currentRole, String permissionKey, List<Map<String, Object>> roles) {
        String normalizedPermission = normalizeUpper(permissionKey);
        if (normalizedPermission == null) {
            return false;
        }
        LinkedHashSet<String> candidateRoleCodes = resolveRoleCodesByAccessRole(normalizeUpper(currentRole));
        if (candidateRoleCodes.isEmpty()) {
            return false;
        }
        for (Map<String, Object> item : roles) {
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

    public RoleAccessUpdatePlan buildUpdatePlan(String roleCode,
                                                RoleAccessConfigUpdateDTO dto,
                                                List<Map<String, Object>> roles) {
        validateUpdateInput(roleCode, dto);
        List<String> menuKeys = sanitizeList(dto.getMenuKeys());
        if (menuKeys.isEmpty()) {
            throw new BizException("ROLE_ACCESS_MENUS_EMPTY", "Role menu access cannot be empty");
        }
        List<String> permissionKeys = sanitizeList(dto.getPermissionKeys());
        validateCompatibleRoleAccess(roleCode, menuKeys, permissionKeys);

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
        return new RoleAccessUpdatePlan(roleCode, dto.getRoleName(), changedFields, roles);
    }

    private void validateUpdateInput(String roleCode, RoleAccessConfigUpdateDTO dto) {
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

    public record RoleAccessUpdatePlan(
            String roleCode,
            String roleName,
            List<String> changedFields,
            List<Map<String, Object>> roles
    ) {
    }
}
