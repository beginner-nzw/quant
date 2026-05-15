package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@Component
@RequiredArgsConstructor
public class GovCnPolicyHtmlSyncAdapter implements EventSourceSyncAdapter {

    private static final String INGEST_MODE = "GOV_CN_POLICY_HTML";
    private static final String DEFAULT_ENDPOINT_URL = "https://www.gov.cn/zhengce/zuixin/";
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2})[-年./](\\d{1,2})[-月./](\\d{1,2})");
    private static final Pattern SINGLE_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([\\w.-]+)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");
    private static final Set<String> SKIP_TITLES = Set.of(
            "更多",
            "返回顶部",
            "首页",
            "下一页",
            "上一页"
    );

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

        HttpRequest httpRequest = buildRequest(endpointUrl, timeoutSeconds, sourceConfig, request);
        HttpClient client = buildHttpClient(sourceConfig, timeoutSeconds);
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("GOV_POLICY_HTTP_FAILED", "Gov policy source sync failed, HTTP status: " + response.statusCode());
            }
            return parseResponse(response.body(), endpointUrl, sourceConfig, request);
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("GOV_POLICY_REQUEST_FAILED", buildRequestFailureMessage("Gov policy source request failed", e));
        }
    }

    @Override
    public List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = StringUtils.hasText(sourceConfig.getEndpointUrl())
                ? sourceConfig.getEndpointUrl().trim() : DEFAULT_ENDPOINT_URL;
        int timeoutSeconds = sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();

        EventSourceRequestDiagnosticItemVO item = new EventSourceRequestDiagnosticItemVO();
        item.setStageCode("PRIMARY_REQUEST");
        item.setStageName(sourceConfig != null && Boolean.FALSE.equals(sourceConfig.getSslVerify())
                ? "Gov Policy Request (SSL Verify Disabled)" : "Gov Policy Request");
        item.setRequestMethod("GET");
        item.setRequestTimeoutSeconds(timeoutSeconds);
        item.setRequestUrl(appendQueryParams(endpointUrl, sourceConfig, request));
        item.setRequestHeadersJson(formatJsonSafely(maskSensitiveHeaders(resolveRequestHeaders(sourceConfig, request))));
        return List.of(item);
    }

    private HttpRequest buildRequest(String endpointUrl,
                                     int timeoutSeconds,
                                     EventSourceConfigItemVO sourceConfig,
                                     MarketEventSourceSyncDTO request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(appendQueryParams(endpointUrl, sourceConfig, request)))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, resolveRequestHeaders(sourceConfig, request));
        return builder.GET().build();
    }

    private HttpClient buildHttpClient(EventSourceConfigItemVO sourceConfig, int timeoutSeconds) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1);
        if (sourceConfig != null && Boolean.FALSE.equals(sourceConfig.getSslVerify())) {
            builder.sslContext(buildTrustAllSslContext());
        }
        return builder.build();
    }

    private SSLContext buildTrustAllSslContext() {
        try {
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new BizException("GOV_POLICY_SSL_CONTEXT_FAILED", buildRequestFailureMessage("Gov policy SSL context initialization failed", e));
        }
    }

    private Map<String, String> resolveRequestHeaders(EventSourceConfigItemVO sourceConfig,
                                                      MarketEventSourceSyncDTO request) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Referer", "https://www.gov.cn/");

        JsonNode headerNode = parseJsonNode(
                sourceConfig == null ? null : sourceConfig.getRequestHeadersJson(),
                "GOV_POLICY_REQUEST_HEADERS_INVALID",
                "Gov policy request headers parsing failed"
        );
        if (headerNode == null || headerNode.isNull()) {
            return headers;
        }
        if (!headerNode.isObject()) {
            throw new BizException("GOV_POLICY_REQUEST_HEADERS_INVALID", "Gov policy request headers must be a JSON object");
        }
        Object rendered = renderTemplateValue(headerNode, buildTemplateVariables(request, sourceConfig));
        if (rendered instanceof Map<?, ?> renderedMap) {
            renderedMap.forEach((key, value) -> {
                if (key != null && value != null && StringUtils.hasText(String.valueOf(key))) {
                    headers.put(String.valueOf(key).trim(), String.valueOf(value).trim());
                }
            });
        }
        return headers;
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
        JsonNode templateNode = parseJsonNode(
                sourceConfig == null ? null : sourceConfig.getRequestQueryJson(),
                "GOV_POLICY_REQUEST_QUERY_INVALID",
                "Gov policy request query parsing failed"
        );
        if (templateNode == null || !templateNode.isObject()) {
            return Map.of();
        }
        Object rendered = renderTemplateValue(templateNode, buildTemplateVariables(request, sourceConfig));
        if (!(rendered instanceof Map<?, ?> renderedMap)) {
            return Map.of();
        }
        Map<String, Object> params = new LinkedHashMap<>();
        renderedMap.forEach((key, value) -> params.put(String.valueOf(key), value));
        return params;
    }

    private Map<String, Object> buildTemplateVariables(MarketEventSourceSyncDTO request, EventSourceConfigItemVO sourceConfig) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("sourceCode", sourceConfig == null ? null : sourceConfig.getSourceCode());
        variables.put("sourceName", sourceConfig == null ? null : sourceConfig.getSourceName());
        variables.put("sourceCategory", sourceConfig == null ? null : sourceConfig.getSourceCategory());
        variables.put("sourceChannel", sourceConfig == null ? null : sourceConfig.getSourceChannel());
        variables.put("targetType", request == null ? "STOCK" : defaultIfBlank(request.getTargetType(), "STOCK"));
        variables.put("targetCode", request == null ? "" : request.getTargetCode());
        variables.put("targetName", request == null ? "" : request.getTargetName());
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

    private List<MarketEventCreateDTO> parseResponse(String responseBody,
                                                     String endpointUrl,
                                                     EventSourceConfigItemVO sourceConfig,
                                                     MarketEventSourceSyncDTO request) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BizException("GOV_POLICY_RESPONSE_EMPTY", "Gov policy response body is empty");
        }

        org.jsoup.nodes.Document document = Jsoup.parse(responseBody, endpointUrl);
        Elements anchors = document.select("a[href]");
        if (anchors.isEmpty()) {
            throw new BizException("GOV_POLICY_RESPONSE_ITEMS_EMPTY", "Gov policy page contains no links");
        }

        int itemCount = request == null || request.getItemCount() == null || request.getItemCount() <= 0
                ? 10 : request.getItemCount();
        List<MarketEventCreateDTO> result = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        for (Element anchor : anchors) {
            String href = trimToNull(anchor.absUrl("href"));
            String title = trimToNull(anchor.text());
            String occurredDate = defaultIfBlank(resolveOccurredDate(anchor), matchDate(href));
            if (!isPolicyItem(href, title, occurredDate)) {
                continue;
            }
            String fingerprint = title + "|" + href;
            if (!dedupe.add(fingerprint)) {
                continue;
            }
            result.add(toMarketEvent(href, title, occurredDate, sourceConfig, request));
            if (result.size() >= itemCount) {
                break;
            }
        }

        if (result.isEmpty()) {
            throw new BizException("GOV_POLICY_RESPONSE_ITEMS_EMPTY", "Gov policy page returned no policy items");
        }
        return result;
    }

    private boolean isPolicyItem(String href, String title, String occurredDate) {
        if (!StringUtils.hasText(href) || !StringUtils.hasText(title)) {
            return false;
        }
        if (SKIP_TITLES.contains(title.trim())) {
            return false;
        }
        String normalizedHref = href.trim().toLowerCase(Locale.ROOT);
        if (!normalizedHref.contains("gov.cn")) {
            return false;
        }
        if (!normalizedHref.contains("/zhengce/")) {
            return false;
        }
        if (title.trim().length() < 8) {
            return false;
        }
        if (title.contains("加载更多") || title.contains("更多>>")) {
            return false;
        }
        return normalizedHref.contains("/content/")
                || normalizedHref.contains("/zhengceku/")
                || normalizedHref.endsWith(".htm")
                || normalizedHref.endsWith(".html");
    }

    private MarketEventCreateDTO toMarketEvent(String href,
                                               String title,
                                               String occurredDate,
                                               EventSourceConfigItemVO sourceConfig,
                                               MarketEventSourceSyncDTO request) {
        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        dto.setTargetType(request == null ? "STOCK" : defaultIfBlank(request.getTargetType(), "STOCK"));
        dto.setTargetCode(request == null ? null : trimToNull(request.getTargetCode()));
        dto.setTargetName(request == null ? null : trimToNull(request.getTargetName()));
        dto.setEventType(defaultIfBlank(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "POLICY"));
        dto.setEventTitle(title);
        dto.setEventSummary(buildSummary(title, occurredDate));
        dto.setSourceChannel(defaultIfBlank(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "POLICY_MONITOR"));
        dto.setSourceUrl(href);
        dto.setImpactLevel(defaultIfBlank(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "HIGH"));
        dto.setEventStatus("ACTIVE");
        dto.setOccurredAt(parseDateTime(occurredDate));
        return dto;
    }

    private String resolveOccurredDate(Element anchor) {
        Element current = anchor;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            String matchedDate = matchDate(current.text());
            if (StringUtils.hasText(matchedDate)) {
                return matchedDate;
            }
            current = current.parent();
        }
        return null;
    }

    private String matchDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildSummary(String title, String occurredDate) {
        if (StringUtils.hasText(occurredDate)) {
            return occurredDate + " 中国政府网发布政策更新：" + title;
        }
        return "中国政府网最新政策更新：" + title;
    }

    private LocalDateTime parseDateTime(String rawValue) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.of(9, 0));
        } catch (Exception ignored) {
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

    private void appendParam(List<String> params, String key, String value) {
        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
            params.add(URLEncoder.encode(key.trim(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
        }
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

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String buildRequestFailureMessage(String prefix, Exception e) {
        if (e == null) {
            return prefix;
        }
        StringBuilder message = new StringBuilder(prefix)
                .append(": ")
                .append(e.getClass().getSimpleName());
        if (StringUtils.hasText(e.getMessage())) {
            message.append(" - ").append(e.getMessage());
        }
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            message.append("; cause=")
                    .append(cause.getClass().getSimpleName());
            if (StringUtils.hasText(cause.getMessage())) {
                message.append(" - ").append(cause.getMessage());
            }
        }
        return message.toString();
    }
}
