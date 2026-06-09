package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CninfoProxyAnnouncementParserManager {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;

    public List<CninfoProxyAnnouncementItemVO> parseUpstreamResponse(String responseBody,
                                                                     EventSourceConfigItemVO sourceConfig) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode itemsNode = resolveItemsNode(root, trimToNull(sourceConfig == null ? null : sourceConfig.getUpstreamItemsField()));
            if (itemsNode == null || !itemsNode.isArray()) {
                throw new BizException("CNINFO_PROXY_UPSTREAM_ITEMS_EMPTY", "巨潮上游接口未返回公告列表");
            }

            Map<String, List<String>> fieldMappings = parseFieldMappings(sourceConfig == null ? null : sourceConfig.getUpstreamFieldMappingJson());
            List<CninfoProxyAnnouncementItemVO> items = new ArrayList<>();
            for (JsonNode itemNode : itemsNode) {
                if (itemNode != null && itemNode.isObject()) {
                    items.add(toAnnouncementItem(itemNode, fieldMappings));
                }
            }
            return items;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("CNINFO_PROXY_UPSTREAM_PARSE_FAILED", "巨潮上游接口响应解析失败");
        }
    }

    private JsonNode resolveItemsNode(JsonNode root, String upstreamItemsField) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (StringUtils.hasText(upstreamItemsField)) {
            JsonNode node = resolvePath(root, upstreamItemsField);
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

    private CninfoProxyAnnouncementItemVO toAnnouncementItem(JsonNode itemNode,
                                                             Map<String, List<String>> fieldMappings) {
        CninfoProxyAnnouncementItemVO item = new CninfoProxyAnnouncementItemVO();
        item.setSecCode(defaultValue(readMappedText(itemNode, fieldMappings, "secCode", "secCode", "targetCode", "stockCode", "symbol"), null));
        item.setSecName(defaultValue(readMappedText(itemNode, fieldMappings, "secName", "secName", "targetName", "stockName", "shortName"), null));
        item.setAnnouncementTitle(defaultValue(readMappedText(itemNode, fieldMappings, "announcementTitle", "announcementTitle", "eventTitle", "title", "headline"), "未命名公告"));
        item.setAnnouncementSummary(defaultValue(readMappedText(itemNode, fieldMappings, "announcementSummary", "announcementSummary", "eventSummary", "summary", "description"), item.getAnnouncementTitle()));
        item.setAnnouncementUrl(defaultValue(readMappedText(itemNode, fieldMappings, "announcementUrl", "announcementUrl", "sourceUrl", "url", "link"), null));
        item.setAnnouncementTime(formatAnnouncementTime(readMappedText(itemNode, fieldMappings, "announcementTime", "announcementTime", "occurredAt", "publishTime", "time", "disclosureTime")));
        item.setImportance(defaultValue(readMappedText(itemNode, fieldMappings, "importance", "importance", "impactLevel", "level"), "HIGH"));
        return item;
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
                throw new BizException("CNINFO_PROXY_UPSTREAM_FIELD_MAPPING_INVALID", "巨潮上游字段映射配置不是合法 JSON 对象");
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
            throw new BizException("CNINFO_PROXY_UPSTREAM_FIELD_MAPPING_INVALID", "巨潮上游字段映射配置解析失败");
        }
    }

    private String formatAnnouncementTime(String rawValue) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now().format(DATETIME_FORMATTER);
        }
        LocalDateTime dateTime = parseDateTime(value);
        return dateTime.format(DATETIME_FORMATTER);
    }

    private LocalDateTime parseDateTime(String rawValue) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
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
