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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
public class CninfoProxyEventSourceSyncAdapter implements EventSourceSyncAdapter {

    private static final Pattern SINGLE_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([\\w.-]+)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");

    private final ObjectMapper objectMapper;
    private final CninfoProxyAnnouncementService cninfoProxyAnnouncementService;

    @Override
    public boolean supports(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig != null
                && StringUtils.hasText(sourceConfig.getIngestMode())
                && "CNINFO_PROXY".equalsIgnoreCase(sourceConfig.getIngestMode().trim());
    }

    @Override
    public List<MarketEventCreateDTO> sync(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getEndpointUrl());
        if (!StringUtils.hasText(endpointUrl)) {
            throw new BizException("CNINFO_PROXY_ENDPOINT_EMPTY", "巨潮公告包装接口地址不能为空");
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
                throw new BizException("CNINFO_PROXY_HTTP_FAILED", "巨潮公告包装接口调用失败，HTTP 状态码: " + response.statusCode());
            }
            return parseResponse(response.body(), sourceConfig, request, endpointUrl);
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("CNINFO_PROXY_REQUEST_FAILED", "巨潮公告包装接口请求失败");
        }
    }

    @Override
    public List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getEndpointUrl());
        if (!StringUtils.hasText(endpointUrl)) {
            throw new BizException("CNINFO_PROXY_ENDPOINT_EMPTY", "巨潮公告包装接口地址不能为空");
        }

        String requestMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getRequestMethod(), "GET").toUpperCase(Locale.ROOT);
        int timeoutSeconds = sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        String method = "POST".equalsIgnoreCase(requestMethod) ? "POST" : "GET";

        List<EventSourceRequestDiagnosticItemVO> items = new ArrayList<>();
        EventSourceRequestDiagnosticItemVO proxyRequest = new EventSourceRequestDiagnosticItemVO();
        proxyRequest.setStageCode("PROXY_REQUEST");
        proxyRequest.setStageName("代理请求");
        proxyRequest.setRequestMethod(method);
        proxyRequest.setRequestTimeoutSeconds(timeoutSeconds);
        proxyRequest.setRequestUrl("POST".equals(method) ? endpointUrl : appendQueryParams(endpointUrl, sourceConfig, request));
        proxyRequest.setRequestHeadersJson(formatJsonSafely(maskSensitiveHeaders(parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson()))));
        if ("POST".equals(method)) {
            proxyRequest.setRequestBodyJson(formatJsonSafely(resolveRequestBody(sourceConfig, request)));
        }
        items.add(proxyRequest);

        EventSourceRequestDiagnosticItemVO upstreamRequest = cninfoProxyAnnouncementService.buildUpstreamRequestDiagnosticItem(sourceConfig, request);
        if (upstreamRequest != null) {
            items.add(upstreamRequest);
        }
        return items;
    }

    private HttpRequest buildRequest(String endpointUrl,
                                     String requestMethod,
                                     int timeoutSeconds,
                                     MarketEventSourceSyncDTO request,
                                     EventSourceConfigItemVO sourceConfig) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson()));

        if ("POST".equalsIgnoreCase(requestMethod)) {
            Object bodyObject = resolveRequestBody(sourceConfig, request);
            String body;
            try {
                body = objectMapper.writeValueAsString(bodyObject);
            } catch (Exception e) {
                throw new BizException("CNINFO_PROXY_REQUEST_SERIALIZE_FAILED", "巨潮公告包装接口请求序列化失败");
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
                "CNINFO_PROXY_REQUEST_BODY_INVALID",
                "巨潮公告请求体配置解析失败"
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
                "CNINFO_PROXY_REQUEST_QUERY_INVALID",
                "巨潮公告查询参数配置解析失败"
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
        payload.put("sourceCode", defaultValue(sourceConfig == null ? null : sourceConfig.getSourceCode(), "CNINFO_ANNOUNCEMENT_PROXY"));
        payload.put("targetType", defaultValue(request == null ? null : request.getTargetType(), "STOCK"));
        payload.put("targetCode", defaultValue(request == null ? null : request.getTargetCode(), ""));
        payload.put("targetName", defaultValue(request == null ? null : request.getTargetName(), ""));
        payload.put("itemCount", request == null || request.getItemCount() == null ? 10 : request.getItemCount());
        payload.put("defaultEventType", defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "ANNOUNCEMENT"));
        payload.put("defaultImpactLevel", defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "HIGH"));
        payload.put("sourceChannel", defaultValue(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "EXCHANGE_FEED"));
        return payload;
    }

    private Map<String, Object> buildTemplateVariables(MarketEventSourceSyncDTO request, EventSourceConfigItemVO sourceConfig) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("sourceCode", defaultValue(sourceConfig == null ? null : sourceConfig.getSourceCode(), "CNINFO_ANNOUNCEMENT_PROXY"));
        variables.put("sourceName", sourceConfig == null ? null : sourceConfig.getSourceName());
        variables.put("sourceCategory", sourceConfig == null ? null : sourceConfig.getSourceCategory());
        variables.put("sourceChannel", defaultValue(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "EXCHANGE_FEED"));
        variables.put("targetType", defaultValue(request == null ? null : request.getTargetType(), "STOCK"));
        variables.put("targetCode", defaultValue(request == null ? null : request.getTargetCode(), ""));
        variables.put("targetName", defaultValue(request == null ? null : request.getTargetName(), ""));
        variables.put("itemCount", request == null || request.getItemCount() == null ? 10 : request.getItemCount());
        variables.put("defaultEventType", defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "ANNOUNCEMENT"));
        variables.put("defaultImpactLevel", defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "HIGH"));
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
                throw new BizException("CNINFO_PROXY_REQUEST_HEADERS_INVALID", "巨潮公告请求头配置不是合法 JSON 对象");
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
            throw new BizException("CNINFO_PROXY_REQUEST_HEADERS_INVALID", "巨潮公告请求头配置解析失败");
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
                                                     MarketEventSourceSyncDTO request,
                                                     String endpointUrl) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode itemsNode = resolveItemsNode(root, trimToNull(sourceConfig == null ? null : sourceConfig.getResponseItemsField()));
            if (itemsNode == null || !itemsNode.isArray()) {
                throw new BizException("CNINFO_PROXY_ITEMS_EMPTY", "巨潮公告包装接口未返回公告列表");
            }

            List<MarketEventCreateDTO> result = new ArrayList<>();
            for (JsonNode itemNode : itemsNode) {
                if (itemNode != null && itemNode.isObject()) {
                    result.add(toMarketEvent(itemNode, sourceConfig, request, endpointUrl));
                }
            }
            if (result.isEmpty()) {
                throw new BizException("CNINFO_PROXY_ITEMS_EMPTY", "巨潮公告包装接口未返回可导入公告");
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("CNINFO_PROXY_PARSE_FAILED", "巨潮公告包装接口响应解析失败");
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
        for (String path : List.of("items", "data.items", "data.records", "records", "announcements", "data.list", "data")) {
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
                                               MarketEventSourceSyncDTO request,
                                               String endpointUrl) {
        Map<String, List<String>> fieldMappings = parseFieldMappings(sourceConfig == null ? null : sourceConfig.getFieldMappingJson());
        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        dto.setTargetType(defaultValue(readMappedText(itemNode, fieldMappings, "targetType", "targetType", "assetType"), defaultValue(request == null ? null : request.getTargetType(), "STOCK")));
        dto.setTargetCode(defaultValue(readMappedText(itemNode, fieldMappings, "targetCode", "targetCode", "secCode", "symbol", "ticker", "stockCode", "secucode"), request == null ? null : request.getTargetCode()));
        dto.setTargetName(defaultValue(readMappedText(itemNode, fieldMappings, "targetName", "targetName", "secName", "secShortName", "shortName", "stockName"), request == null ? null : request.getTargetName()));
        dto.setEventType(defaultValue(readMappedText(itemNode, fieldMappings, "eventType", "eventType", "eventCategory"), defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "ANNOUNCEMENT")));
        dto.setEventTitle(defaultValue(readMappedText(itemNode, fieldMappings, "eventTitle", "eventTitle", "announcementTitle", "title", "noticeTitle", "headline"), "未命名公告"));
        dto.setEventSummary(defaultValue(readMappedText(itemNode, fieldMappings, "eventSummary", "eventSummary", "announcementSummary", "summary", "contentAbstract", "description"), dto.getEventTitle()));
        dto.setSourceChannel(defaultValue(readMappedText(itemNode, fieldMappings, "sourceChannel", "sourceChannel", "channel"), defaultValue(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "EXCHANGE_FEED")));
        dto.setSourceUrl(resolveSourceUrl(readMappedText(itemNode, fieldMappings, "sourceUrl", "sourceUrl", "announcementUrl", "adjunctUrl", "url", "link"), endpointUrl));
        dto.setImpactLevel(defaultValue(readMappedText(itemNode, fieldMappings, "impactLevel", "impactLevel", "importance", "level"), defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "HIGH")));
        dto.setEventStatus(defaultValue(readMappedText(itemNode, fieldMappings, "eventStatus", "eventStatus", "status"), "ACTIVE"));
        dto.setOccurredAt(parseDateTime(readMappedText(itemNode, fieldMappings, "occurredAt", "occurredAt", "announcementTime", "publishTime", "announcementDate", "disclosureTime", "time")));
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
                throw new BizException("CNINFO_PROXY_FIELD_MAPPING_INVALID", "巨潮公告字段映射配置不是合法 JSON 对象");
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
            throw new BizException("CNINFO_PROXY_FIELD_MAPPING_INVALID", "巨潮公告字段映射配置解析失败");
        }
    }

    private String resolveSourceUrl(String rawUrl, String endpointUrl) {
        String value = trimToNull(rawUrl);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        try {
            URI baseUri = URI.create(endpointUrl);
            return baseUri.resolve(value).toString();
        } catch (Exception ignored) {
            return value;
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
        if (value.chars().allMatch(Character::isDigit)) {
            try {
                long timestamp = Long.parseLong(value);
                if (value.length() == 10) {
                    timestamp = timestamp * 1000;
                }
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            } catch (Exception ignored) {
            }
        }
        List<DateTimeFormatter> dateTimeFormatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        List<DateTimeFormatter> dateFormatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.BASIC_ISO_DATE
        );
        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                return LocalDate.parse(value, formatter).atStartOfDay();
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
