package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EventSourceConfigPolicyManager {

    private final ObjectMapper objectMapper;

    public EventSourceConfigItemVO toSourceItem(Map<String, Object> item) {
        EventSourceConfigItemVO vo = new EventSourceConfigItemVO();
        vo.setSourceCode(normalize(item.get("sourceCode")));
        vo.setSourceName(normalize(item.get("sourceName")));
        vo.setSourceCategory(normalize(item.get("sourceCategory")));
        vo.setSourceChannel(normalize(item.get("sourceChannel")));
        vo.setIngestMode(normalize(item.get("ingestMode")));
        vo.setEnabled(readBoolean(item.get("enabled"), true));
        vo.setSupportsMockIngest(readBoolean(item.get("supportsMockIngest"), false));
        vo.setSslVerify(readBoolean(item.get("sslVerify"), true));
        vo.setEndpointUrl(normalize(item.get("endpointUrl")));
        vo.setRequestMethod(normalize(item.get("requestMethod")));
        vo.setRequestTimeoutSeconds(readInteger(item.get("requestTimeoutSeconds"), 15));
        vo.setRequestHeadersJson(normalize(item.get("requestHeadersJson")));
        vo.setRequestQueryJson(normalize(item.get("requestQueryJson")));
        vo.setRequestBodyJson(normalize(item.get("requestBodyJson")));
        vo.setResponseItemsField(normalize(item.get("responseItemsField")));
        vo.setFieldMappingJson(normalize(item.get("fieldMappingJson")));
        vo.setUpstreamUrl(normalize(item.get("upstreamUrl")));
        vo.setUpstreamMethod(normalize(item.get("upstreamMethod")));
        vo.setUpstreamHeadersJson(normalize(item.get("upstreamHeadersJson")));
        vo.setUpstreamQueryJson(normalize(item.get("upstreamQueryJson")));
        vo.setUpstreamBodyJson(normalize(item.get("upstreamBodyJson")));
        vo.setUpstreamItemsField(normalize(item.get("upstreamItemsField")));
        vo.setUpstreamFieldMappingJson(normalize(item.get("upstreamFieldMappingJson")));
        vo.setDefaultEventType(normalize(item.get("defaultEventType")));
        vo.setDefaultImpactLevel(normalize(item.get("defaultImpactLevel")));
        vo.setRemark(normalize(item.get("remark")));
        return vo;
    }

    public void validateSaveSource(String sourceCode, EventSourceConfigUpdateDTO dto) {
        if (!StringUtils.hasText(sourceCode)) {
            throw new BizException("EVENT_SOURCE_CODE_EMPTY", "event source code cannot be empty");
        }
        if (dto == null) {
            throw new BizException("EVENT_SOURCE_CONFIG_EMPTY", "event source config cannot be empty");
        }
        if (!StringUtils.hasText(dto.getSourceName())) {
            throw new BizException("EVENT_SOURCE_NAME_EMPTY", "event source name cannot be empty");
        }
        if (!StringUtils.hasText(dto.getSourceCategory())) {
            throw new BizException("EVENT_SOURCE_CATEGORY_EMPTY", "event source category cannot be empty");
        }
        if (!StringUtils.hasText(dto.getSourceChannel())) {
            throw new BizException("EVENT_SOURCE_CHANNEL_EMPTY", "event source channel cannot be empty");
        }
        if (!StringUtils.hasText(dto.getIngestMode())) {
            throw new BizException("EVENT_SOURCE_INGEST_MODE_EMPTY", "event source ingest mode cannot be empty");
        }
        if (!StringUtils.hasText(dto.getDefaultEventType())) {
            throw new BizException("EVENT_SOURCE_DEFAULT_EVENT_TYPE_EMPTY", "default event type cannot be empty");
        }
        if (!StringUtils.hasText(dto.getDefaultImpactLevel())) {
            throw new BizException("EVENT_SOURCE_DEFAULT_IMPACT_EMPTY", "default impact level cannot be empty");
        }
        validateHeadersJson(dto.getRequestHeadersJson(), "EVENT_SOURCE_REQUEST_HEADERS_INVALID", "event source request headers must be a JSON object");
        validateHeadersJson(dto.getUpstreamHeadersJson(), "EVENT_SOURCE_UPSTREAM_HEADERS_INVALID", "event source upstream headers must be a JSON object");
        validateObjectJson(dto.getRequestQueryJson(), "EVENT_SOURCE_REQUEST_QUERY_INVALID", "event source request query must be a JSON object");
        validateJson(dto.getRequestBodyJson(), "EVENT_SOURCE_REQUEST_BODY_INVALID", "event source request body must be valid JSON");
        validateFieldMappingJson(dto.getFieldMappingJson(), "EVENT_SOURCE_FIELD_MAPPING_INVALID", "event source field mapping must be a JSON object");
        validateObjectJson(dto.getUpstreamQueryJson(), "EVENT_SOURCE_UPSTREAM_QUERY_INVALID", "event source upstream query must be a JSON object");
        validateJson(dto.getUpstreamBodyJson(), "EVENT_SOURCE_UPSTREAM_BODY_INVALID", "event source upstream body must be valid JSON");
        validateFieldMappingJson(dto.getUpstreamFieldMappingJson(), "EVENT_SOURCE_UPSTREAM_FIELD_MAPPING_INVALID", "event source upstream field mapping must be a JSON object");

        String ingestMode = dto.getIngestMode().trim().toUpperCase();
        if (("HTTP_JSON".equalsIgnoreCase(ingestMode)
                || "RSS_XML".equalsIgnoreCase(ingestMode)
                || "GOV_CN_POLICY_HTML".equalsIgnoreCase(ingestMode)
                || "CSRC_RISK_HTML".equalsIgnoreCase(ingestMode)
                || "CNINFO_PROXY".equalsIgnoreCase(ingestMode)
                || "CNINFO_PUBLIC_CRAWLER".equalsIgnoreCase(ingestMode))
                && Boolean.TRUE.equals(dto.getEnabled())
                && !StringUtils.hasText(dto.getEndpointUrl())) {
            throw new BizException("EVENT_SOURCE_ENDPOINT_URL_EMPTY", "event source endpoint url cannot be empty");
        }
    }

    public Map<String, Object> applyUpdate(Map<String, Object> item, EventSourceConfigUpdateDTO dto) {
        Map<String, Object> before = new LinkedHashMap<>(item);
        String ingestMode = dto.getIngestMode().trim().toUpperCase();
        item.put("sourceName", dto.getSourceName().trim());
        item.put("sourceCategory", dto.getSourceCategory().trim());
        item.put("sourceChannel", dto.getSourceChannel().trim());
        item.put("ingestMode", ingestMode);
        item.put("enabled", dto.getEnabled() == null || Boolean.TRUE.equals(dto.getEnabled()));
        item.put("supportsMockIngest", dto.getSupportsMockIngest() != null && dto.getSupportsMockIngest());
        item.put("sslVerify", dto.getSslVerify() == null || Boolean.TRUE.equals(dto.getSslVerify()));
        item.put("endpointUrl", normalize(dto.getEndpointUrl()));
        item.put("requestMethod", StringUtils.hasText(dto.getRequestMethod()) ? dto.getRequestMethod().trim().toUpperCase() : "GET");
        item.put("requestTimeoutSeconds", dto.getRequestTimeoutSeconds() == null || dto.getRequestTimeoutSeconds() <= 0 ? 15 : dto.getRequestTimeoutSeconds());
        item.put("requestHeadersJson", normalize(dto.getRequestHeadersJson()));
        item.put("requestQueryJson", normalize(dto.getRequestQueryJson()));
        item.put("requestBodyJson", normalize(dto.getRequestBodyJson()));
        item.put("responseItemsField", normalize(dto.getResponseItemsField()));
        item.put("fieldMappingJson", normalize(dto.getFieldMappingJson()));
        item.put("upstreamUrl", normalize(dto.getUpstreamUrl()));
        item.put("upstreamMethod", StringUtils.hasText(dto.getUpstreamMethod()) ? dto.getUpstreamMethod().trim().toUpperCase() : "GET");
        item.put("upstreamHeadersJson", normalize(dto.getUpstreamHeadersJson()));
        item.put("upstreamQueryJson", normalize(dto.getUpstreamQueryJson()));
        item.put("upstreamBodyJson", normalize(dto.getUpstreamBodyJson()));
        item.put("upstreamItemsField", normalize(dto.getUpstreamItemsField()));
        item.put("upstreamFieldMappingJson", normalize(dto.getUpstreamFieldMappingJson()));
        item.put("defaultEventType", dto.getDefaultEventType().trim());
        item.put("defaultImpactLevel", dto.getDefaultImpactLevel().trim());
        item.put("remark", normalize(dto.getRemark()));
        return before;
    }

    public List<String> diffFields(Map<String, Object> before, Map<String, Object> after) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        fields.addAll(before.keySet());
        fields.addAll(after.keySet());
        List<String> result = new ArrayList<>();
        for (String field : fields) {
            if (!Objects.equals(before.get(field), after.get(field))) {
                result.add(field);
            }
        }
        return result;
    }

    public String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean readBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        return "true".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private Integer readInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private void validateHeadersJson(String rawHeadersJson, String errorCode, String errorMessage) {
        validateObjectJson(rawHeadersJson, errorCode, errorMessage);
    }

    private void validateObjectJson(String rawJson, String errorCode, String errorMessage) {
        String value = normalize(rawJson);
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            if (!objectMapper.readTree(value).isObject()) {
                throw new BizException(errorCode, errorMessage);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(errorCode, errorMessage);
        }
    }

    private void validateJson(String rawJson, String errorCode, String errorMessage) {
        String value = normalize(rawJson);
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            objectMapper.readTree(value);
        } catch (Exception e) {
            throw new BizException(errorCode, errorMessage);
        }
    }

    private void validateFieldMappingJson(String rawJson, String errorCode, String errorMessage) {
        validateObjectJson(rawJson, errorCode, errorMessage);
    }
}
