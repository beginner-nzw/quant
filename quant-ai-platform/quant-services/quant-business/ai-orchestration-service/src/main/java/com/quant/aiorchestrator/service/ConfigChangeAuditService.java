package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.vo.ConfigChangeAuditItemVO;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConfigChangeAuditService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String configAuditPath;
    private final ObjectMapper objectMapper;

    public ConfigChangeAuditService(
            @Value("${quant.ai.config-audit:../../../ai-config/config-change-audits.json}") String configAuditPath,
            ObjectMapper objectMapper
    ) {
        this.configAuditPath = configAuditPath;
        this.objectMapper = objectMapper;
    }

    public void appendAudit(String configType,
                            String targetCode,
                            String targetName,
                            String operation,
                            String configPath,
                            String changeSummary,
                            List<String> changedFields) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("auditId", UUID.randomUUID().toString());
        item.put("configType", normalize(configType));
        item.put("targetCode", normalize(targetCode));
        item.put("targetName", normalize(targetName));
        item.put("operation", normalize(operation));
        item.put("operatorId", normalize(SecurityUtils.currentUserId()) == null ? "unknown" : normalize(SecurityUtils.currentUserId()));
        item.put("operatorRole", normalize(SecurityUtils.currentUserRole()) == null ? "UNKNOWN" : normalize(SecurityUtils.currentUserRole()));
        item.put("configPath", normalize(configPath));
        item.put("changeSummary", normalize(changeSummary));
        item.put("changedFields", sanitizeList(changedFields));
        item.put("createdAt", DATE_TIME_FORMATTER.format(LocalDateTime.now()));

        Path auditPath = resolveAuditPath();
        List<Map<String, Object>> audits = readAudits();
        audits.add(0, item);
        if (audits.size() > 50) {
            audits = new ArrayList<>(audits.subList(0, 50));
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("audits", audits);
        try {
            Files.createDirectories(auditPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(auditPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("CONFIG_AUDIT_SAVE_FAILED", "保存配置变更审计失败");
        }
    }

    public List<ConfigChangeAuditItemVO> loadRecentAudits() {
        List<Map<String, Object>> audits = readAudits();
        List<ConfigChangeAuditItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : audits) {
            ConfigChangeAuditItemVO vo = new ConfigChangeAuditItemVO();
            vo.setAuditId(normalize(item.get("auditId")));
            vo.setConfigType(normalize(item.get("configType")));
            vo.setTargetCode(normalize(item.get("targetCode")));
            vo.setTargetName(normalize(item.get("targetName")));
            vo.setOperation(normalize(item.get("operation")));
            vo.setOperatorId(normalize(item.get("operatorId")));
            vo.setOperatorRole(normalize(item.get("operatorRole")));
            vo.setConfigPath(normalize(item.get("configPath")));
            vo.setChangeSummary(normalize(item.get("changeSummary")));
            vo.setChangedFields(sanitizeList(castList(item.get("changedFields"))));
            vo.setCreatedAt(normalize(item.get("createdAt")));
            result.add(vo);
        }
        return result;
    }

    private List<Map<String, Object>> readAudits() {
        Path auditPath = resolveAuditPath();
        if (!Files.exists(auditPath)) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    Files.readString(auditPath, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
            Object audits = root.get("audits");
            if (!(audits instanceof List<?> auditList)) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : auditList) {
                if (item instanceof Map<?, ?> rawItem) {
                    result.add(new LinkedHashMap<>(objectMapper.convertValue(
                            rawItem,
                            new TypeReference<LinkedHashMap<String, Object>>() {}
                    )));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BizException("CONFIG_AUDIT_READ_FAILED", "读取配置变更审计失败");
        }
    }

    private Path resolveAuditPath() {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(configAuditPath);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("ai-config").resolve("config-change-audits.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("config-change-audits.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("config-change-audits.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("config-change-audits.json").normalize());
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

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
