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
public class EventSourceConfigStoreManager {

    private final ObjectMapper objectMapper;

    public Map<String, Object> readRootConfig(String configPath) {
        Path path = resolveConfigPath(configPath);
        if (!Files.exists(path)) {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("sources", new ArrayList<>());
            return root;
        }
        try {
            return objectMapper.readValue(
                    Files.readString(path, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new BizException("EVENT_SOURCE_CONFIG_READ_FAILED", "event source config read failed");
        }
    }

    public void writeRootConfig(Path path, Map<String, Object> root) {
        try {
            Files.createDirectories(path.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(path, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("EVENT_SOURCE_CONFIG_SAVE_FAILED", "event source config save failed");
        }
    }

    public List<Map<String, Object>> readSourceMaps(Object value) {
        if (!(value instanceof List<?> rawSources)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object rawSource : rawSources) {
            if (rawSource instanceof Map<?, ?> rawMap) {
                result.add(new LinkedHashMap<>(objectMapper.convertValue(
                        rawMap,
                        new TypeReference<LinkedHashMap<String, Object>>() {}
                )));
            }
        }
        return result;
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

        candidates.add(userDir.resolve("ai-config").resolve("event-source-configs.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("event-source-configs.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("event-source-configs.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("event-source-configs.json").normalize());
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
