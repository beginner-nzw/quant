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
public class CninfoProxyUpstreamPayloadManager {

    private static final String SOURCE_CODE = "CNINFO_ANNOUNCEMENT_PROXY";
    private static final Pattern SINGLE_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([\\w.-]+)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");

    private final ObjectMapper objectMapper;

    public Object resolveUpstreamBody(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO dto) {
        Map<String, Object> defaults = buildDefaultPayload(dto, sourceConfig);
        JsonNode templateNode = parseJsonNode(
                sourceConfig == null ? null : sourceConfig.getUpstreamBodyJson(),
                "CNINFO_PROXY_UPSTREAM_BODY_INVALID",
                "巨潮上游请求体配置解析失败"
        );
        if (templateNode == null || templateNode.isNull()) {
            return defaults;
        }
        Object rendered = renderTemplateValue(templateNode, buildTemplateVariables(dto, sourceConfig));
        if (rendered instanceof Map<?, ?> renderedMap) {
            Map<String, Object> merged = new LinkedHashMap<>(defaults);
            renderedMap.forEach((key, value) -> merged.put(String.valueOf(key), value));
            return merged;
        }
        return rendered;
    }

    public String appendQueryParams(String upstreamUrl,
                                    EventSourceConfigItemVO sourceConfig,
                                    MarketEventSourceSyncDTO dto) {
        Map<String, Object> params = resolveUpstreamQueryParams(sourceConfig, dto);
        if (params.isEmpty()) {
            return upstreamUrl;
        }
        List<String> encodedParams = new ArrayList<>();
        params.forEach((key, value) -> appendParam(encodedParams, key, value == null ? null : String.valueOf(value)));
        if (encodedParams.isEmpty()) {
            return upstreamUrl;
        }
        return upstreamUrl + (upstreamUrl.contains("?") ? "&" : "?") + String.join("&", encodedParams);
    }

    public Map<String, String> parseHeaders(String rawHeadersJson) {
        String value = trimToNull(rawHeadersJson);
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null || !root.isObject()) {
                throw new BizException("CNINFO_PROXY_UPSTREAM_HEADERS_INVALID", "巨潮上游请求头配置不是合法 JSON 对象");
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
            throw new BizException("CNINFO_PROXY_UPSTREAM_HEADERS_INVALID", "巨潮上游请求头配置解析失败");
        }
    }

    public String formatJsonSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    public Map<String, String> maskSensitiveHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, String> masked = new LinkedHashMap<>();
        headers.forEach((key, value) -> masked.put(key, isSensitiveHeader(key) ? "******" : value));
        return masked;
    }

    private Map<String, Object> resolveUpstreamQueryParams(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO dto) {
        Map<String, Object> params = buildDefaultPayload(dto, sourceConfig);
        JsonNode templateNode = parseJsonNode(
                sourceConfig == null ? null : sourceConfig.getUpstreamQueryJson(),
                "CNINFO_PROXY_UPSTREAM_QUERY_INVALID",
                "巨潮上游查询参数配置解析失败"
        );
        if (templateNode == null || !templateNode.isObject()) {
            return params;
        }
        Object rendered = renderTemplateValue(templateNode, buildTemplateVariables(dto, sourceConfig));
        if (rendered instanceof Map<?, ?> renderedMap) {
            renderedMap.forEach((key, value) -> params.put(String.valueOf(key), value));
        }
        return params;
    }

    private Map<String, Object> buildDefaultPayload(MarketEventSourceSyncDTO dto, EventSourceConfigItemVO sourceConfig) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceCode", SOURCE_CODE);
        payload.put("targetType", defaultValue(dto == null ? null : dto.getTargetType(), "STOCK"));
        payload.put("targetCode", defaultValue(dto == null ? null : dto.getTargetCode(), ""));
        payload.put("targetName", defaultValue(dto == null ? null : dto.getTargetName(), ""));
        payload.put("itemCount", dto == null || dto.getItemCount() == null ? 10 : dto.getItemCount());
        payload.put("defaultEventType", sourceConfig == null ? null : sourceConfig.getDefaultEventType());
        payload.put("defaultImpactLevel", sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel());
        payload.put("sourceChannel", sourceConfig == null ? null : sourceConfig.getSourceChannel());
        return payload;
    }

    private Map<String, Object> buildTemplateVariables(MarketEventSourceSyncDTO dto, EventSourceConfigItemVO sourceConfig) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("sourceCode", SOURCE_CODE);
        variables.put("sourceName", sourceConfig == null ? null : sourceConfig.getSourceName());
        variables.put("sourceCategory", sourceConfig == null ? null : sourceConfig.getSourceCategory());
        variables.put("sourceChannel", sourceConfig == null ? null : sourceConfig.getSourceChannel());
        variables.put("targetType", defaultValue(dto == null ? null : dto.getTargetType(), "STOCK"));
        variables.put("targetCode", defaultValue(dto == null ? null : dto.getTargetCode(), ""));
        variables.put("targetName", defaultValue(dto == null ? null : dto.getTargetName(), ""));
        variables.put("itemCount", dto == null || dto.getItemCount() == null ? 10 : dto.getItemCount());
        variables.put("defaultEventType", sourceConfig == null ? null : sourceConfig.getDefaultEventType());
        variables.put("defaultImpactLevel", sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel());
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

    private void appendParam(List<String> params, String key, String value) {
        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
            params.add(URLEncoder.encode(key.trim(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
        }
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
