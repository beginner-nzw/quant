package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RssXmlEventItemAssembler {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern FULL_DATE_PATTERN = Pattern.compile("(20\\d{2})[-年./](\\d{1,2})[-月./](\\d{1,2})");
    private static final Pattern COMPACT_DATE_PATTERN = Pattern.compile("(?<!\\d)(20\\d{2})(\\d{2})(\\d{2})(?!\\d)");

    private final RssXmlElementReadManager elementReadManager;

    public MarketEventCreateDTO toMarketEventFromHtml(org.jsoup.nodes.Element anchor,
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

    public MarketEventCreateDTO toMarketEvent(Element itemElement,
                                              EventSourceConfigItemVO sourceConfig,
                                              MarketEventSourceSyncDTO request) {
        Map<String, List<String>> fieldMappings = elementReadManager.parseFieldMappings(sourceConfig == null ? null : sourceConfig.getFieldMappingJson());
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
        return elementReadManager.readMappedText(itemElement, fieldMappings, canonicalField, fallbackFieldNames);
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

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
