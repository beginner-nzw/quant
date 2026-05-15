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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CninfoPublicAnnouncementSyncAdapter implements EventSourceSyncAdapter {

    private static final String INGEST_MODE = "CNINFO_PUBLIC_CRAWLER";
    private static final String DEFAULT_ENDPOINT_URL = "https://www.cninfo.com.cn/new/hisAnnouncement/query";
    private static final String DEFAULT_REFERER = "https://www.cninfo.com.cn/new/commonUrl/pageOfSearch?url=disclosure/list/search";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SINGLE_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([\\w.-]+)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");
    private static final Pattern TRAILING_EXCHANGE_PATTERN = Pattern.compile("(?i)(?:\\.|_)?(SH|SZ|BJ|HK)$");
    private static final Pattern LEADING_EXCHANGE_PATTERN = Pattern.compile("(?i)^(SH|SZ|BJ|HK)");

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig != null
                && StringUtils.hasText(sourceConfig.getIngestMode())
                && INGEST_MODE.equalsIgnoreCase(sourceConfig.getIngestMode().trim());
    }

    @Override
    public List<MarketEventCreateDTO> sync(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = StringUtils.hasText(sourceConfig.getEndpointUrl())
                ? sourceConfig.getEndpointUrl().trim() : DEFAULT_ENDPOINT_URL;
        int timeoutSeconds = sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        List<String> searchKeywords = resolveSearchKeywords(request);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .build();

        try {
            for (String searchKeyword : searchKeywords) {
                Map<String, String> headers = resolveRequestHeaders(sourceConfig, request, searchKeyword);
                String requestBody = buildRequestBody(sourceConfig, request, searchKeyword);
                HttpRequest httpRequest = buildRequest(endpointUrl, timeoutSeconds, headers, requestBody);
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new BizException("CNINFO_PUBLIC_HTTP_FAILED", "巨潮公开公告检索失败，HTTP 状态码: " + response.statusCode());
                }
                List<MarketEventCreateDTO> events = parseResponse(response.body(), request, sourceConfig, endpointUrl);
                if (!events.isEmpty()) {
                    return events;
                }
            }
            throw new BizException("CNINFO_PUBLIC_ITEMS_EMPTY", "巨潮公开公告未返回公告");
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("CNINFO_PUBLIC_REQUEST_FAILED", "巨潮公开公告请求失败");
        }
    }

    @Override
    public List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = StringUtils.hasText(sourceConfig.getEndpointUrl())
                ? sourceConfig.getEndpointUrl().trim() : DEFAULT_ENDPOINT_URL;
        int timeoutSeconds = sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        List<String> searchKeywords = resolveSearchKeywords(request);
        List<EventSourceRequestDiagnosticItemVO> items = new ArrayList<>();
        for (int index = 0; index < searchKeywords.size(); index++) {
            String searchKeyword = searchKeywords.get(index);
            EventSourceRequestDiagnosticItemVO item = new EventSourceRequestDiagnosticItemVO();
            item.setStageCode(index == 0 ? "PUBLIC_CRAWLER_PRIMARY_REQUEST" : "PUBLIC_CRAWLER_FALLBACK_REQUEST_" + index);
            item.setStageName(resolveDiagnosticStageName(index, searchKeyword, request));
            item.setRequestMethod("POST");
            item.setRequestTimeoutSeconds(timeoutSeconds);
            item.setRequestUrl(endpointUrl);
            item.setRequestHeadersJson(formatJsonSafely(maskSensitiveHeaders(resolveRequestHeaders(sourceConfig, request, searchKeyword))));
            item.setRequestBodyJson(buildRequestBody(sourceConfig, request, searchKeyword));
            items.add(item);
        }
        return items;
    }

    private HttpRequest buildRequest(String endpointUrl,
                                     int timeoutSeconds,
                                     Map<String, String> headers,
                                     String requestBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, headers);
        return builder.POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)).build();
    }

    private Map<String, String> resolveRequestHeaders(EventSourceConfigItemVO sourceConfig,
                                                      MarketEventSourceSyncDTO request,
                                                      String searchKeyword) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("Referer", DEFAULT_REFERER);
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("User-Agent", "Mozilla/5.0");
        headers.put("Accept", "application/json, text/plain, */*");

        JsonNode headerNode = parseJsonNode(
                sourceConfig == null ? null : sourceConfig.getRequestHeadersJson(),
                "CNINFO_PUBLIC_REQUEST_HEADERS_INVALID",
                "巨潮公开公告请求头配置解析失败"
        );
        if (headerNode == null || headerNode.isNull()) {
            return headers;
        }
        if (!headerNode.isObject()) {
            throw new BizException("CNINFO_PUBLIC_REQUEST_HEADERS_INVALID", "巨潮公开公告请求头配置不是合法 JSON 对象");
        }
        Object rendered = renderTemplateValue(headerNode, buildTemplateVariables(request, sourceConfig, searchKeyword));
        if (rendered instanceof Map<?, ?> renderedMap) {
            renderedMap.forEach((key, value) -> {
                if (key != null && value != null && StringUtils.hasText(String.valueOf(key))) {
                    headers.put(String.valueOf(key).trim(), String.valueOf(value).trim());
                }
            });
        }
        return headers;
    }

    private String buildRequestBody(EventSourceConfigItemVO sourceConfig,
                                    MarketEventSourceSyncDTO request,
                                    String searchKeyword) {
        Map<String, Object> params = buildDefaultRequestParams(request, sourceConfig, searchKeyword);
        JsonNode bodyNode = parseJsonNode(
                sourceConfig == null ? null : sourceConfig.getRequestBodyJson(),
                "CNINFO_PUBLIC_REQUEST_BODY_INVALID",
                "巨潮公开公告请求体配置解析失败"
        );
        if (bodyNode != null && !bodyNode.isNull()) {
            if (!bodyNode.isObject()) {
                throw new BizException("CNINFO_PUBLIC_REQUEST_BODY_INVALID", "巨潮公开公告请求体配置不是合法 JSON 对象");
            }
            Object rendered = renderTemplateValue(bodyNode, buildTemplateVariables(request, sourceConfig, searchKeyword));
            if (rendered instanceof Map<?, ?> renderedMap) {
                renderedMap.forEach((key, value) -> {
                    if (key != null && StringUtils.hasText(String.valueOf(key))) {
                        params.put(String.valueOf(key), value);
                    }
                });
            }
        }
        return encodeFormBody(params);
    }

    private Map<String, Object> buildDefaultRequestParams(MarketEventSourceSyncDTO request,
                                                          EventSourceConfigItemVO sourceConfig,
                                                          String searchKeyword) {
        String dateRange = buildDateRange();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("pageNum", 1);
        params.put("pageSize", request == null || request.getItemCount() == null ? 10 : request.getItemCount());
        params.put("column", resolveExchangeColumn(request));
        params.put("tabName", "fulltext");
        params.put("plate", "");
        params.put("stock", "");
        params.put("searchkey", searchKeyword);
        params.put("secid", "");
        params.put("category", "");
        params.put("trade", "");
        params.put("seDate", dateRange);
        params.put("sortName", "");
        params.put("sortType", "");
        params.put("isHLtitle", true);
        return params;
    }

    private String encodeFormBody(Map<String, Object> params) {
        List<String> encoded = new ArrayList<>();
        params.forEach((key, value) -> {
            if (StringUtils.hasText(key) && value != null && StringUtils.hasText(String.valueOf(value))) {
                encoded.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8));
            }
        });
        return String.join("&", encoded);
    }

    private List<MarketEventCreateDTO> parseResponse(String responseBody,
                                                     MarketEventSourceSyncDTO request,
                                                     EventSourceConfigItemVO sourceConfig,
                                                     String endpointUrl) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode announcements = readItemsNode(root, sourceConfig);
            if (announcements == null || !announcements.isArray() || announcements.isEmpty()) {
                return List.of();
            }
            List<MarketEventCreateDTO> result = new ArrayList<>();
            for (JsonNode item : announcements) {
                if (item != null && item.isObject()) {
                    result.add(toMarketEvent(item, request, sourceConfig, endpointUrl));
                }
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("CNINFO_PUBLIC_PARSE_FAILED", "巨潮公开公告解析失败");
        }
    }

    private JsonNode readItemsNode(JsonNode root, EventSourceConfigItemVO sourceConfig) {
        if (root == null || root.isNull()) {
            return null;
        }
        String fieldPath = defaultIfBlank(sourceConfig == null ? null : sourceConfig.getResponseItemsField(), "announcements");
        JsonNode current = root;
        for (String segment : fieldPath.split("\\.")) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            current = current.path(segment.trim());
        }
        return current;
    }

    private MarketEventCreateDTO toMarketEvent(JsonNode item,
                                               MarketEventSourceSyncDTO request,
                                               EventSourceConfigItemVO sourceConfig,
                                               String endpointUrl) {
        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        String title = stripHtml(item.path("announcementTitle").asText(item.path("shortTitle").asText("")));
        dto.setTargetType(request != null && StringUtils.hasText(request.getTargetType()) ? request.getTargetType().trim() : "STOCK");
        dto.setTargetCode(defaultIfBlank(stripHtml(item.path("secCode").asText("")), request == null ? null : request.getTargetCode()));
        dto.setTargetName(defaultIfBlank(
                stripHtml(item.path("secName").asText(item.path("tileSecName").asText(""))),
                request == null ? null : request.getTargetName()
        ));
        dto.setEventType(resolveEventType(title, sourceConfig));
        dto.setEventTitle(defaultIfBlank(title, "未命名公告"));
        dto.setEventSummary(defaultIfBlank(stripHtml(item.path("announcementContent").asText("")), dto.getEventTitle()));
        dto.setSourceChannel(defaultIfBlank(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "EXCHANGE_FEED"));
        dto.setSourceUrl(resolveSourceUrl(item.path("adjunctUrl").asText(item.path("announcementUrl").asText("")), endpointUrl));
        dto.setImpactLevel(resolveImpactLevel(item, title, dto.getEventType(), sourceConfig));
        dto.setEventStatus("ACTIVE");
        dto.setOccurredAt(parseDateTime(item.path("announcementTime").asText(item.path("time").asText(""))));
        return dto;
    }

    private String resolveEventType(String title, EventSourceConfigItemVO sourceConfig) {
        if (containsAny(title,
                "年度报告", "半年度报告", "季度报告",
                "业绩预告", "业绩快报", "产销快报",
                "利润分配", "分红派息")) {
            return "EARNINGS";
        }
        if (containsAny(title,
                "风险提示", "股票交易异常波动", "异常波动",
                "停牌", "复牌", "终止上市", "退市",
                "问询函", "监管函", "立案", "处罚",
                "诉讼", "仲裁", "冻结", "违约", "失信")) {
            return "RISK_ALERT";
        }
        return defaultIfBlank(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "ANNOUNCEMENT");
    }

    private String resolveImpactLevel(JsonNode item, String title, String eventType, EventSourceConfigItemVO sourceConfig) {
        String important = item.path("important").asText("");
        if ("true".equalsIgnoreCase(important) || "1".equals(important)) {
            return "HIGH";
        }
        if ("RISK_ALERT".equalsIgnoreCase(eventType) || "EARNINGS".equalsIgnoreCase(eventType)) {
            return "HIGH";
        }
        if (containsAny(title,
                "重大事项", "回购", "停牌", "复牌",
                "发行", "收购", "并购", "重组",
                "减持", "增持", "控制权变更", "质押")) {
            return "HIGH";
        }
        return defaultIfBlank(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "MEDIUM");
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

    private String resolveSourceUrl(String rawUrl, String endpointUrl) {
        String value = trimToNull(rawUrl);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        try {
            return URI.create("https://www.cninfo.com.cn/").resolve(value).toString();
        } catch (Exception ignored) {
            try {
                return URI.create(endpointUrl).resolve(value).toString();
            } catch (Exception e) {
                return value;
            }
        }
    }

    private List<String> resolveSearchKeywords(MarketEventSourceSyncDTO request) {
        Set<String> keywords = new LinkedHashSet<>();
        String normalizedTargetCode = normalizeTargetCode(request == null ? null : request.getTargetCode());
        String rawTargetCode = trimToNull(request == null ? null : request.getTargetCode());
        String targetName = trimToNull(request == null ? null : request.getTargetName());

        if (StringUtils.hasText(normalizedTargetCode)) {
            keywords.add(normalizedTargetCode);
        }
        if (StringUtils.hasText(targetName)) {
            keywords.add(targetName);
        }
        if (!StringUtils.hasText(normalizedTargetCode) && StringUtils.hasText(rawTargetCode)) {
            keywords.add(rawTargetCode);
        }
        if (keywords.isEmpty()) {
            keywords.add("");
        }
        return new ArrayList<>(keywords);
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

    private String resolveExchangeColumn(MarketEventSourceSyncDTO request) {
        String targetCode = normalizeTargetCode(request == null ? null : request.getTargetCode());
        if (!StringUtils.hasText(targetCode)) {
            return "szse";
        }
        if (targetCode.startsWith("6") || targetCode.startsWith("9")) {
            return "sse";
        }
        return "szse";
    }

    private String resolveDiagnosticStageName(int index, String searchKeyword, MarketEventSourceSyncDTO request) {
        String normalizedTargetCode = normalizeTargetCode(request == null ? null : request.getTargetCode());
        String targetName = trimToNull(request == null ? null : request.getTargetName());
        if (index == 0 && StringUtils.hasText(normalizedTargetCode) && normalizedTargetCode.equals(searchKeyword)) {
            return "巨潮公开公告请求（代码检索）";
        }
        if (StringUtils.hasText(targetName) && targetName.equals(searchKeyword)) {
            return index == 0 ? "巨潮公开公告请求（名称检索）" : "巨潮公开公告请求（名称回退）";
        }
        return index == 0 ? "巨潮公开公告请求" : "巨潮公开公告请求（回退）";
    }

    private String buildDateRange() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return startDate.format(DATE_FORMATTER) + "~" + endDate.format(DATE_FORMATTER);
    }

    private Map<String, Object> buildTemplateVariables(MarketEventSourceSyncDTO request,
                                                       EventSourceConfigItemVO sourceConfig,
                                                       String searchKeyword) {
        String normalizedTargetCode = normalizeTargetCode(request == null ? null : request.getTargetCode());
        String dateRange = buildDateRange();
        LocalDate[] dates = resolveDateRange();
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("sourceCode", sourceConfig == null ? null : sourceConfig.getSourceCode());
        variables.put("sourceName", sourceConfig == null ? null : sourceConfig.getSourceName());
        variables.put("sourceCategory", sourceConfig == null ? null : sourceConfig.getSourceCategory());
        variables.put("sourceChannel", sourceConfig == null ? null : sourceConfig.getSourceChannel());
        variables.put("targetType", defaultIfBlank(request == null ? null : request.getTargetType(), "STOCK"));
        variables.put("targetCode", request == null ? null : request.getTargetCode());
        variables.put("normalizedTargetCode", normalizedTargetCode);
        variables.put("targetName", request == null ? null : request.getTargetName());
        variables.put("itemCount", request == null || request.getItemCount() == null ? 10 : request.getItemCount());
        variables.put("defaultEventType", sourceConfig == null ? null : sourceConfig.getDefaultEventType());
        variables.put("defaultImpactLevel", sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel());
        variables.put("searchKeyword", searchKeyword);
        variables.put("exchangeColumn", resolveExchangeColumn(request));
        variables.put("dateStart", dates[0].format(DATE_FORMATTER));
        variables.put("dateEnd", dates[1].format(DATE_FORMATTER));
        variables.put("dateRange", dateRange);
        return variables;
    }

    private LocalDate[] resolveDateRange() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return new LocalDate[]{startDate, endDate};
    }

    private JsonNode parseJsonNode(String rawJson, String errorCode, String errorMessage) {
        if (!StringUtils.hasText(rawJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new BizException(errorCode, errorMessage);
        }
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

    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (builder == null || headers == null) {
            return;
        }
        headers.forEach((key, value) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                builder.header(key.trim(), value.trim());
            }
        });
    }

    private Map<String, String> maskSensitiveHeaders(Map<String, String> headers) {
        Map<String, String> masked = new LinkedHashMap<>();
        if (headers == null) {
            return masked;
        }
        headers.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
            if (normalizedKey.contains("authorization")
                    || normalizedKey.contains("token")
                    || normalizedKey.contains("api-key")
                    || normalizedKey.contains("secret")) {
                masked.put(key, "******");
            } else {
                masked.put(key, value);
            }
        });
        return masked;
    }

    private String formatJsonSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private String stripHtml(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return HTML_TAG_PATTERN.matcher(normalized).replaceAll("").trim();
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
