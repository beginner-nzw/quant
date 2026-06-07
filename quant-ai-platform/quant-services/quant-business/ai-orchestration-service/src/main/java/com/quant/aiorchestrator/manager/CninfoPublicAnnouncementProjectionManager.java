package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CninfoPublicAnnouncementProjectionManager {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final ObjectMapper objectMapper;

    public List<MarketEventCreateDTO> parseResponse(String responseBody,
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
