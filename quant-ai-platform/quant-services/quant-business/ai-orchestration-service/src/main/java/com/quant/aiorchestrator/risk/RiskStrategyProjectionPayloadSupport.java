package com.quant.aiorchestrator.risk;

import com.quant.common.model.message.AiTaskResultMessage;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RiskStrategyProjectionPayloadSupport {

    private RiskStrategyProjectionPayloadSupport() {
    }

    static Object approvedPayloadValue(AiTaskResultMessage.ResultPayload payload, String key) {
        Map<String, Object> approvedPayloadMap = approvedPayloadMap(payload);
        if (approvedPayloadMap == null) {
            return null;
        }
        return approvedPayloadMap.get(key);
    }

    static boolean hasApprovedPayload(AiTaskResultMessage.ResultPayload payload) {
        return approvedPayloadMap(payload) != null;
    }

    private static Map<String, Object> approvedPayloadMap(AiTaskResultMessage.ResultPayload payload) {
        Map<String, Object> reportMeta = payload.getReportMeta();
        if (reportMeta == null) {
            return null;
        }
        Object approvedPayload = reportMeta.get("approvedPayload");
        if (!(approvedPayload instanceof Map<?, ?> approvedPayloadMap)) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        approvedPayloadMap.forEach((rawKey, rawValue) -> {
            String normalizedKey = normalizeText(rawKey);
            if (StringUtils.hasText(normalizedKey)) {
                result.put(normalizedKey, rawValue);
            }
        });
        return result;
    }

    static Map<String, Object> approvedObjectMap(AiTaskResultMessage.ResultPayload payload, String key) {
        Object value = approvedPayloadValue(payload, key);
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        rawMap.forEach((rawKey, rawValue) -> {
            String normalizedKey = normalizeText(rawKey);
            if (StringUtils.hasText(normalizedKey)) {
                result.put(normalizedKey, rawValue);
            }
        });
        return result;
    }

    static List<Map<String, Object>> approvedObjectList(AiTaskResultMessage.ResultPayload payload, String key) {
        Object value = approvedPayloadValue(payload, key);
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> normalizedMap = new LinkedHashMap<>();
            rawMap.forEach((rawKey, rawValue) -> {
                String normalizedKey = normalizeText(rawKey);
                if (StringUtils.hasText(normalizedKey)) {
                    normalizedMap.put(normalizedKey, rawValue);
                }
            });
            if (!normalizedMap.isEmpty()) {
                result.add(normalizedMap);
            }
        }
        return result;
    }

    static List<String> approvedTextList(AiTaskResultMessage.ResultPayload payload, String key) {
        return normalizeTextList(approvedPayloadValue(payload, key));
    }

    static List<String> mergeTextList(List<String> left, List<String> right) {
        List<String> result = new ArrayList<>();
        result.addAll(left);
        for (String item : right) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    static List<String> normalizeTextList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : values) {
            String text = normalizeText(item);
            if (StringUtils.hasText(text) && !result.contains(text)) {
                result.add(text);
            }
        }
        return result;
    }

    static String resolveFirstText(List<String> values, String fallback) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return fallback;
    }

    static String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return null;
    }

    static String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    static String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || maxLength <= 0) {
            return value;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
