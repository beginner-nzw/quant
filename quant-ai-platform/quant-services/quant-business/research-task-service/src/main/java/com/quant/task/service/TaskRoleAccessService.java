package com.quant.task.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class TaskRoleAccessService {

    public static final String PERMISSION_TASK_CREATE = "TASK_CREATE";

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

    public TaskRoleAccessService(
            @Value("${quant.ai.role-access-config:../../../ai-config/role-access-configs.json}") String roleAccessConfigPath,
            ObjectMapper objectMapper
    ) {
        this.roleAccessConfigPath = roleAccessConfigPath;
        this.objectMapper = objectMapper;
    }

    public void requirePermission(String permissionKey) {
        if (!hasPermission(SecurityUtils.currentUserRole(), permissionKey)) {
            throw new BizException("FORBIDDEN", "当前用户无权限执行该操作");
        }
    }

    private boolean hasPermission(String currentRole, String permissionKey) {
        String normalizedRole = normalize(currentRole);
        String normalizedPermission = normalize(permissionKey);
        if (normalizedRole == null || normalizedPermission == null) {
            return false;
        }

        LinkedHashSet<String> candidateRoleCodes = resolveRoleCodesByAccessRole(normalizedRole);
        if (candidateRoleCodes.isEmpty()) {
            return false;
        }

        for (Map<String, Object> item : readRoles()) {
            String roleCode = normalize(item.get("roleCode"));
            if (!candidateRoleCodes.contains(roleCode)) {
                continue;
            }

            List<String> permissionKeys = castList(item.get("permissionKeys"));
            if (permissionKeys.contains(normalizedPermission)) {
                return true;
            }
        }

        return false;
    }

    private LinkedHashSet<String> resolveRoleCodesByAccessRole(String currentRole) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String normalizedRole = normalize(currentRole);
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

    private List<Map<String, Object>> readRoles() {
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            return List.of();
        }

        try {
            Map<String, Object> root = objectMapper.readValue(
                    Files.readString(configPath, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
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
            return List.of();
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

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.toUpperCase();
    }
}
