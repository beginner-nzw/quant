package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.common.core.exception.BizException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GovCnPolicyProjectionManager {

    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2})[-年./](\\d{1,2})[-月./](\\d{1,2})");
    private static final Set<String> SKIP_TITLES = Set.of(
            "更多",
            "返回顶部",
            "首页",
            "下一页",
            "上一页"
    );

    public List<MarketEventCreateDTO> parseResponse(String responseBody,
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

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

}
