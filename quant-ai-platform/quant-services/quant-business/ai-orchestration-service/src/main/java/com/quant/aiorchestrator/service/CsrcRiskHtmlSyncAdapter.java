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
import java.time.Year;
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
public class CsrcRiskHtmlSyncAdapter implements EventSourceSyncAdapter {

    private static final String INGEST_MODE = "CSRC_RISK_HTML";
    private static final String DEFAULT_ENDPOINT_URL = "https://www.csrc.gov.cn/csrc/zwxx/index.shtml";
    private static final Pattern FULL_DATE_PATTERN = Pattern.compile("(20\\d{2})[-年./](\\d{1,2})[-月./](\\d{1,2})");
    private static final Pattern SHORT_DATE_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,2})[-/.](\\d{1,2})(?!\\d)");
    private static final Pattern SINGLE_PLACEHOLDER_PATTERN = Pattern.compile("^\\{\\{\\s*([\\w.-]+)\\s*}}$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");
    private static final Pattern TRAILING_EXCHANGE_PATTERN = Pattern.compile("(?i)(?:\\.|_)?(SH|SZ|BJ|HK)$");
    private static final Pattern LEADING_EXCHANGE_PATTERN = Pattern.compile("(?i)^(SH|SZ|BJ|HK)");
    private static final Pattern DECISION_NO_PATTERN = Pattern.compile("([〔（(]?20\\d{2}[〕）)]?\\s*\\d+\\s*号)");
    private static final Pattern AGENCY_PATTERN = Pattern.compile("([^，。；;\\s]{2,40}(?:证监局|证券监督管理委员会|证券监督管理局|证券监管局|证券交易所|期货交易所|交易所|证监会))");
    private static final List<Pattern> MONEY_AMOUNT_PATTERNS = List.of(
            Pattern.compile("((?:罚款|没收违法所得|罚没|罚没款)(?:人民币)?[0-9０-９一二三四五六七八九十百千万亿点\\.,，、]+元)"),
            Pattern.compile("(处以(?:人民币)?[0-9０-９一二三四五六七八九十百千万亿点\\.,，、]+元罚款)")
    );
    private static final List<Pattern> SUBJECT_PATTERNS = List.of(
            Pattern.compile("(?:当事人|处罚对象|监管对象|被处罚人|被监管对象)[:：]?\\s*([^。；;\\n]{2,120})"),
            Pattern.compile("对([^，。；;\\n]{2,80})(?:采取|出具|给予|作出|责令|予以|实施)")
    );
    private static final List<String> VIOLATION_KEYWORDS = List.of(
            "违法",
            "违规",
            "信息披露",
            "内幕交易",
            "操纵",
            "未按规定",
            "欺诈发行",
            "虚假记载",
            "误导性陈述",
            "重大遗漏"
    );
    private static final List<RiskTypeRule> RISK_TYPE_RULES = List.of(
            new RiskTypeRule("市场禁入", "市场禁入"),
            new RiskTypeRule("行政处罚", "行政处罚"),
            new RiskTypeRule("处罚决定", "行政处罚"),
            new RiskTypeRule("立案调查", "立案调查"),
            new RiskTypeRule("责令改正", "责令改正"),
            new RiskTypeRule("警示函", "出具警示函"),
            new RiskTypeRule("监管措施", "监管措施"),
            new RiskTypeRule("监管谈话", "监管谈话"),
            new RiskTypeRule("纪律处分", "纪律处分")
    );
    private static final List<String> RISK_KEYWORDS = List.of(
            "行政处罚",
            "处罚决定",
            "市场禁入",
            "监管措施",
            "责令改正",
            "警示函",
            "立案调查",
            "纪律处分",
            "监管谈话"
    );
    private static final Set<String> SKIP_TITLES = Set.of(
            "更多",
            "首页",
            "返回顶部",
            "下一页",
            "上一页",
            "政务信息"
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
        int timeoutSeconds = resolveTimeoutSeconds(sourceConfig);

        HttpClient client = buildHttpClient(sourceConfig, timeoutSeconds);
        HttpRequest httpRequest = buildRequest(endpointUrl, timeoutSeconds, sourceConfig, request);
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("CSRC_RISK_HTTP_FAILED", "CSRC risk source sync failed, HTTP status: " + response.statusCode());
            }
            return parseResponse(response.body(), endpointUrl, client, timeoutSeconds, sourceConfig, request);
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("CSRC_RISK_REQUEST_FAILED", buildRequestFailureMessage("CSRC risk source request failed", e));
        }
    }

    @Override
    public List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = StringUtils.hasText(sourceConfig.getEndpointUrl())
                ? sourceConfig.getEndpointUrl().trim() : DEFAULT_ENDPOINT_URL;
        int timeoutSeconds = resolveTimeoutSeconds(sourceConfig);

        EventSourceRequestDiagnosticItemVO item = new EventSourceRequestDiagnosticItemVO();
        item.setStageCode("PRIMARY_REQUEST");
        item.setStageName(sourceConfig != null && Boolean.FALSE.equals(sourceConfig.getSslVerify())
                ? "CSRC Risk Request (SSL Verify Disabled)" : "CSRC Risk Request");
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
            throw new BizException("CSRC_RISK_SSL_CONTEXT_FAILED", buildRequestFailureMessage("CSRC risk SSL context initialization failed", e));
        }
    }

    private List<MarketEventCreateDTO> parseResponse(String responseBody,
                                                     String endpointUrl,
                                                     HttpClient client,
                                                     int timeoutSeconds,
                                                     EventSourceConfigItemVO sourceConfig,
                                                     MarketEventSourceSyncDTO request) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BizException("CSRC_RISK_RESPONSE_EMPTY", "CSRC risk response body is empty");
        }

        org.jsoup.nodes.Document document = Jsoup.parse(responseBody, endpointUrl);
        List<RiskLink> links = resolveRiskLinks(document);
        if (links.isEmpty()) {
            throw new BizException("CSRC_RISK_RESPONSE_ITEMS_EMPTY", "CSRC risk page contains no risk links");
        }

        int itemCount = request == null || request.getItemCount() == null || request.getItemCount() <= 0
                ? 10 : request.getItemCount();
        int maxScanCount = Math.max(itemCount * 8, 24);
        List<String> targetTokens = buildTargetTokens(request);
        List<MarketEventCreateDTO> result = new ArrayList<>();

        for (int index = 0; index < links.size() && index < maxScanCount; index++) {
            RiskLink link = links.get(index);
            if (result.size() >= itemCount) {
                break;
            }

            DetailContent detail = fetchDetail(link.href(), client, timeoutSeconds, sourceConfig, request);
            String combinedText = combineText(link.title(), detail.title(), detail.content());
            if (!targetTokens.isEmpty() && !containsAnyToken(combinedText, targetTokens)) {
                continue;
            }
            result.add(toMarketEvent(link, detail, sourceConfig, request, true));
        }

        if (result.isEmpty()) {
            return buildLatestRiskFallbackEvents(links, client, timeoutSeconds, sourceConfig, request, itemCount, maxScanCount);
        }
        return result;
    }

    private List<MarketEventCreateDTO> buildLatestRiskFallbackEvents(List<RiskLink> links,
                                                                     HttpClient client,
                                                                     int timeoutSeconds,
                                                                     EventSourceConfigItemVO sourceConfig,
                                                                     MarketEventSourceSyncDTO request,
                                                                     int itemCount,
                                                                     int maxScanCount) {
        List<MarketEventCreateDTO> result = new ArrayList<>();
        for (int index = 0; index < links.size() && index < maxScanCount; index++) {
            RiskLink link = links.get(index);
            DetailContent detail = fetchDetail(link.href(), client, timeoutSeconds, sourceConfig, request);
            result.add(toMarketEvent(link, detail, sourceConfig, request, false));
            if (result.size() >= itemCount) {
                break;
            }
        }
        if (result.isEmpty()) {
            throw new BizException("CSRC_RISK_RESPONSE_ITEMS_EMPTY", "CSRC risk page returned no importable risk items");
        }
        return result;
    }

    private List<RiskLink> resolveRiskLinks(org.jsoup.nodes.Document document) {
        Elements anchors = document == null ? new Elements() : document.select("a[href]");
        List<RiskLink> result = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        for (Element anchor : anchors) {
            String href = trimToNull(anchor.absUrl("href"));
            String title = trimToNull(anchor.text());
            if (!isRiskLink(href, title)) {
                continue;
            }
            String occurredDate = resolveOccurredDate(anchor);
            String fingerprint = href + "|" + title;
            if (!dedupe.add(fingerprint)) {
                continue;
            }
            result.add(new RiskLink(href, title, occurredDate));
        }
        return result;
    }

    private boolean isRiskLink(String href, String title) {
        if (!StringUtils.hasText(href) || !StringUtils.hasText(title)) {
            return false;
        }
        if (SKIP_TITLES.contains(title.trim())) {
            return false;
        }
        String normalizedHref = href.trim().toLowerCase(Locale.ROOT);
        if (!normalizedHref.contains("csrc.gov.cn")) {
            return false;
        }
        return RISK_KEYWORDS.stream().anyMatch(title::contains);
    }

    private DetailContent fetchDetail(String href,
                                      HttpClient client,
                                      int timeoutSeconds,
                                      EventSourceConfigItemVO sourceConfig,
                                      MarketEventSourceSyncDTO request) {
        if (!StringUtils.hasText(href)) {
            return new DetailContent(null, null, null);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(href))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, resolveRequestHeaders(sourceConfig, request));
        try {
            HttpResponse<String> response = client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new DetailContent(null, null, null);
            }
            org.jsoup.nodes.Document document = Jsoup.parse(response.body(), href);
            String title = firstText(document, "h1", ".title", ".article-title", "title");
            String date = matchDate(document.text());
            String content = extractDetailContent(document);
            return new DetailContent(title, date, normalizeWhitespace(content));
        } catch (Exception ignored) {
            return new DetailContent(null, null, null);
        }
    }

    private String extractDetailContent(org.jsoup.nodes.Document document) {
        if (document == null) {
            return null;
        }
        String content = firstText(
                document,
                ".TRS_Editor",
                "#zoom",
                ".article-content",
                ".article_content",
                ".content",
                ".detail_content",
                ".detailContent",
                ".content_con",
                ".xxgk_content",
                ".main-content",
                ".mainContent",
                "article"
        );
        if (StringUtils.hasText(content)) {
            return content;
        }
        return firstText(document, "body");
    }

    private String firstText(org.jsoup.nodes.Document document, String... selectors) {
        if (document == null || selectors == null) {
            return null;
        }
        for (String selector : selectors) {
            if (!StringUtils.hasText(selector)) {
                continue;
            }
            Element element = document.selectFirst(selector);
            String text = element == null ? null : trimToNull(element.text());
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private MarketEventCreateDTO toMarketEvent(RiskLink link,
                                               DetailContent detail,
                                               EventSourceConfigItemVO sourceConfig,
                                               MarketEventSourceSyncDTO request,
                                               boolean targetMatched) {
        String title = defaultIfBlank(detail.title(), link.title());
        String occurredDate = defaultIfBlank(detail.occurredDate(), link.occurredDate());
        RiskExtraction riskExtraction = extractRiskDetail(title, detail.content());
        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        dto.setTargetType(request == null ? "STOCK" : defaultIfBlank(request.getTargetType(), "STOCK"));
        dto.setTargetCode(request == null ? null : trimToNull(request.getTargetCode()));
        dto.setTargetName(request == null ? null : trimToNull(request.getTargetName()));
        dto.setEventType(defaultIfBlank(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "RISK_ALERT"));
        dto.setEventTitle(title);
        dto.setEventSummary(buildSummary(title, occurredDate, detail.content(), riskExtraction, targetMatched));
        dto.setSourceChannel(defaultIfBlank(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "RISK_MONITOR"));
        dto.setSourceUrl(link.href());
        dto.setImpactLevel(resolveImpactLevel(title, detail.content(), riskExtraction, sourceConfig));
        dto.setEventStatus("ACTIVE");
        dto.setOccurredAt(parseDateTime(occurredDate));
        return dto;
    }

    private String buildSummary(String title,
                                String occurredDate,
                                String content,
                                RiskExtraction riskExtraction,
                                boolean targetMatched) {
        StringBuilder summary = new StringBuilder();
        if (targetMatched) {
            summary.append("证监会公开监管风险信息");
        } else {
            summary.append("证监会最新监管风险背景信息，未命中该标的精确监管记录");
        }
        if (StringUtils.hasText(occurredDate)) {
            summary.append("（").append(occurredDate.trim()).append("）");
        }
        summary.append("：").append(title);
        String structuredDetail = buildStructuredRiskDetail(riskExtraction, targetMatched);
        if (StringUtils.hasText(structuredDetail)) {
            summary.append("。结构化要素：").append(structuredDetail);
        }
        String excerpt = buildRiskContentExcerpt(content, riskExtraction);
        if (StringUtils.hasText(excerpt)) {
            summary.append("。正文摘要：").append(excerpt);
        }
        return summary.toString();
    }

    private String buildStructuredRiskDetail(RiskExtraction riskExtraction, boolean targetMatched) {
        List<String> parts = new ArrayList<>();
        if (riskExtraction != null) {
            appendStructuredPart(parts, "监管类型", riskExtraction.regulatoryType());
            appendStructuredPart(parts, "监管机构", riskExtraction.agency());
            appendStructuredPart(parts, "处罚/监管对象", riskExtraction.subject());
            appendStructuredPart(parts, "罚没金额", riskExtraction.penaltyAmount());
            appendStructuredPart(parts, "文号", riskExtraction.decisionNo());
            appendStructuredPart(parts, "违规事项", riskExtraction.violationSummary());
        }
        parts.add("命中标的=" + (targetMatched ? "是" : "否"));
        return String.join("；", parts);
    }

    private void appendStructuredPart(List<String> parts, String label, String value) {
        String normalized = abbreviate(value, 120);
        if (StringUtils.hasText(normalized)) {
            parts.add(label + "=" + normalized);
        }
    }

    private String buildRiskContentExcerpt(String content, RiskExtraction riskExtraction) {
        if (riskExtraction != null && StringUtils.hasText(riskExtraction.violationSummary())) {
            return abbreviate(riskExtraction.violationSummary(), 260);
        }
        return abbreviate(content, 360);
    }

    private String resolveImpactLevel(String title,
                                      String content,
                                      RiskExtraction riskExtraction,
                                      EventSourceConfigItemVO sourceConfig) {
        String combinedText = combineText(
                title,
                content,
                riskExtraction == null ? null : riskExtraction.regulatoryType(),
                riskExtraction == null ? null : riskExtraction.penaltyAmount()
        );
        if (containsAnyKeyword(
                combinedText,
                List.of("行政处罚", "处罚决定", "市场禁入", "立案调查", "刑事", "移送司法", "罚没")
        )) {
            return "HIGH";
        }
        if (containsAnyKeyword(
                combinedText,
                List.of("警示函", "责令改正", "监管措施", "监管谈话", "纪律处分")
        )) {
            return "MEDIUM";
        }
        return defaultIfBlank(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "HIGH");
    }

    private RiskExtraction extractRiskDetail(String title, String content) {
        String combinedText = normalizeWhitespace(combineText(title, content));
        String regulatoryType = extractRegulatoryType(combinedText);
        String agency = extractAgency(combinedText);
        String subject = extractSubject(combinedText);
        String penaltyAmount = extractPenaltyAmount(combinedText);
        String decisionNo = extractFirstMatch(DECISION_NO_PATTERN, combinedText, 1);
        String violationSummary = extractViolationSummary(combinedText);
        return new RiskExtraction(
                regulatoryType,
                agency,
                subject,
                penaltyAmount,
                decisionNo,
                violationSummary
        );
    }

    private String extractRegulatoryType(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (RiskTypeRule rule : RISK_TYPE_RULES) {
            if (text.contains(rule.keyword())) {
                return rule.label();
            }
        }
        return null;
    }

    private String extractAgency(String text) {
        String agency = extractFirstMatch(AGENCY_PATTERN, text, 1);
        if (StringUtils.hasText(agency)) {
            return agency;
        }
        if (StringUtils.hasText(text) && text.contains("证监会")) {
            return "中国证监会";
        }
        return null;
    }

    private String extractPenaltyAmount(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (Pattern pattern : MONEY_AMOUNT_PATTERNS) {
            String amount = cleanupExtractedPhrase(extractFirstMatch(pattern, text, 1));
            if (StringUtils.hasText(amount)) {
                return amount;
            }
        }
        return null;
    }

    private String extractSubject(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (Pattern pattern : SUBJECT_PATTERNS) {
            String subject = cleanupExtractedPhrase(extractFirstMatch(pattern, text, 1));
            if (StringUtils.hasText(subject)) {
                return subject;
            }
        }
        return null;
    }

    private String extractViolationSummary(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String[] sentences = text.split("[。！？!?]");
        for (String sentence : sentences) {
            String normalized = cleanupExtractedPhrase(sentence);
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            if (containsAnyKeyword(normalized, VIOLATION_KEYWORDS)) {
                return normalized;
            }
        }
        return null;
    }

    private String extractFirstMatch(Pattern pattern, String text, int groupIndex) {
        if (pattern == null || !StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return trimToNull(matcher.group(groupIndex));
    }

    private boolean containsAnyKeyword(String text, List<String> keywords) {
        if (!StringUtils.hasText(text) || keywords == null || keywords.isEmpty()) {
            return false;
        }
        return keywords.stream()
                .filter(StringUtils::hasText)
                .anyMatch(text::contains);
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
        String value = trimToNull(text);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher fullMatcher = FULL_DATE_PATTERN.matcher(value);
        if (fullMatcher.find()) {
            int year = Integer.parseInt(fullMatcher.group(1));
            int month = Integer.parseInt(fullMatcher.group(2));
            int day = Integer.parseInt(fullMatcher.group(3));
            return formatDate(year, month, day);
        }
        Matcher shortMatcher = SHORT_DATE_PATTERN.matcher(value);
        if (shortMatcher.find()) {
            int month = Integer.parseInt(shortMatcher.group(1));
            int day = Integer.parseInt(shortMatcher.group(2));
            int year = Year.now().getValue();
            try {
                LocalDate candidate = LocalDate.of(year, month, day);
                if (candidate.isAfter(LocalDate.now().plusDays(7))) {
                    candidate = candidate.minusYears(1);
                }
                return candidate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String formatDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            return null;
        }
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
        return LocalDateTime.now();
    }

    private Map<String, String> resolveRequestHeaders(EventSourceConfigItemVO sourceConfig,
                                                      MarketEventSourceSyncDTO request) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Referer", "https://www.csrc.gov.cn/");

        JsonNode headerNode = parseJsonNode(
                sourceConfig == null ? null : sourceConfig.getRequestHeadersJson(),
                "CSRC_RISK_REQUEST_HEADERS_INVALID",
                "CSRC risk request headers parsing failed"
        );
        if (headerNode == null || headerNode.isNull()) {
            return headers;
        }
        if (!headerNode.isObject()) {
            throw new BizException("CSRC_RISK_REQUEST_HEADERS_INVALID", "CSRC risk request headers must be a JSON object");
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
                "CSRC_RISK_REQUEST_QUERY_INVALID",
                "CSRC risk request query parsing failed"
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
        String targetCode = defaultIfBlank(request == null ? null : request.getTargetCode(), "");
        String targetName = defaultIfBlank(request == null ? null : request.getTargetName(), "");
        String normalizedTargetCode = defaultIfBlank(normalizeTargetCode(targetCode), "");
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("sourceCode", sourceConfig == null ? null : sourceConfig.getSourceCode());
        variables.put("sourceName", sourceConfig == null ? null : sourceConfig.getSourceName());
        variables.put("sourceCategory", sourceConfig == null ? null : sourceConfig.getSourceCategory());
        variables.put("sourceChannel", sourceConfig == null ? null : sourceConfig.getSourceChannel());
        variables.put("targetType", request == null ? "STOCK" : defaultIfBlank(request.getTargetType(), "STOCK"));
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

    private List<String> buildTargetTokens(MarketEventSourceSyncDTO request) {
        List<String> tokens = new ArrayList<>();
        if (request == null) {
            return tokens;
        }
        addToken(tokens, request.getTargetName());
        addToken(tokens, request.getTargetCode());
        addToken(tokens, normalizeTargetCode(request.getTargetCode()));
        return tokens.stream().distinct().toList();
    }

    private void addToken(List<String> tokens, String value) {
        String normalized = trimToNull(value);
        if (StringUtils.hasText(normalized) && normalized.length() >= 2) {
            tokens.add(normalized);
        }
    }

    private boolean containsAnyToken(String text, List<String> tokens) {
        if (!StringUtils.hasText(text) || tokens == null || tokens.isEmpty()) {
            return false;
        }
        String normalizedText = text.toUpperCase(Locale.ROOT);
        return tokens.stream()
                .filter(StringUtils::hasText)
                .map(item -> item.toUpperCase(Locale.ROOT))
                .anyMatch(normalizedText::contains);
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

    private String normalizeWhitespace(String value) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized.replaceAll("\\s+", " ").trim() : null;
    }

    private String cleanupExtractedPhrase(String value) {
        String normalized = normalizeWhitespace(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        normalized = normalized
                .replaceAll("^[：:，,；;。\\s]+", "")
                .replaceAll("[：:，,；;。\\s]+$", "")
                .replaceAll("^关于对?", "")
                .trim();
        return StringUtils.hasText(normalized) ? normalized : null;
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

    private int resolveTimeoutSeconds(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
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

    private record RiskLink(String href, String title, String occurredDate) {
    }

    private record DetailContent(String title, String occurredDate, String content) {
    }

    private record RiskExtraction(String regulatoryType,
                                  String agency,
                                  String subject,
                                  String penaltyAmount,
                                  String decisionNo,
                                  String violationSummary) {
    }

    private record RiskTypeRule(String keyword, String label) {
    }
}
