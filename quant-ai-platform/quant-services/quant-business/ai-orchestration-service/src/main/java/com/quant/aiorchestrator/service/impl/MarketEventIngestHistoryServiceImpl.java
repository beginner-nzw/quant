package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.service.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MarketEventIngestHistoryServiceImpl implements MarketEventIngestHistoryService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String eventIngestHistoryPath;
    private final ObjectMapper objectMapper;

    public MarketEventIngestHistoryServiceImpl(
            @Value("${quant.ai.event-ingest-history:../../../ai-config/event-ingest-histories.json}") String eventIngestHistoryPath,
            ObjectMapper objectMapper
    ) {
        this.eventIngestHistoryPath = eventIngestHistoryPath;
        this.objectMapper = objectMapper;
    }

    public void appendHistory(String sourceType,
                              String sourceLabel,
                              String sourceCode,
                              String sourceName,
                              String sourceCategory,
                              String sourceChannel,
                              String sourceDetail,
                              Integer totalCount,
                              Integer successCount,
                              Integer failedCount,
                              Integer duplicateCount,
                              Integer autoTriggeredCount,
                              String summary) {
        appendHistory(
                sourceType,
                sourceLabel,
                sourceCode,
                sourceName,
                sourceCategory,
                sourceChannel,
                sourceDetail,
                totalCount,
                successCount,
                failedCount,
                duplicateCount,
                autoTriggeredCount,
                resolveResultStatus(successCount, failedCount),
                null,
                summary
        );
    }

    public void appendHistory(String sourceType,
                              String sourceLabel,
                              String sourceCode,
                              String sourceName,
                              String sourceCategory,
                              String sourceChannel,
                              String sourceDetail,
                              Integer totalCount,
                              Integer successCount,
                              Integer failedCount,
                              Integer duplicateCount,
                              Integer autoTriggeredCount,
                              String resultStatus,
                              String errorMessage,
                              String summary) {
        List<Map<String, Object>> items = readHistoryItems();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("historyId", UUID.randomUUID().toString());
        item.put("sourceType", normalize(sourceType));
        item.put("sourceLabel", normalize(sourceLabel));
        item.put("sourceCode", normalize(sourceCode));
        item.put("sourceName", normalize(sourceName));
        item.put("sourceCategory", normalize(sourceCategory));
        item.put("sourceChannel", normalize(sourceChannel));
        item.put("sourceDetail", normalize(sourceDetail));
        item.put("totalCount", totalCount == null ? 0 : totalCount);
        item.put("successCount", successCount == null ? 0 : successCount);
        item.put("failedCount", failedCount == null ? 0 : failedCount);
        item.put("duplicateCount", duplicateCount == null ? 0 : duplicateCount);
        item.put("autoTriggeredCount", autoTriggeredCount == null ? 0 : autoTriggeredCount);
        item.put("resultStatus", normalize(resultStatus));
        item.put("errorMessage", normalize(errorMessage));
        item.put("operatorId", normalize(SecurityUtils.currentUserId()) == null ? "unknown" : normalize(SecurityUtils.currentUserId()));
        item.put("operatorRole", normalize(SecurityUtils.currentUserRole()) == null ? "UNKNOWN" : normalize(SecurityUtils.currentUserRole()));
        item.put("summary", normalize(summary));
        item.put("createdAt", DATE_TIME_FORMATTER.format(LocalDateTime.now()));
        items.add(0, item);
        if (items.size() > 100) {
            items = new ArrayList<>(items.subList(0, 100));
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("items", items);
        Path historyPath = resolveHistoryPath();
        try {
            Files.createDirectories(historyPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(historyPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("MARKET_EVENT_INGEST_HISTORY_SAVE_FAILED", "保存事件接入历史失败");
        }
    }

    public List<MarketEventIngestHistoryItemVO> loadRecentHistory() {
        List<Map<String, Object>> items = readHistoryItems();
        List<MarketEventIngestHistoryItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            MarketEventIngestHistoryItemVO vo = new MarketEventIngestHistoryItemVO();
            vo.setHistoryId(normalize(item.get("historyId")));
            vo.setSourceType(normalize(item.get("sourceType")));
            vo.setSourceLabel(normalize(item.get("sourceLabel")));
            vo.setSourceCode(normalize(item.get("sourceCode")));
            vo.setSourceName(normalize(item.get("sourceName")));
            vo.setSourceCategory(normalize(item.get("sourceCategory")));
            vo.setSourceChannel(normalize(item.get("sourceChannel")));
            vo.setSourceDetail(normalize(item.get("sourceDetail")));
            vo.setTotalCount(toInteger(item.get("totalCount")));
            vo.setSuccessCount(toInteger(item.get("successCount")));
            vo.setFailedCount(toInteger(item.get("failedCount")));
            vo.setDuplicateCount(toInteger(item.get("duplicateCount")));
            vo.setAutoTriggeredCount(toInteger(item.get("autoTriggeredCount")));
            vo.setResultStatus(normalize(item.get("resultStatus")));
            vo.setErrorMessage(normalize(item.get("errorMessage")));
            vo.setOperatorId(normalize(item.get("operatorId")));
            vo.setOperatorRole(normalize(item.get("operatorRole")));
            vo.setSummary(normalize(item.get("summary")));
            vo.setCreatedAt(normalize(item.get("createdAt")));
            result.add(vo);
        }
        return result;
    }

    private List<Map<String, Object>> readHistoryItems() {
        Path historyPath = resolveHistoryPath();
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
            throw new BizException("MARKET_EVENT_INGEST_HISTORY_READ_FAILED", "读取事件接入历史失败");
        }
    }

    private Path resolveHistoryPath() {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(eventIngestHistoryPath);
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

    private Integer toInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveResultStatus(Integer successCount, Integer failedCount) {
        int success = successCount == null ? 0 : successCount;
        int failed = failedCount == null ? 0 : failedCount;
        if (failed > 0 && success > 0) {
            return "PARTIAL_SUCCESS";
        }
        if (failed > 0) {
            return "FAILED";
        }
        return "SUCCESS";
    }
}
