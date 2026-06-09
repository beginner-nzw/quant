package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CsrcRiskDetailFetchManager {

    private static final Pattern FULL_DATE_PATTERN = Pattern.compile("(20\\d{2})[-年./](\\d{1,2})[-月./](\\d{1,2})");
    private static final Pattern SHORT_DATE_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,2})[-/.](\\d{1,2})(?!\\d)");

    private final EventSourceRequestTemplateManager requestTemplateManager;

    public DetailContent fetchDetail(String href,
                                     HttpClient client,
                                     int timeoutSeconds,
                                     EventSourceConfigItemVO sourceConfig,
                                     MarketEventSourceSyncDTO request) {
        if (!StringUtils.hasText(href)) {
            return DetailContent.empty();
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(href))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, resolveRequestHeaders(sourceConfig, request));
        try {
            HttpResponse<String> response = client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return DetailContent.empty();
            }
            org.jsoup.nodes.Document document = Jsoup.parse(response.body(), href);
            String title = firstText(document, "h1", ".title", ".article-title", "title");
            String date = matchDate(document.text());
            String content = extractDetailContent(document);
            return new DetailContent(title, date, normalizeWhitespace(content));
        } catch (Exception ignored) {
            return DetailContent.empty();
        }
    }

    private Map<String, String> resolveRequestHeaders(EventSourceConfigItemVO sourceConfig,
                                                      MarketEventSourceSyncDTO request) {
        Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.putAll(requestTemplateManager.parseHeaders(
                sourceConfig == null ? null : sourceConfig.getRequestHeadersJson(),
                "CSRC_RISK_REQUEST_HEADERS_INVALID",
                "CSRC risk request headers parsing failed",
                "CSRC risk request headers must be a JSON object"
        ));
        return headers;
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

    private String matchDate(String text) {
        String value = trimToNull(text);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher fullMatcher = FULL_DATE_PATTERN.matcher(value);
        if (fullMatcher.find()) {
            return formatDate(fullMatcher.group(1), fullMatcher.group(2), fullMatcher.group(3));
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

    private String normalizeWhitespace(String value) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized.replaceAll("\\s+", " ").trim() : null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record DetailContent(String title, String occurredDate, String content) {

        private static DetailContent empty() {
            return new DetailContent(null, null, null);
        }
    }
}
