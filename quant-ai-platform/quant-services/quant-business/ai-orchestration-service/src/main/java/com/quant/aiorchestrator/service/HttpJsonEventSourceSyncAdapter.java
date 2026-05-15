package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
public class HttpJsonEventSourceSyncAdapter implements EventSourceSyncAdapter {

    private static final Pattern SINGLE_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([\\w.-]+)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig != null
                && StringUtils.hasText(sourceConfig.getIngestMode())
                && "HTTP_JSON".equalsIgnoreCase(sourceConfig.getIngestMode().trim());
    }

    @Override
    public List<MarketEventCreateDTO> sync(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getEndpointUrl());
        if (!StringUtils.hasText(endpointUrl)) {
            throw new BizException("EVENT_SOURCE_ENDPOINT_URL_EMPTY", "HTTP 事件源地址不能为空");
        }

        String requestMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getRequestMethod(), "GET").toUpperCase(Locale.ROOT);
        int timeoutSeconds = sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();

        HttpRequest httpRequest = buildRequest(endpointUrl, requestMethod, timeoutSeconds, request, sourceConfig);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("EVENT_SOURCE_HTTP_FAILED", "事件源同步失败，HTTP 状态码: " + response.statusCode());
            }
            return parseResponse(response.body(), sourceConfig, request);
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("EVENT_SOURCE_HTTP_REQUEST_FAILED", "事件源 HTTP 请求失败");
        }
    }

    @Override
    public List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getEndpointUrl());
        if (!StringUtils.hasText(endpointUrl)) {
            throw new BizException("EVENT_SOURCE_ENDPOINT_URL_EMPTY", "HTTP 事件源地址不能为空");
        }

        String requestMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getRequestMethod(), "GET").toUpperCase(Locale.ROOT);
        int timeoutSeconds = sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        String method = "POST".equalsIgnoreCase(requestMethod) ? "POST" : "GET";

        EventSourceRequestDiagnosticItemVO item = new EventSourceRequestDiagnosticItemVO();
        item.setStageCode("PRIMARY_REQUEST");
        item.setStageName("主请求");
        item.setRequestMethod(method);
        item.setRequestTimeoutSeconds(timeoutSeconds);
        item.setRequestUrl("POST".equals(method) ? endpointUrl : appendQueryParams(endpointUrl, sourceConfig, request));
        item.setRequestHeadersJson(formatJsonSafely(maskSensitiveHeaders(parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson()))));
        if ("POST".equals(method)) {
            item.setRequestBodyJson(formatJsonSafely(resolveRequestBody(sourceConfig, request)));
        }
        return List.of(item);
    }

    private HttpRequest buildRequest(String endpointUrl,
                                     String requestMethod,
                                     int timeoutSeconds,
                                     MarketEventSourceSyncDTO request,
                                     EventSourceConfigItemVO sourceConfig) {
        String method = "POST".equalsIgnoreCase(requestMethod) ? "POST" : "GET";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson()));

        if ("POST".equals(method)) {
            Object bodyObject = resolveRequestBody(sourceConfig, request);
            String body;
            try {
                body = objectMapper.writeValueAsString(bodyObject);
            } catch (Exception e) {
                throw new BizException("EVENT_SOURCE_REQUEST_SERIALIZE_FAILED", "事件源同步请求序列化失败");
            }
            return builder
                    .uri(URI.create(endpointUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
        }

        return builder
                .uri(URI.create(appendQueryParams(endpointUrl, sourceConfig, request)))
                .GET()
                .build();
    }

    private Object resolveRequestBody(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        Map<String, Object> defaults = buildDefaultPayload(request, sourceConfig);
        JsonNode templateNode = parseJsonNode(
                sourceConfig == null ? null : sourceConfig.getRequestBodyJson(),
                "EVENT_SOURCE_REQUEST_BODY_INVALID",
                "事件源请求体配置解析失败"
        );
        if (templateNode == null || templateNode.isNull()) {
            return defaults;
        }
        Object rendered = renderTemplateValue(templateNode, buildTemplateVariables(request, sourceConfig));
        if (rendered instanceof Map<?, ?> renderedMap) {
            Map<String, Object> merged = new LinkedHashMap<>(defaults);
            renderedMap.forEach((key, value) -> merged.put(String.valueOf(key), value));
            return merged;
        }
        return rendered;
    }

    private String appendQueryParams(String endpointUrl,
                                     EventSourceConfigItemVO sourceConfig,
                                     MarketEventSourceSyncDTO request) {
        Map<String, Object> params = resolveQueryParams(sourceConfig, request);
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

    private Map<String, Object> resolveQueryParams(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        Map<String, Object> params = buildDefaultPayload(request, sourceConfig);
        JsonNode templateNode = parseJsonNode(
                sourceConfig == null ? null : sourceConfig.getRequestQueryJson(),
                "EVENT_SOURCE_REQUEST_QUERY_INVALID",
                "事件源查询参数配置解析失败"
        );
        if (templateNode == null || !templateNode.isObject()) {
            return params;
        }
        Object rendered = renderTemplateValue(templateNode, buildTemplateVariables(request, sourceConfig));
        if (rendered instanceof Map<?, ?> renderedMap) {
            renderedMap.forEach((key, value) -> params.put(String.valueOf(key), value));
        }
        return params;
    }

    private Map<String, Object> buildDefaultPayload(MarketEventSourceSyncDTO request, EventSourceConfigItemVO sourceConfig) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceCode", defaultValue(sourceConfig == null ? null : sourceConfig.getSourceCode(), ""));
        payload.put("targetType", defaultValue(request == null ? null : request.getTargetType(), "STOCK"));
        payload.put("targetCode", defaultValue(request == null ? null : request.getTargetCode(), ""));
        payload.put("targetName", defaultValue(request == null ? null : request.getTargetName(), ""));
        payload.put("itemCount", request == null || request.getItemCount() == null ? 10 : request.getItemCount());
        payload.put("defaultEventType", sourceConfig == null ? null : sourceConfig.getDefaultEventType());
        payload.put("defaultImpactLevel", sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel());
        payload.put("sourceChannel", sourceConfig == null ? null : sourceConfig.getSourceChannel());
        return payload;
    }

    private Map<String, Object> buildTemplateVariables(MarketEventSourceSyncDTO request, EventSourceConfigItemVO sourceConfig) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("sourceCode", defaultValue(sourceConfig == null ? null : sourceConfig.getSourceCode(), ""));
        variables.put("sourceName", sourceConfig == null ? null : sourceConfig.getSourceName());
        variables.put("sourceCategory", sourceConfig == null ? null : sourceConfig.getSourceCategory());
        variables.put("sourceChannel", sourceConfig == null ? null : sourceConfig.getSourceChannel());
        variables.put("targetType", defaultValue(request == null ? null : request.getTargetType(), "STOCK"));
        variables.put("targetCode", defaultValue(request == null ? null : request.getTargetCode(), ""));
        variables.put("targetName", defaultValue(request == null ? null : request.getTargetName(), ""));
        variables.put("itemCount", request == null || request.getItemCount() == null ? 10 : request.getItemCount());
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

    private void appendParam(List<String> params, String key, String value) {
        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
            params.add(URLEncoder.encode(key.trim(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
        }
    }

    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (builder == null || headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach((key, value) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                builder.header(key.trim(), value.trim());
            }
        });
    }

    private Map<String, String> parseHeaders(String rawHeadersJson) {
        String value = trimToNull(rawHeadersJson);
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null || !root.isObject()) {
                throw new BizException("EVENT_SOURCE_REQUEST_HEADERS_INVALID", "事件源请求头配置不是合法 JSON 对象");
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
            throw new BizException("EVENT_SOURCE_REQUEST_HEADERS_INVALID", "事件源请求头配置解析失败");
        }
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

    private List<MarketEventCreateDTO> parseResponse(String responseBody,
                                                     EventSourceConfigItemVO sourceConfig,
                                                     MarketEventSourceSyncDTO request) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode itemsNode = resolveItemsNode(root, trimToNull(sourceConfig == null ? null : sourceConfig.getResponseItemsField()));
            if (itemsNode == null || !itemsNode.isArray()) {
                throw new BizException("EVENT_SOURCE_RESPONSE_ITEMS_EMPTY", "事件源响应中未找到事件列表");
            }

            List<MarketEventCreateDTO> result = new ArrayList<>();
            for (JsonNode itemNode : itemsNode) {
                if (itemNode != null && itemNode.isObject()) {
                    result.add(toMarketEvent(itemNode, sourceConfig, request));
                }
            }
            if (result.isEmpty()) {
                throw new BizException("EVENT_SOURCE_RESPONSE_ITEMS_EMPTY", "事件源未返回可导入记录");
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("EVENT_SOURCE_RESPONSE_PARSE_FAILED", "事件源响应解析失败");
        }
    }

    private JsonNode resolveItemsNode(JsonNode root, String responseItemsField) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (StringUtils.hasText(responseItemsField)) {
            JsonNode node = resolvePath(root, responseItemsField);
            if (node != null && node.isArray()) {
                return node;
            }
        }
        for (String path : List.of("items", "data.items", "data.list", "data.records", "data", "records", "list", "result.items", "result.list")) {
            JsonNode node = resolvePath(root, path);
            if (node != null && node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private JsonNode resolvePath(JsonNode root, String path) {
        if (root == null || !StringUtils.hasText(path)) {
            return null;
        }
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (!StringUtils.hasText(segment) || current == null || current.isNull()) {
                return null;
            }
            current = current.path(segment.trim());
        }
        return current == null || current.isMissingNode() || current.isNull() ? null : current;
    }

    private MarketEventCreateDTO toMarketEvent(JsonNode itemNode,
                                               EventSourceConfigItemVO sourceConfig,
                                               MarketEventSourceSyncDTO request) {
        Map<String, List<String>> fieldMappings = parseFieldMappings(sourceConfig == null ? null : sourceConfig.getFieldMappingJson());
        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        dto.setTargetType(defaultValue(readMappedText(itemNode, fieldMappings, "targetType", "targetType", "assetType", "symbolType"), defaultValue(request == null ? null : request.getTargetType(), "STOCK")));
        dto.setTargetCode(defaultValue(readMappedText(itemNode, fieldMappings, "targetCode", "targetCode", "code", "symbol", "ticker", "stockCode", "secCode"), request == null ? null : request.getTargetCode()));
        dto.setTargetName(defaultValue(readMappedText(itemNode, fieldMappings, "targetName", "targetName", "name", "stockName", "securityName", "shortName"), request == null ? null : request.getTargetName()));
        dto.setEventType(defaultValue(readMappedText(itemNode, fieldMappings, "eventType", "eventType", "type", "eventCategory"), sourceConfig == null ? null : sourceConfig.getDefaultEventType()));
        dto.setEventTitle(defaultValue(readMappedText(itemNode, fieldMappings, "eventTitle", "eventTitle", "title", "headline", "subject", "announcementTitle"), "未命名事件"));
        dto.setEventSummary(defaultValue(readMappedText(itemNode, fieldMappings, "eventSummary", "eventSummary", "summary", "content", "abstract", "description"), dto.getEventTitle()));
        dto.setSourceChannel(defaultValue(readMappedText(itemNode, fieldMappings, "sourceChannel", "sourceChannel", "channel", "source"), sourceConfig == null ? null : sourceConfig.getSourceChannel()));
        dto.setSourceUrl(readMappedText(itemNode, fieldMappings, "sourceUrl", "sourceUrl", "url", "link"));
        dto.setImpactLevel(defaultValue(readMappedText(itemNode, fieldMappings, "impactLevel", "impactLevel", "level", "priority"), sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel()));
        dto.setEventStatus(defaultValue(readMappedText(itemNode, fieldMappings, "eventStatus", "eventStatus", "status"), "ACTIVE"));
        dto.setOccurredAt(parseDateTime(readMappedText(itemNode, fieldMappings, "occurredAt", "occurredAt", "occurredTime", "eventTime", "publishTime", "time", "createdAt", "disclosureTime")));
        return dto;
    }

    private String readMappedText(JsonNode itemNode,
                                  Map<String, List<String>> fieldMappings,
                                  String canonicalField,
                                  String... fallbackFieldNames) {
        List<String> mappingFields = fieldMappings.getOrDefault(canonicalField, List.of());
        if (!mappingFields.isEmpty()) {
            String mappedValue = readText(itemNode, mappingFields.toArray(String[]::new));
            if (StringUtils.hasText(mappedValue)) {
                return mappedValue;
            }
        }
        return readText(itemNode, fallbackFieldNames);
    }

    private Map<String, List<String>> parseFieldMappings(String rawFieldMappingJson) {
        String value = trimToNull(rawFieldMappingJson);
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null || !root.isObject()) {
                throw new BizException("EVENT_SOURCE_FIELD_MAPPING_INVALID", "响应字段映射配置不是合法 JSON 对象");
            }
            Map<String, List<String>> mappings = new LinkedHashMap<>();
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldNode = root.get(fieldName);
                if (!StringUtils.hasText(fieldName) || fieldNode == null || fieldNode.isNull()) {
                    continue;
                }
                List<String> rawFields = new ArrayList<>();
                if (fieldNode.isTextual()) {
                    rawFields.add(fieldNode.asText(""));
                } else if (fieldNode.isArray()) {
                    for (JsonNode item : fieldNode) {
                        if (item != null && item.isTextual() && StringUtils.hasText(item.asText())) {
                            rawFields.add(item.asText().trim());
                        }
                    }
                }
                List<String> normalizedFields = rawFields.stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .distinct()
                        .toList();
                if (!normalizedFields.isEmpty()) {
                    mappings.put(fieldName.trim(), normalizedFields);
                }
            }
            return mappings;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("EVENT_SOURCE_FIELD_MAPPING_INVALID", "响应字段映射配置解析失败");
        }
    }

    private String readText(JsonNode itemNode, String... fieldNames) {
        if (itemNode == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (!StringUtils.hasText(fieldName)) {
                continue;
            }
            JsonNode valueNode = itemNode.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText(null);
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String rawValue) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return LocalDateTime.now();
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
}
