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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CninfoProxyEventProjectionManager {

    private final ObjectMapper objectMapper;

    public List<MarketEventCreateDTO> parseResponse(String responseBody,
                                                     EventSourceConfigItemVO sourceConfig,
                                                     MarketEventSourceSyncDTO request,
                                                     String endpointUrl) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode itemsNode = resolveItemsNode(root, trimToNull(sourceConfig == null ? null : sourceConfig.getResponseItemsField()));
            if (itemsNode == null || !itemsNode.isArray()) {
                throw new BizException("CNINFO_PROXY_ITEMS_EMPTY", "巨潮公告包装接口未返回公告列表");
            }

            List<MarketEventCreateDTO> result = new ArrayList<>();
            for (JsonNode itemNode : itemsNode) {
                if (itemNode != null && itemNode.isObject()) {
                    result.add(toMarketEvent(itemNode, sourceConfig, request, endpointUrl));
                }
            }
            if (result.isEmpty()) {
                throw new BizException("CNINFO_PROXY_ITEMS_EMPTY", "巨潮公告包装接口未返回可导入公告");
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("CNINFO_PROXY_PARSE_FAILED", "巨潮公告包装接口响应解析失败");
        }
    }

    private JsonNode resolveItemsNode(JsonNode root, String responseItemsField) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (StringUtils.hasText(responseItemsField)) {
            JsonNode node = resolvePath(root, responseItemsField);
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

    private MarketEventCreateDTO toMarketEvent(JsonNode itemNode,
                                               EventSourceConfigItemVO sourceConfig,
                                               MarketEventSourceSyncDTO request,
                                               String endpointUrl) {
        Map<String, List<String>> fieldMappings = parseFieldMappings(sourceConfig == null ? null : sourceConfig.getFieldMappingJson());
        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        dto.setTargetType(defaultValue(readMappedText(itemNode, fieldMappings, "targetType", "targetType", "assetType"), defaultValue(request == null ? null : request.getTargetType(), "STOCK")));
        dto.setTargetCode(defaultValue(readMappedText(itemNode, fieldMappings, "targetCode", "targetCode", "secCode", "symbol", "ticker", "stockCode", "secucode"), request == null ? null : request.getTargetCode()));
        dto.setTargetName(defaultValue(readMappedText(itemNode, fieldMappings, "targetName", "targetName", "secName", "secShortName", "shortName", "stockName"), request == null ? null : request.getTargetName()));
        dto.setEventType(defaultValue(readMappedText(itemNode, fieldMappings, "eventType", "eventType", "eventCategory"), defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "ANNOUNCEMENT")));
        dto.setEventTitle(defaultValue(readMappedText(itemNode, fieldMappings, "eventTitle", "eventTitle", "announcementTitle", "title", "noticeTitle", "headline"), "未命名公告"));
        dto.setEventSummary(defaultValue(readMappedText(itemNode, fieldMappings, "eventSummary", "eventSummary", "announcementSummary", "summary", "contentAbstract", "description"), dto.getEventTitle()));
        dto.setSourceChannel(defaultValue(readMappedText(itemNode, fieldMappings, "sourceChannel", "sourceChannel", "channel"), defaultValue(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "EXCHANGE_FEED")));
        dto.setSourceUrl(resolveSourceUrl(readMappedText(itemNode, fieldMappings, "sourceUrl", "sourceUrl", "announcementUrl", "adjunctUrl", "url", "link"), endpointUrl));
        dto.setImpactLevel(defaultValue(readMappedText(itemNode, fieldMappings, "impactLevel", "impactLevel", "importance", "level"), defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "HIGH")));
        dto.setEventStatus(defaultValue(readMappedText(itemNode, fieldMappings, "eventStatus", "eventStatus", "status"), "ACTIVE"));
        dto.setOccurredAt(parseDateTime(readMappedText(itemNode, fieldMappings, "occurredAt", "occurredAt", "announcementTime", "publishTime", "announcementDate", "disclosureTime", "time")));
        return dto;
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
                throw new BizException("CNINFO_PROXY_FIELD_MAPPING_INVALID", "巨潮公告字段映射配置不是合法 JSON 对象");
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
            throw new BizException("CNINFO_PROXY_FIELD_MAPPING_INVALID", "巨潮公告字段映射配置解析失败");
        }
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
            URI baseUri = URI.create(endpointUrl);
            return baseUri.resolve(value).toString();
        } catch (Exception ignored) {
            return value;
        }
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
        List<DateTimeFormatter> dateTimeFormatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        List<DateTimeFormatter> dateFormatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.BASIC_ISO_DATE
        );
        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                return LocalDate.parse(value, formatter).atStartOfDay();
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
