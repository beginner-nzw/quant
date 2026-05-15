package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementItemVO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

@Service
@RequiredArgsConstructor
public class CninfoProxyAnnouncementService {

    private static final String SOURCE_CODE = "CNINFO_ANNOUNCEMENT_PROXY";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern SINGLE_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([\\w.-]+)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");

    private final EventSourceConfigService eventSourceConfigService;
    private final ObjectMapper objectMapper;

    public CninfoProxyAnnouncementResponseVO previewAnnouncements(MarketEventSourceSyncDTO dto) {
        if (dto == null) {
            throw new BizException("CNINFO_PROXY_REQUEST_EMPTY", "巨潮公告包装请求不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "标的代码不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "标的名称不能为空");
        }

        EventSourceConfigItemVO sourceConfig = eventSourceConfigService.findSource(SOURCE_CODE);
        if (!StringUtils.hasText(sourceConfig == null ? null : sourceConfig.getUpstreamUrl())) {
            throw new BizException("CNINFO_PROXY_UPSTREAM_URL_EMPTY", "巨潮公告包装源未配置真实上游地址");
        }
        List<CninfoProxyAnnouncementItemVO> items = loadUpstreamAnnouncements(sourceConfig, dto);
        if (items.isEmpty()) {
            throw new BizException("CNINFO_PROXY_UPSTREAM_ITEMS_EMPTY", "巨潮公告上游未返回公告");
        }

        CninfoProxyAnnouncementResponseVO response = new CninfoProxyAnnouncementResponseVO();
        response.setSourceCode(SOURCE_CODE);
        response.setSourceName(sourceConfig == null ? "巨潮公告包装源" : defaultValue(sourceConfig.getSourceName(), "巨潮公告包装源"));
        response.setTargetCode(dto.getTargetCode().trim());
        response.setTargetName(dto.getTargetName().trim());
        response.setItemCount(items.size());
        response.setItems(items);
        return response;
    }

    public EventSourceRequestDiagnosticItemVO buildUpstreamRequestDiagnosticItem(EventSourceConfigItemVO sourceConfig,
                                                                                 MarketEventSourceSyncDTO dto) {
        String upstreamUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getUpstreamUrl());
        if (!StringUtils.hasText(upstreamUrl)) {
            return null;
        }

        String upstreamMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getUpstreamMethod(), "GET").toUpperCase(Locale.ROOT);
        int timeoutSeconds = sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        String method = "POST".equalsIgnoreCase(upstreamMethod) ? "POST" : "GET";

        EventSourceRequestDiagnosticItemVO item = new EventSourceRequestDiagnosticItemVO();
        item.setStageCode("UPSTREAM_REQUEST");
        item.setStageName("上游请求");
        item.setRequestMethod(method);
        item.setRequestTimeoutSeconds(timeoutSeconds);
        item.setRequestUrl("POST".equals(method) ? upstreamUrl : appendQueryParams(upstreamUrl, sourceConfig, dto));
        item.setRequestHeadersJson(formatJsonSafely(maskSensitiveHeaders(parseHeaders(sourceConfig == null ? null : sourceConfig.getUpstreamHeadersJson()))));
        if ("POST".equals(method)) {
            item.setRequestBodyJson(formatJsonSafely(resolveUpstreamBody(sourceConfig, dto)));
        }
        return item;
    }

    private List<CninfoProxyAnnouncementItemVO> loadUpstreamAnnouncements(EventSourceConfigItemVO sourceConfig,
                                                                          MarketEventSourceSyncDTO dto) {
        String upstreamUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getUpstreamUrl());
        if (!StringUtils.hasText(upstreamUrl)) {
            return List.of();
        }

        String upstreamMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getUpstreamMethod(), "GET").toUpperCase(Locale.ROOT);
        int timeoutSeconds = sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();

        HttpRequest request = buildRequest(upstreamUrl, upstreamMethod, timeoutSeconds, dto, sourceConfig);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("CNINFO_PROXY_UPSTREAM_HTTP_FAILED", "巨潮上游接口调用失败，HTTP 状态码: " + response.statusCode());
            }
            return parseUpstreamResponse(response.body(), sourceConfig);
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("CNINFO_PROXY_UPSTREAM_REQUEST_FAILED", "巨潮上游接口请求失败");
        }
    }

    private HttpRequest buildRequest(String upstreamUrl,
                                     String upstreamMethod,
                                     int timeoutSeconds,
                                     MarketEventSourceSyncDTO dto,
                                     EventSourceConfigItemVO sourceConfig) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, parseHeaders(sourceConfig == null ? null : sourceConfig.getUpstreamHeadersJson()));

        if ("POST".equalsIgnoreCase(upstreamMethod)) {
            Object bodyObject = resolveUpstreamBody(sourceConfig, dto);
            String body;
            try {
                body = objectMapper.writeValueAsString(bodyObject);
            } catch (Exception e) {
                throw new BizException("CNINFO_PROXY_UPSTREAM_SERIALIZE_FAILED", "巨潮上游请求序列化失败");
            }
            return builder
                    .uri(URI.create(upstreamUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
        }

        return builder
                .uri(URI.create(appendQueryParams(upstreamUrl, sourceConfig, dto)))
                .GET()
                .build();
    }

    private Object resolveUpstreamBody(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO dto) {
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

    private String appendQueryParams(String upstreamUrl,
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

    private List<CninfoProxyAnnouncementItemVO> parseUpstreamResponse(String responseBody, EventSourceConfigItemVO sourceConfig) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode itemsNode = resolveItemsNode(root, trimToNull(sourceConfig == null ? null : sourceConfig.getUpstreamItemsField()));
            if (itemsNode == null || !itemsNode.isArray()) {
                throw new BizException("CNINFO_PROXY_UPSTREAM_ITEMS_EMPTY", "巨潮上游接口未返回公告列表");
            }

            Map<String, List<String>> fieldMappings = parseFieldMappings(sourceConfig == null ? null : sourceConfig.getUpstreamFieldMappingJson());
            List<CninfoProxyAnnouncementItemVO> items = new ArrayList<>();
            for (JsonNode itemNode : itemsNode) {
                if (itemNode != null && itemNode.isObject()) {
                    items.add(toAnnouncementItem(itemNode, fieldMappings));
                }
            }
            return items;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("CNINFO_PROXY_UPSTREAM_PARSE_FAILED", "巨潮上游接口响应解析失败");
        }
    }

    private JsonNode resolveItemsNode(JsonNode root, String upstreamItemsField) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (StringUtils.hasText(upstreamItemsField)) {
            JsonNode node = resolvePath(root, upstreamItemsField);
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

    private CninfoProxyAnnouncementItemVO toAnnouncementItem(JsonNode itemNode, Map<String, List<String>> fieldMappings) {
        CninfoProxyAnnouncementItemVO item = new CninfoProxyAnnouncementItemVO();
        item.setSecCode(defaultValue(readMappedText(itemNode, fieldMappings, "secCode", "secCode", "targetCode", "stockCode", "symbol"), null));
        item.setSecName(defaultValue(readMappedText(itemNode, fieldMappings, "secName", "secName", "targetName", "stockName", "shortName"), null));
        item.setAnnouncementTitle(defaultValue(readMappedText(itemNode, fieldMappings, "announcementTitle", "announcementTitle", "eventTitle", "title", "headline"), "未命名公告"));
        item.setAnnouncementSummary(defaultValue(readMappedText(itemNode, fieldMappings, "announcementSummary", "announcementSummary", "eventSummary", "summary", "description"), item.getAnnouncementTitle()));
        item.setAnnouncementUrl(defaultValue(readMappedText(itemNode, fieldMappings, "announcementUrl", "announcementUrl", "sourceUrl", "url", "link"), null));
        item.setAnnouncementTime(formatAnnouncementTime(readMappedText(itemNode, fieldMappings, "announcementTime", "announcementTime", "occurredAt", "publishTime", "time", "disclosureTime")));
        item.setImportance(defaultValue(readMappedText(itemNode, fieldMappings, "importance", "importance", "impactLevel", "level"), "HIGH"));
        return item;
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
                throw new BizException("CNINFO_PROXY_UPSTREAM_FIELD_MAPPING_INVALID", "巨潮上游字段映射配置不是合法 JSON 对象");
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
            throw new BizException("CNINFO_PROXY_UPSTREAM_FIELD_MAPPING_INVALID", "巨潮上游字段映射配置解析失败");
        }
    }

    private String formatAnnouncementTime(String rawValue) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now().format(DATETIME_FORMATTER);
        }
        LocalDateTime dateTime = parseDateTime(value);
        return dateTime.format(DATETIME_FORMATTER);
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

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
