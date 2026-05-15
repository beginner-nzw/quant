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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
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
public class RssXmlEventSourceSyncAdapter implements EventSourceSyncAdapter {

    private static final String INGEST_MODE = "RSS_XML";
    private static final Pattern SINGLE_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([\\w.-]+)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern FULL_DATE_PATTERN = Pattern.compile("(20\\d{2})[-年./](\\d{1,2})[-月./](\\d{1,2})");
    private static final Pattern COMPACT_DATE_PATTERN = Pattern.compile("(?<!\\d)(20\\d{2})(\\d{2})(\\d{2})(?!\\d)");
    private static final Pattern TRAILING_EXCHANGE_PATTERN = Pattern.compile("(?i)(?:\\.|_)?(SH|SZ|BJ|HK)$");
    private static final Pattern LEADING_EXCHANGE_PATTERN = Pattern.compile("(?i)^(SH|SZ|BJ|HK)");
    private static final List<String> DEFAULT_ITEM_PATHS = List.of(
            "rss.channel.item",
            "channel.item",
            "feed.entry",
            "entry",
            "item"
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
        String endpointUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getEndpointUrl());
        if (!StringUtils.hasText(endpointUrl)) {
            throw new BizException("EVENT_SOURCE_ENDPOINT_URL_EMPTY", "RSS endpoint URL cannot be empty");
        }

        String requestMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getRequestMethod(), "GET").toUpperCase(Locale.ROOT);
        if (!"GET".equals(requestMethod)) {
            throw new BizException("EVENT_SOURCE_REQUEST_METHOD_UNSUPPORTED", "RSS source only supports GET requests");
        }

        int timeoutSeconds = sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        HttpRequest httpRequest = buildRequest(endpointUrl, timeoutSeconds, sourceConfig, request);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("EVENT_SOURCE_HTTP_FAILED", buildHttpFailureMessage(response));
            }
            return parseResponse(response.body(), sourceConfig, request);
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("EVENT_SOURCE_HTTP_REQUEST_FAILED", "RSS source request failed");
        }
    }

    @Override
    public List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getEndpointUrl());
        if (!StringUtils.hasText(endpointUrl)) {
            throw new BizException("EVENT_SOURCE_ENDPOINT_URL_EMPTY", "RSS endpoint URL cannot be empty");
        }

        String requestMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getRequestMethod(), "GET").toUpperCase(Locale.ROOT);
        if (!"GET".equals(requestMethod)) {
            throw new BizException("EVENT_SOURCE_REQUEST_METHOD_UNSUPPORTED", "RSS source only supports GET requests");
        }

        int timeoutSeconds = sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        EventSourceRequestDiagnosticItemVO item = new EventSourceRequestDiagnosticItemVO();
        item.setStageCode("PRIMARY_REQUEST");
        item.setStageName("RSS Request");
        item.setRequestMethod("GET");
        item.setRequestTimeoutSeconds(timeoutSeconds);
        item.setRequestUrl(appendQueryParams(endpointUrl, sourceConfig, request));
        item.setRequestHeadersJson(formatJsonSafely(maskSensitiveHeaders(parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson()))));
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
        applyHeaders(builder, parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson()));
        return builder.GET().build();
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
                "EVENT_SOURCE_REQUEST_QUERY_INVALID",
                "RSS query config parsing failed"
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

    private Map<String, String> parseHeaders(String rawHeadersJson) {
        String value = trimToNull(rawHeadersJson);
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null || !root.isObject()) {
                throw new BizException("EVENT_SOURCE_REQUEST_HEADERS_INVALID", "RSS request headers must be a JSON object");
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
            throw new BizException("EVENT_SOURCE_REQUEST_HEADERS_INVALID", "RSS request headers parsing failed");
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
            return parseXmlResponse(responseBody, sourceConfig, request);
        } catch (BizException e) {
            if ("EVENT_SOURCE_RESPONSE_XML_INVALID".equals(e.getCode())) {
                return parseHtmlResponse(responseBody, sourceConfig, request);
            }
            throw e;
        }
    }

    private List<MarketEventCreateDTO> parseXmlResponse(String responseBody,
                                                        EventSourceConfigItemVO sourceConfig,
                                                        MarketEventSourceSyncDTO request) {
        try {
            Document document = parseXml(responseBody);
            List<Element> itemElements = resolveItemElements(
                    document == null ? null : document.getDocumentElement(),
                    trimToNull(sourceConfig == null ? null : sourceConfig.getResponseItemsField())
            );
            if (itemElements.isEmpty()) {
                throw new BizException("EVENT_SOURCE_RESPONSE_ITEMS_EMPTY", "RSS response does not contain any items");
            }

            List<MarketEventCreateDTO> result = new ArrayList<>();
            for (Element itemElement : itemElements) {
                if (itemElement != null) {
                    result.add(toMarketEvent(itemElement, sourceConfig, request));
                }
            }
            if (result.isEmpty()) {
                throw new BizException("EVENT_SOURCE_RESPONSE_ITEMS_EMPTY", "RSS source returned no importable items");
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("EVENT_SOURCE_RESPONSE_PARSE_FAILED", "RSS response parsing failed");
        }
    }

    private List<MarketEventCreateDTO> parseHtmlResponse(String responseBody,
                                                         EventSourceConfigItemVO sourceConfig,
                                                         MarketEventSourceSyncDTO request) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BizException("EVENT_SOURCE_RESPONSE_EMPTY", "RSS response body is empty");
        }

        String baseUri = defaultValue(sourceConfig == null ? null : sourceConfig.getEndpointUrl(), "");
        org.jsoup.nodes.Document document = Jsoup.parse(responseBody, baseUri);
        org.jsoup.select.Elements anchors = document.select("a[href]");
        if (anchors.isEmpty()) {
            throw new BizException("EVENT_SOURCE_RESPONSE_HTML_ITEMS_EMPTY", "RSS fallback HTML response contains no links");
        }

        int itemCount = request == null || request.getItemCount() == null || request.getItemCount() <= 0
                ? 10 : request.getItemCount();
        List<MarketEventCreateDTO> result = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        for (org.jsoup.nodes.Element anchor : anchors) {
            String href = trimToNull(anchor.absUrl("href"));
            String title = trimToNull(anchor.text());
            if (!isHtmlSearchResultItem(href, title, sourceConfig)) {
                continue;
            }
            String fingerprint = title + "|" + href;
            if (!dedupe.add(fingerprint)) {
                continue;
            }
            result.add(toMarketEventFromHtml(anchor, href, title, sourceConfig, request));
            if (result.size() >= itemCount) {
                break;
            }
        }

        if (result.isEmpty()) {
            throw new BizException("EVENT_SOURCE_RESPONSE_HTML_ITEMS_EMPTY", "RSS fallback HTML response returned no importable search results");
        }
        return result;
    }

    private boolean isHtmlSearchResultItem(String href, String title, EventSourceConfigItemVO sourceConfig) {
        if (!StringUtils.hasText(href) || !StringUtils.hasText(title) || title.trim().length() < 6) {
            return false;
        }
        String normalizedHref = href.trim().toLowerCase(Locale.ROOT);
        if (!normalizedHref.startsWith("http")) {
            return false;
        }
        if (normalizedHref.contains("javascript:")
                || normalizedHref.contains("/news/search")
                || normalizedHref.contains("/search?")) {
            return false;
        }
        String sourceCode = sourceConfig == null ? "" : defaultValue(sourceConfig.getSourceCode(), "");
        if ("POLICY_TRACKER".equalsIgnoreCase(sourceCode)) {
            return normalizedHref.contains("gov.cn")
                    || title.contains("政策")
                    || title.contains("国务院")
                    || title.contains("中国政府网");
        }
        return !title.equalsIgnoreCase("bing")
                && !title.contains("登录")
                && !title.contains("设置");
    }

    private MarketEventCreateDTO toMarketEventFromHtml(org.jsoup.nodes.Element anchor,
                                                       String href,
                                                       String title,
                                                       EventSourceConfigItemVO sourceConfig,
                                                       MarketEventSourceSyncDTO request) {
        String summary = resolveHtmlSearchSummary(anchor, title);
        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        dto.setTargetType(defaultValue(request == null ? null : request.getTargetType(), "STOCK"));
        dto.setTargetCode(request == null ? null : request.getTargetCode());
        dto.setTargetName(request == null ? null : request.getTargetName());
        dto.setEventType(defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "NEWS"));
        dto.setEventTitle(title);
        dto.setEventSummary(defaultValue(summary, title));
        dto.setSourceChannel(defaultValue(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "NEWS_FEED"));
        dto.setSourceUrl(href);
        dto.setImpactLevel(defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "MEDIUM"));
        dto.setEventStatus("ACTIVE");
        dto.setOccurredAt(parseDateTime(matchDate(combineText(title, summary, href))));
        return dto;
    }

    private String resolveHtmlSearchSummary(org.jsoup.nodes.Element anchor, String title) {
        org.jsoup.nodes.Element container = anchor.closest("li, article, div");
        String text = normalizeWhitespace(container == null ? null : container.text());
        if (!StringUtils.hasText(text)) {
            return title;
        }
        if (StringUtils.hasText(title) && text.startsWith(title)) {
            text = text.substring(title.length()).trim();
        }
        return abbreviate(defaultValue(text, title), 260);
    }

    private Document parseXml(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BizException("EVENT_SOURCE_RESPONSE_EMPTY", "RSS response body is empty");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            disableExternalEntities(factory);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(responseBody)));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("EVENT_SOURCE_RESPONSE_XML_INVALID", "RSS response is not valid XML");
        }
    }

    private void disableExternalEntities(DocumentBuilderFactory factory) {
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (Exception ignored) {
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {
        }
        factory.setExpandEntityReferences(false);
    }

    private List<Element> resolveItemElements(Element root, String responseItemsField) {
        if (root == null) {
            return List.of();
        }
        List<String> candidatePaths = new ArrayList<>();
        if (StringUtils.hasText(responseItemsField)) {
            candidatePaths.add(responseItemsField);
        }
        candidatePaths.addAll(DEFAULT_ITEM_PATHS);
        for (String path : candidatePaths) {
            List<Element> resolved = resolveElements(root, path);
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }
        return List.of();
    }

    private List<Element> resolveElements(Element root, String fieldPath) {
        if (root == null || !StringUtils.hasText(fieldPath)) {
            return List.of();
        }
        List<String> segments = List.of(fieldPath.trim().split("\\."));
        List<Element> current = new ArrayList<>();
        current.add(root);

        int startIndex = matchesElementName(root, segments.get(0)) ? 1 : 0;
        for (int index = startIndex; index < segments.size(); index++) {
            String segment = segments.get(index);
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            List<Element> next = new ArrayList<>();
            for (Element element : current) {
                next.addAll(findDirectChildElements(element, segment));
            }
            if (next.isEmpty()) {
                return List.of();
            }
            current = next;
        }
        return current;
    }

    private List<Element> findDirectChildElements(Element parent, String expectedName) {
        List<Element> result = new ArrayList<>();
        if (parent == null || !StringUtils.hasText(expectedName)) {
            return result;
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node child = childNodes.item(index);
            if (child instanceof Element element && matchesElementName(element, expectedName)) {
                result.add(element);
            }
        }
        return result;
    }

    private boolean matchesElementName(Element element, String expectedName) {
        if (element == null || !StringUtils.hasText(expectedName)) {
            return false;
        }
        String normalizedExpected = normalizeElementName(expectedName);
        String nodeName = normalizeElementName(element.getNodeName());
        String localName = normalizeElementName(element.getLocalName());
        return normalizedExpected.equalsIgnoreCase(nodeName)
                || normalizedExpected.equalsIgnoreCase(localName);
    }

    private String normalizeElementName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        int colonIndex = normalized.indexOf(':');
        return colonIndex >= 0 ? normalized.substring(colonIndex + 1) : normalized;
    }

    private MarketEventCreateDTO toMarketEvent(Element itemElement,
                                               EventSourceConfigItemVO sourceConfig,
                                               MarketEventSourceSyncDTO request) {
        Map<String, List<String>> fieldMappings = parseFieldMappings(sourceConfig == null ? null : sourceConfig.getFieldMappingJson());
        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        String title = defaultValue(
                stripHtml(readMappedText(itemElement, fieldMappings, "eventTitle", "eventTitle", "title", "headline", "subject")),
                "未命名事件"
        );
        dto.setTargetType(defaultValue(
                readMappedText(itemElement, fieldMappings, "targetType", "targetType", "assetType", "symbolType"),
                defaultValue(request == null ? null : request.getTargetType(), "STOCK")
        ));
        dto.setTargetCode(defaultValue(
                readMappedText(itemElement, fieldMappings, "targetCode", "targetCode", "code", "symbol", "ticker", "stockCode", "secCode"),
                request == null ? null : request.getTargetCode()
        ));
        dto.setTargetName(defaultValue(
                stripHtml(readMappedText(itemElement, fieldMappings, "targetName", "targetName", "name", "stockName", "securityName")),
                request == null ? null : request.getTargetName()
        ));
        dto.setEventType(defaultValue(
                readMappedText(itemElement, fieldMappings, "eventType", "eventType", "type", "category"),
                defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "NEWS")
        ));
        dto.setEventTitle(title);
        dto.setEventSummary(defaultValue(
                stripHtml(readMappedText(itemElement, fieldMappings, "eventSummary", "eventSummary", "summary", "description", "content", "contentSnippet")),
                title
        ));
        dto.setSourceChannel(defaultValue(
                readMappedText(itemElement, fieldMappings, "sourceChannel", "sourceChannel", "channel", "source"),
                defaultValue(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "NEWS_FEED")
        ));
        dto.setSourceUrl(readMappedText(itemElement, fieldMappings, "sourceUrl", "sourceUrl", "link", "url", "id"));
        dto.setImpactLevel(defaultValue(
                readMappedText(itemElement, fieldMappings, "impactLevel", "impactLevel", "level", "priority"),
                defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "MEDIUM")
        ));
        dto.setEventStatus(defaultValue(
                readMappedText(itemElement, fieldMappings, "eventStatus", "eventStatus", "status"),
                "ACTIVE"
        ));
        dto.setOccurredAt(parseDateTime(
                readMappedText(itemElement, fieldMappings, "occurredAt", "occurredAt", "pubDate", "published", "updated", "date", "dc:date")
        ));
        return dto;
    }

    private String readMappedText(Element itemElement,
                                  Map<String, List<String>> fieldMappings,
                                  String canonicalField,
                                  String... fallbackFieldNames) {
        List<String> mappingFields = fieldMappings.getOrDefault(canonicalField, List.of());
        if (!mappingFields.isEmpty()) {
            String mappedValue = readText(itemElement, mappingFields.toArray(String[]::new));
            if (StringUtils.hasText(mappedValue)) {
                return mappedValue;
            }
        }
        return readText(itemElement, fallbackFieldNames);
    }

    private Map<String, List<String>> parseFieldMappings(String rawFieldMappingJson) {
        String value = trimToNull(rawFieldMappingJson);
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null || !root.isObject()) {
                throw new BizException("EVENT_SOURCE_FIELD_MAPPING_INVALID", "RSS field mapping must be a JSON object");
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
            throw new BizException("EVENT_SOURCE_FIELD_MAPPING_INVALID", "RSS field mapping parsing failed");
        }
    }

    private String readText(Element itemElement, String... fieldPaths) {
        if (itemElement == null || fieldPaths == null) {
            return null;
        }
        for (String fieldPath : fieldPaths) {
            String value = resolveFieldPathValue(itemElement, fieldPath);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String resolveFieldPathValue(Element root, String fieldPath) {
        if (root == null || !StringUtils.hasText(fieldPath)) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        for (String segment : fieldPath.trim().split("\\.")) {
            if (StringUtils.hasText(segment)) {
                segments.add(segment.trim());
            }
        }
        if (segments.isEmpty()) {
            return null;
        }

        String attributeName = null;
        String lastSegment = segments.get(segments.size() - 1);
        if (lastSegment.startsWith("@") && lastSegment.length() > 1) {
            attributeName = lastSegment.substring(1);
            segments.remove(segments.size() - 1);
        }

        Element current = root;
        for (String segment : segments) {
            current = findFirstDirectChildElement(current, segment);
            if (current == null) {
                return null;
            }
        }

        if (StringUtils.hasText(attributeName)) {
            return trimToNull(current.getAttribute(attributeName));
        }

        String text = trimToNull(current.getTextContent());
        if (StringUtils.hasText(text)) {
            return text;
        }
        return trimToNull(current.getAttribute("href"));
    }

    private Element findFirstDirectChildElement(Element parent, String expectedName) {
        List<Element> matched = findDirectChildElements(parent, expectedName);
        return matched.isEmpty() ? null : matched.get(0);
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
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
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
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {
        }
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ISO_LOCAL_DATE
        )) {
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
                    return LocalDate.parse(value, formatter).atStartOfDay();
                }
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        return LocalDateTime.now();
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

    private String stripHtml(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return HTML_TAG_PATTERN.matcher(normalized).replaceAll("").trim();
    }

    private String matchDate(String text) {
        String value = trimToNull(text);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher fullMatcher = FULL_DATE_PATTERN.matcher(value);
        if (fullMatcher.find()) {
            return formatDate(fullMatcher.group(1), fullMatcher.group(2), fullMatcher.group(3));
        }
        Matcher compactMatcher = COMPACT_DATE_PATTERN.matcher(value);
        if (compactMatcher.find()) {
            return formatDate(compactMatcher.group(1), compactMatcher.group(2), compactMatcher.group(3));
        }
        return null;
    }

    private String formatDate(String yearValue, String monthValue, String dayValue) {
        try {
            int year = Integer.parseInt(yearValue);
            int month = Integer.parseInt(monthValue);
            int day = Integer.parseInt(dayValue);
            return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String combineText(String... values) {
        List<String> parts = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    parts.add(value.trim());
                }
            }
        }
        return String.join(" ", parts);
    }

    private String normalizeWhitespace(String value) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized.replaceAll("\\s+", " ").trim() : null;
    }

    private String abbreviate(String value, int maxLength) {
        String normalized = normalizeWhitespace(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
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

    private String buildHttpFailureMessage(HttpResponse<String> response) {
        if (response == null) {
            return "RSS source sync failed";
        }
        StringBuilder message = new StringBuilder("RSS source sync failed, HTTP status: ")
                .append(response.statusCode());
        String location = response.headers()
                .firstValue("Location")
                .orElse(null);
        if (StringUtils.hasText(location)) {
            message.append(", Location: ").append(location);
        }
        return message.toString();
    }
}
