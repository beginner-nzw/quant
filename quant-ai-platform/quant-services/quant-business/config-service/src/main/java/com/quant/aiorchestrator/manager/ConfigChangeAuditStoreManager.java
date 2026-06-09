package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConfigChangeAuditStoreManager {

    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> readAudits(String configAuditPath) {
        Path auditPath = resolveAuditPath(configAuditPath);
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
            throw new BizException("CONFIG_AUDIT_READ_FAILED", "璇诲彇閰嶇疆鍙樻洿瀹¤澶辫触");
        }
    }

    public void writeAudits(String configAuditPath, List<Map<String, Object>> audits) {
        Path auditPath = resolveAuditPath(configAuditPath);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("audits", audits);
        try {
            Files.createDirectories(auditPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(auditPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("CONFIG_AUDIT_SAVE_FAILED", "淇濆瓨閰嶇疆鍙樻洿瀹¤澶辫触");
        }
    }

    private Path resolveAuditPath(String configAuditPath) {
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
}
