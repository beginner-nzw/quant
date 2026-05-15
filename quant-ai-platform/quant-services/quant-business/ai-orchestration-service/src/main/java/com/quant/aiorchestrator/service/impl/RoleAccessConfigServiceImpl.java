package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private final String roleAccessConfigPath;
    private final ObjectMapper objectMapper;
    private final ConfigChangeAuditService configChangeAuditService;

    public RoleAccessConfigServiceImpl(
            @Value("${quant.ai.role-access-config:../../../ai-config/role-access-configs.json}") String roleAccessConfigPath,
            ObjectMapper objectMapper,
            ConfigChangeAuditService configChangeAuditService
    ) {
        this.roleAccessConfigPath = roleAccessConfigPath;
        this.objectMapper = objectMapper;
        this.configChangeAuditService = configChangeAuditService;
    }

    public List<RoleAccessConfigItemVO> loadRoles() {
        List<Map<String, Object>> roles = readRoles();
        List<RoleAccessConfigItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : roles) {
            result.add(toRoleItem(item));
        }
        return result;
    }

    public boolean hasPermissionForCurrentRole(String permissionKey) {
        return hasPermission(SecurityUtils.currentUserRole(), permissionKey);
    }

    public boolean hasPermission(String currentRole, String permissionKey) {
        String normalizedRole = normalize(currentRole);
        String normalizedPermission = normalize(permissionKey);
        if (normalizedRole == null || normalizedPermission == null) {
            return false;
        }

        LinkedHashSet<String> candidateRoleCodes = resolveRoleCodesByAccessRole(normalizedRole);
        if (candidateRoleCodes.isEmpty()) {
            return false;
        }

        return loadRoles().stream()
                .filter(item -> candidateRoleCodes.contains(item.getRoleCode()))
                .map(RoleAccessConfigItemVO::getPermissionKeys)
                .filter(Objects::nonNull)
                .anyMatch(permissionKeys -> permissionKeys.contains(normalizedPermission));
    }

    public void requirePermission(String permissionKey) {
        if (!hasPermissionForCurrentRole(permissionKey)) {
            throw new BizException("FORBIDDEN", "当前用户无权限执行该操作");
        }
    }

    public void saveRole(String roleCode, RoleAccessConfigUpdateDTO dto) {
        if (dto == null) {
            throw new BizException("ROLE_ACCESS_EMPTY", "角色权限配置内容不能为空");
        }
        if (!hasText(roleCode)) {
            throw new BizException("ROLE_ACCESS_CODE_EMPTY", "角色编码不能为空");
        }
        if (!hasText(dto.getRoleName())) {
            throw new BizException("ROLE_ACCESS_NAME_EMPTY", "角色名称不能为空");
        }
        if (!hasText(dto.getRoleDescription())) {
            throw new BizException("ROLE_ACCESS_DESC_EMPTY", "角色说明不能为空");
        }

        List<String> menuKeys = sanitizeList(dto.getMenuKeys());
        if (menuKeys.isEmpty()) {
            throw new BizException("ROLE_ACCESS_MENUS_EMPTY", "菜单权限不能为空");
        }
        List<String> permissionKeys = sanitizeList(dto.getPermissionKeys());

        Path configPath = resolveConfigPath();
        List<Map<String, Object>> roles = readRoles();
        boolean updated = false;

        for (Map<String, Object> item : roles) {
            if (Objects.equals(normalize(item.get("roleCode")), roleCode.trim())) {
                Map<String, Object> before = new LinkedHashMap<>(item);
                item.put("roleName", dto.getRoleName().trim());
                item.put("roleDescription", dto.getRoleDescription().trim());
                item.put("menuKeys", menuKeys);
                item.put("permissionKeys", permissionKeys);
                item.put("remark", normalize(dto.getRemark()));
                updated = true;
                configChangeAuditService.appendAudit(
                        "ROLE_ACCESS_CONFIG",
                        roleCode,
                        dto.getRoleName(),
                        "UPDATE",
                        configPath.toString(),
                        "更新角色权限配置",
                        diffFields(before, item)
                );
                break;
            }
        }

        if (!updated) {
            throw new BizException("ROLE_ACCESS_NOT_FOUND", "未找到角色权限配置: " + roleCode);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("roles", roles);
        try {
            Files.createDirectories(configPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(configPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("ROLE_ACCESS_SAVE_FAILED", "保存角色权限配置失败: " + roleCode);
        }
    }

    public String resolveConfigPathForDisplay() {
        return resolveConfigPath().toString();
    }

    private LinkedHashSet<String> resolveRoleCodesByAccessRole(String currentRole) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String normalizedRole = normalize(currentRole);
        if (normalizedRole == null) {
            return result;
        }

        result.add(normalizedRole);

        switch (normalizedRole.toUpperCase()) {
            case ACCESS_ROLE_ADMIN:
                result.add(ROLE_ADMIN);
                break;
            case ACCESS_ROLE_REVIEWER:
                result.add(ROLE_COMPLIANCE_AUDITOR);
                break;
            case ACCESS_ROLE_USER:
                result.add(ROLE_RESEARCHER);
                result.add(ROLE_PM);
                result.add(ROLE_RISK_MANAGER);
                break;
            default:
                break;
        }

        return result;
    }

    private RoleAccessConfigItemVO toRoleItem(Map<String, Object> item) {
        RoleAccessConfigItemVO vo = new RoleAccessConfigItemVO();
        vo.setRoleCode(normalize(item.get("roleCode")));
        vo.setRoleName(normalize(item.get("roleName")));
        vo.setRoleDescription(normalize(item.get("roleDescription")));
        vo.setMenuKeys(sanitizeList(castList(item.get("menuKeys"))));
        vo.setPermissionKeys(sanitizeList(castList(item.get("permissionKeys"))));
        vo.setRemark(normalize(item.get("remark")));
        return vo;
    }

    private List<Map<String, Object>> readRoles() {
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    Files.readString(configPath, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
            Object roles = root.get("roles");
            if (!(roles instanceof List<?> roleList)) {
                return new ArrayList<>();
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
            throw new BizException("ROLE_ACCESS_READ_FAILED", "读取角色权限配置失败");
        }
    }

    private Path resolveConfigPath() {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(roleAccessConfigPath);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("ai-config").resolve("role-access-configs.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("role-access-configs.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("role-access-configs.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("role-access-configs.json").normalize());
            current = current.getParent();
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return candidates.iterator().next();
    }

    private List<String> castList(Object value) {
        if (!(value instanceof List<?> items)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            String normalized = normalize(item);
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
            String normalized = normalize(value);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
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
}
