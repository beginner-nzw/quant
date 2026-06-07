package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.MarketEventIngestRunDO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.common.security.SecurityUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MarketEventIngestHistoryItemManager {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Map<String, Object> buildHistoryItem(String sourceType,
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
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("historyId", UUID.randomUUID().toString());
        item.put("sourceType", normalize(sourceType));
        item.put("sourceLabel", normalize(sourceLabel));
        item.put("sourceCode", normalize(sourceCode));
        item.put("sourceName", normalize(sourceName));
        item.put("sourceCategory", normalize(sourceCategory));
        item.put("sourceChannel", normalize(sourceChannel));
        item.put("sourceDetail", normalize(sourceDetail));
        item.put("fetchStatus", defaultValue(extractSourceDetailValue(sourceDetail, "status"), normalize(resultStatus)));
        item.put("rawPayloadRef", extractSourceDetailValue(sourceDetail, "rawPayloadRef"));
        item.put("retryCount", extractRetryCount(sourceDetail, summary));
        item.put("deadlettered", "DEADLETTERED".equalsIgnoreCase(normalize(resultStatus)));
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
        return item;
    }

    public MarketEventIngestHistoryItemVO toHistoryItem(Map<String, Object> item) {
        MarketEventIngestHistoryItemVO vo = new MarketEventIngestHistoryItemVO();
        vo.setHistoryId(normalize(item.get("historyId")));
        vo.setSourceType(normalize(item.get("sourceType")));
        vo.setSourceLabel(normalize(item.get("sourceLabel")));
        vo.setSourceCode(normalize(item.get("sourceCode")));
        vo.setSourceName(normalize(item.get("sourceName")));
        vo.setSourceCategory(normalize(item.get("sourceCategory")));
        vo.setSourceChannel(normalize(item.get("sourceChannel")));
        vo.setSourceDetail(normalize(item.get("sourceDetail")));
        vo.setFetchStatus(normalize(item.get("fetchStatus")));
        vo.setRawPayloadRef(normalize(item.get("rawPayloadRef")));
        vo.setRetryCount(toInteger(item.get("retryCount")));
        vo.setDeadlettered(Boolean.TRUE.equals(item.get("deadlettered"))
                || "true".equalsIgnoreCase(normalize(item.get("deadlettered"))));
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
        return vo;
    }

    public MarketEventIngestHistoryItemVO toHistoryItem(MarketEventIngestRunDO run) {
        MarketEventIngestHistoryItemVO vo = new MarketEventIngestHistoryItemVO();
        vo.setHistoryId(run.getIngestRunId());
        vo.setSourceType("SOURCE_SYNC");
        vo.setSourceLabel(defaultValue(run.getSourceName(), "data ingest source"));
        vo.setSourceCode(run.getSourceCode());
        vo.setSourceName(run.getSourceName());
        vo.setSourceCategory(run.getSourceCategory());
        vo.setSourceChannel(run.getSourceChannel());
        vo.setSourceDetail("data-ingest source=" + defaultValue(run.getSourceCode(), "UNKNOWN")
                + " mode=" + defaultValue(run.getIngestMode(), "UNKNOWN")
                + " target=" + defaultValue(run.getRequestTarget(), "UNKNOWN")
                + " status=" + defaultValue(run.getFetchStatus(), "UNKNOWN")
                + " rawPayloadRef=" + defaultValue(run.getRawPayloadRef(), "NONE"));
        vo.setFetchStatus(run.getFetchStatus());
        vo.setRawPayloadRef(run.getRawPayloadRef());
        vo.setRetryCount(run.getRetryCount());
        vo.setDeadlettered(run.getDeadlettered() != null && run.getDeadlettered() == 1);
        vo.setTotalCount(run.getTotalCount());
        vo.setSuccessCount(run.getSuccessCount());
        vo.setFailedCount(run.getFailedCount());
        vo.setDuplicateCount(run.getDuplicateCount());
        vo.setAutoTriggeredCount(run.getAutoTriggeredCount());
        vo.setResultStatus(resolveHistoryResultStatus(run));
        vo.setErrorMessage(run.getErrorMessage());
        vo.setSummary(run.getErrorMessage());
        vo.setCreatedAt(run.getCreatedAt() == null ? null : DATE_TIME_FORMATTER.format(run.getCreatedAt()));
        return vo;
    }

    public String resolveResultStatus(Integer successCount, Integer failedCount) {
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

    private String resolveHistoryResultStatus(MarketEventIngestRunDO run) {
        if (run == null) {
            return null;
        }
        if (run.getDeadlettered() != null && run.getDeadlettered() == 1) {
            return "DEADLETTERED";
        }
        if ("STANDARDIZED".equalsIgnoreCase(run.getFetchStatus())) {
            int failed = run.getFailedCount() == null ? 0 : run.getFailedCount();
            int success = run.getSuccessCount() == null ? 0 : run.getSuccessCount();
            return resolveResultStatus(success, failed);
        }
        return run.getFetchStatus();
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

    private String defaultValue(String value, String fallback) {
        return normalize(value) == null ? fallback : normalize(value);
    }

    private String extractSourceDetailValue(String sourceDetail, String key) {
        String normalized = normalize(sourceDetail);
        if (normalized == null || key == null) {
            return null;
        }
        String marker = key + "=";
        int start = normalized.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int valueStart = start + marker.length();
        int valueEnd = normalized.indexOf(' ', valueStart);
        String value = valueEnd < 0 ? normalized.substring(valueStart) : normalized.substring(valueStart, valueEnd);
        return normalize("NONE".equalsIgnoreCase(value) ? null : value);
    }

    private Integer extractRetryCount(String sourceDetail, String summary) {
        String value = extractSourceDetailValue(sourceDetail, "retry");
        if (value == null) {
            value = extractSourceDetailValue(summary, "retry");
        }
        if (value == null || !value.contains("/")) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(0, value.indexOf('/')).trim());
        } catch (Exception ignored) {
            return 0;
        }
    }
}
