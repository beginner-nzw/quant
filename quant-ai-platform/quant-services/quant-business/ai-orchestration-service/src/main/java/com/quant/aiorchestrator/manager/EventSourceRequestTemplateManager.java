package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class EventSourceRequestTemplateManager {

    private static final Pattern SINGLE_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([\\w.-]+)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");
    private static final Pattern TRAILING_EXCHANGE_PATTERN = Pattern.compile("(?i)(?:\\.|_)?(SH|SZ|BJ|HK)$");
    private static final Pattern LEADING_EXCHANGE_PATTERN = Pattern.compile("(?i)^(SH|SZ|BJ|HK)");

    private final ObjectMapper objectMapper;

    public String appendQueryParams(String endpointUrl,
                                    EventSourceConfigItemVO sourceConfig,
                                    MarketEventSourceSyncDTO request,
                                    String errorCode,
                                    String errorMessage) {
        return appendQueryParams(endpointUrl, sourceConfig, request, Map.of(), errorCode, errorMessage);
    }

    public String appendQueryParams(String endpointUrl,
                                    EventSourceConfigItemVO sourceConfig,
                                    MarketEventSourceSyncDTO request,
                                    Map<String, Object> defaults,
                                    String errorCode,
                                    String errorMessage) {
        Map<String, Object> params = resolveQueryParams(sourceConfig, request, defaults, errorCode, errorMessage);
        if (params.isEmpty()) {
            return endpointUrl;
        }
        List<String> encodedParams = new ArrayList<>();
        params.forEach((key, value) -> appendParam(encodedParams, key, value == null ? null : String.valueOf(value)));
        if (encodedParams.isEmpty()) {
            return endpointUrl;
        }
        return endpointUrl + (endpointUrl.contains("?") ? "&" : "?") + String.join("&", encodedParams);
    }

    public Map<String, String> parseHeaders(String rawHeadersJson, String errorCode, String parseErrorMessage, String objectErrorMessage) {
        String value = trimToNull(rawHeadersJson);
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null || !root.isObject()) {
                throw new BizException(errorCode, objectErrorMessage);
            }
            Map<String, String> headers = new LinkedHashMap<>();
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldNode = root.get(fieldName);
                if (StringUtils.hasText(fieldName) && fieldNode != null && !fieldNode.isNull()) {
                    headers.put(fieldName.trim(), fieldNode.asText(""));
                }
            }
            return headers;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(errorCode, parseErrorMessage);
        }
    }

    public String formatMaskedHeadersJson(Map<String, String> headers) {
        return formatJsonSafely(maskSensitiveHeaders(headers));
    }

    public Object renderRequestTemplate(String rawJson,
                                        MarketEventSourceSyncDTO request,
                                        EventSourceConfigItemVO sourceConfig,
                                        String errorCode,
                                        String errorMessage) {
        return renderRequestTemplate(rawJson, request, sourceConfig, Map.of(), errorCode, errorMessage);
    }

    public Object renderRequestTemplate(String rawJson,
                                        MarketEventSourceSyncDTO request,
                                        EventSourceConfigItemVO sourceConfig,
                                        Map<String, Object> defaults,
                                        String errorCode,
                                        String errorMessage) {
        JsonNode templateNode = parseJsonNode(rawJson, errorCode, errorMessage);
        if (templateNode == null || templateNode.isNull()) {
            return null;
        }
        return renderTemplateValue(templateNode, buildTemplateVariables(request, sourceConfig, defaults));
    }

    private Map<String, Object> resolveQueryParams(EventSourceConfigItemVO sourceConfig,
                                                   MarketEventSourceSyncDTO request,
                                                   Map<String, Object> defaults,
                                                   String errorCode,
                                                   String errorMessage) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (defaults != null) {
            params.putAll(defaults);
        }
        JsonNode templateNode = parseJsonNode(sourceConfig == null ? null : sourceConfig.getRequestQueryJson(), errorCode, errorMessage);
        if (templateNode == null || !templateNode.isObject()) {
            return params;
        }
        Object rendered = renderTemplateValue(templateNode, buildTemplateVariables(request, sourceConfig, defaults));
        if (!(rendered instanceof Map<?, ?> renderedMap)) {
            return params;
        }
        renderedMap.forEach((key, value) -> params.put(String.valueOf(key), value));
        return params;
    }

    private Map<String, Object> buildTemplateVariables(MarketEventSourceSyncDTO request, EventSourceConfigItemVO sourceConfig) {
        return buildTemplateVariables(request, sourceConfig, Map.of());
    }

    private Map<String, Object> buildTemplateVariables(MarketEventSourceSyncDTO request,
                                                       EventSourceConfigItemVO sourceConfig,
                                                       Map<String, Object> defaults) {
        String targetCode = defaultValue(request == null ? null : request.getTargetCode(), "");
        String targetName = defaultValue(request == null ? null : request.getTargetName(), "");
        String normalizedTargetCode = defaultValue(normalizeTargetCode(targetCode), "");
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("sourceCode", defaultValue(sourceConfig == null ? null : sourceConfig.getSourceCode(), ""));
        variables.put("sourceName", sourceConfig == null ? null : sourceConfig.getSourceName());
        variables.put("sourceCategory", sourceConfig == null ? null : sourceConfig.getSourceCategory());
        variables.put("sourceChannel", sourceConfig == null ? null : sourceConfig.getSourceChannel());
        variables.put("targetType", defaultValue(request == null ? null : request.getTargetType(), "STOCK"));
        variables.put("targetCode", targetCode);
        variables.put("normalizedTargetCode", normalizedTargetCode);
        variables.put("targetName", targetName);
        variables.put("searchKeyword", buildSearchKeyword(targetName, normalizedTargetCode, targetCode));
        variables.put("itemCount", request == null || request.getItemCount() == null ? 10 : request.getItemCount());
        variables.put("defaultEventType", sourceConfig == null ? null : sourceConfig.getDefaultEventType());
        variables.put("defaultImpactLevel", sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel());
        if (defaults != null) {
            variables.putAll(defaults);
        }
        return variables;
    }

    private Object renderTemplateValue(JsonNode node, Map<String, Object> variables) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> result = new LinkedHashMap<>();
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                result.put(fieldName, renderTemplateValue(node.get(fieldName), variables));
            }
            return result;
        }
        if (node.isArray()) {
            List<Object> result = new ArrayList<>();
            node.forEach(item -> result.add(renderTemplateValue(item, variables)));
            return result;
        }
        if (node.isTextual()) {
            return renderTemplateString(node.asText(), variables);
        }
        return objectMapper.convertValue(node, Object.class);
    }

    private Object renderTemplateString(String template, Map<String, Object> variables) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        Matcher exactMatcher = SINGLE_PLACEHOLDER_PATTERN.matcher(template);
        if (exactMatcher.matches()) {
            return variables.get(exactMatcher.group(1));
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object value = variables.get(matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private JsonNode parseJsonNode(String rawJson, String errorCode, String errorMessage) {
        String value = trimToNull(rawJson);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            throw new BizException(errorCode, errorMessage);
        }
    }

    private String buildSearchKeyword(String targetName, String normalizedTargetCode, String rawTargetCode) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(targetName)) {
            parts.add(targetName.trim());
        }
        if (StringUtils.hasText(normalizedTargetCode)) {
            parts.add(normalizedTargetCode.trim());
        } else if (StringUtils.hasText(rawTargetCode)) {
            parts.add(rawTargetCode.trim());
        }
        return String.join(" ", parts).trim();
    }

    private String normalizeTargetCode(String rawTargetCode) {
        String value = trimToNull(rawTargetCode);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = TRAILING_EXCHANGE_PATTERN.matcher(value).replaceFirst("");
        normalized = LEADING_EXCHANGE_PATTERN.matcher(normalized).replaceFirst("");
        normalized = normalized.replaceAll("[^0-9A-Za-z]", "");
        return StringUtils.hasText(normalized) ? normalized : value;
    }

    private void appendParam(List<String> params, String key, String value) {
        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
            params.add(URLEncoder.encode(key.trim(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
        }
    }

    private String formatJsonSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Map<String, String> maskSensitiveHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, String> masked = new LinkedHashMap<>();
        headers.forEach((key, value) -> masked.put(key, isSensitiveHeader(key) ? "******" : value));
        return masked;
    }

    private boolean isSensitiveHeader(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("authorization")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("api-key")
                || normalized.contains("apikey")
                || normalized.contains("signature");
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
