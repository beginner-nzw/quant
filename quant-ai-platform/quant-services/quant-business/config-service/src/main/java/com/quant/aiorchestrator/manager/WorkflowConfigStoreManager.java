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
public class WorkflowConfigStoreManager {

    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> readWorkflows(String configPath) {
        Path path = resolveConfigPath(configPath);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    Files.readString(path, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
            Object workflows = root.get("workflows");
            if (!(workflows instanceof List<?> workflowList)) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : workflowList) {
                if (item instanceof Map<?, ?> rawItem) {
                    result.add(new LinkedHashMap<>(objectMapper.convertValue(
                            rawItem,
                            new TypeReference<LinkedHashMap<String, Object>>() {}
                    )));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BizException("WORKFLOW_READ_FAILED", "workflow config read failed");
        }
    }

    public void writeWorkflows(Path path, List<Map<String, Object>> workflows) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("workflows", workflows);
        try {
            Files.createDirectories(path.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(path, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("WORKFLOW_SAVE_FAILED", "workflow config save failed");
        }
    }

    public Path resolveConfigPath(String configPath) {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(configPath);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("ai-config").resolve("workflow-configs.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("workflow-configs.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("workflow-configs.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("workflow-configs.json").normalize());
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
