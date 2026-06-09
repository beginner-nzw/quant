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
public class MarketEventIngestHistoryFileManager {

    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> readHistoryItems(String historyPathConfig) {
        Path historyPath = resolveHistoryPath(historyPathConfig);
        if (!Files.exists(historyPath)) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    Files.readString(historyPath, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
            Object items = root.get("items");
            if (!(items instanceof List<?> itemList)) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : itemList) {
                if (item instanceof Map<?, ?> rawItem) {
                    result.add(new LinkedHashMap<>(objectMapper.convertValue(
                            rawItem,
                            new TypeReference<LinkedHashMap<String, Object>>() {}
                    )));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BizException("MARKET_EVENT_INGEST_HISTORY_READ_FAILED", "market event ingest history read failed");
        }
    }

    public void writeHistoryItems(String historyPathConfig, List<Map<String, Object>> items) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("items", items);
        Path historyPath = resolveHistoryPath(historyPathConfig);
        try {
            Files.createDirectories(historyPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(historyPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("MARKET_EVENT_INGEST_HISTORY_SAVE_FAILED", "market event ingest history save failed");
        }
    }

    public Path resolveHistoryPath(String historyPathConfig) {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(historyPathConfig);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("ai-config").resolve("event-ingest-histories.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("event-ingest-histories.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("event-ingest-histories.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("event-ingest-histories.json").normalize());
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
