package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CninfoPublicAnnouncementPayloadManager {

    private static final String DEFAULT_REFERER = "https://www.cninfo.com.cn/new/commonUrl/pageOfSearch?url=disclosure/list/search";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern TRAILING_EXCHANGE_PATTERN = Pattern.compile("(?i)(?:\\.|_)?(SH|SZ|BJ|HK)$");
    private static final Pattern LEADING_EXCHANGE_PATTERN = Pattern.compile("(?i)^(SH|SZ|BJ|HK)");

    private final ObjectMapper objectMapper;
    private final EventSourceRequestTemplateManager requestTemplateManager;

    public List<String> resolveSearchKeywords(MarketEventSourceSyncDTO request) {
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

    public Map<String, String> resolveRequestHeaders(EventSourceConfigItemVO sourceConfig,
                                                     MarketEventSourceSyncDTO request,
                                                     String searchKeyword) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("Referer", DEFAULT_REFERER);
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("User-Agent", "Mozilla/5.0");
        headers.put("Accept", "application/json, text/plain, */*");

        Object rendered = requestTemplateManager.renderRequestTemplate(
                sourceConfig == null ? null : sourceConfig.getRequestHeadersJson(),
                request,
                sourceConfig,
                buildTemplateVariables(request, sourceConfig, searchKeyword),
                "CNINFO_PUBLIC_REQUEST_HEADERS_INVALID",
                "Cninfo public announcement request headers parsing failed"
        );
        if (rendered == null) {
            return headers;
        }
        if (!(rendered instanceof Map<?, ?> renderedMap)) {
            throw new BizException("CNINFO_PUBLIC_REQUEST_HEADERS_INVALID", "Cninfo public announcement request headers must be a JSON object");
        }
        renderedMap.forEach((key, value) -> {
            if (key != null && value != null && StringUtils.hasText(String.valueOf(key))) {
                headers.put(String.valueOf(key).trim(), String.valueOf(value).trim());
            }
        });
        return headers;
    }

    public String buildRequestBody(EventSourceConfigItemVO sourceConfig,
                                   MarketEventSourceSyncDTO request,
                                   String searchKeyword) {
        Map<String, Object> params = buildDefaultRequestParams(request, sourceConfig, searchKeyword);
        Object rendered = requestTemplateManager.renderRequestTemplate(
                sourceConfig == null ? null : sourceConfig.getRequestBodyJson(),
                request,
                sourceConfig,
                buildTemplateVariables(request, sourceConfig, searchKeyword),
                "CNINFO_PUBLIC_REQUEST_BODY_INVALID",
                "Cninfo public announcement request body parsing failed"
        );
        if (rendered != null) {
            if (!(rendered instanceof Map<?, ?> renderedMap)) {
                throw new BizException("CNINFO_PUBLIC_REQUEST_BODY_INVALID", "Cninfo public announcement request body must be a JSON object");
            }
            renderedMap.forEach((key, value) -> {
                if (key != null && StringUtils.hasText(String.valueOf(key))) {
                    params.put(String.valueOf(key), value);
                }
            });
        }
        return encodeFormBody(params);
    }

    public List<EventSourceRequestDiagnosticItemVO> buildDiagnostics(String endpointUrl,
                                                                    int timeoutSeconds,
                                                                    EventSourceConfigItemVO sourceConfig,
                                                                    MarketEventSourceSyncDTO request) {
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
            return "Cninfo Public Announcement Request (Code Search)";
        }
        if (StringUtils.hasText(targetName) && targetName.equals(searchKeyword)) {
            return index == 0 ? "Cninfo Public Announcement Request (Name Search)" : "Cninfo Public Announcement Request (Name Fallback)";
        }
        return index == 0 ? "Cninfo Public Announcement Request" : "Cninfo Public Announcement Request (Fallback)";
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
}
