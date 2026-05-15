package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;
import com.quant.common.core.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EventSourceConfigService {

    private final String configPath;
    private final ObjectMapper objectMapper;
    private final ConfigChangeAuditService configChangeAuditService;

    public EventSourceConfigService(
            @Value("${quant.ai.event-source-config:../../../ai-config/event-source-configs.json}") String configPath,
            ObjectMapper objectMapper,
            ConfigChangeAuditService configChangeAuditService
    ) {
        this.configPath = configPath;
        this.objectMapper = objectMapper;
        this.configChangeAuditService = configChangeAuditService;
    }

    public EventSourceConfigVO loadConfigView() {
        EventSourceConfigVO vo = new EventSourceConfigVO();
        vo.setConfigPath(resolveConfigPathForDisplay());
        vo.setSources(loadSources());
        return vo;
    }

    public List<EventSourceConfigItemVO> loadSources() {
        List<Map<String, Object>> sourceMaps = readSourceMaps(readRootConfig().get("sources"));
        List<EventSourceConfigItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : sourceMaps) {
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
            result.add(vo);
        }
        return result;
    }

    public EventSourceConfigItemVO findSource(String sourceCode) {
        if (!StringUtils.hasText(sourceCode)) {
            return null;
        }
        return loadSources().stream()
                .filter(item -> sourceCode.trim().equalsIgnoreCase(item.getSourceCode()))
                .findFirst()
                .orElse(null);
    }

    public void saveSource(String sourceCode, EventSourceConfigUpdateDTO dto) {
        if (!StringUtils.hasText(sourceCode)) {
            throw new BizException("EVENT_SOURCE_CODE_EMPTY", "事件源编码不能为空");
        }
        if (dto == null) {
            throw new BizException("EVENT_SOURCE_CONFIG_EMPTY", "事件源配置不能为空");
        }
        if (!StringUtils.hasText(dto.getSourceName())) {
            throw new BizException("EVENT_SOURCE_NAME_EMPTY", "事件源名称不能为空");
        }
        if (!StringUtils.hasText(dto.getSourceCategory())) {
            throw new BizException("EVENT_SOURCE_CATEGORY_EMPTY", "事件源分类不能为空");
        }
        if (!StringUtils.hasText(dto.getSourceChannel())) {
            throw new BizException("EVENT_SOURCE_CHANNEL_EMPTY", "来源渠道不能为空");
        }
        if (!StringUtils.hasText(dto.getIngestMode())) {
            throw new BizException("EVENT_SOURCE_INGEST_MODE_EMPTY", "接入模式不能为空");
        }
        if (!StringUtils.hasText(dto.getDefaultEventType())) {
            throw new BizException("EVENT_SOURCE_DEFAULT_EVENT_TYPE_EMPTY", "默认事件类型不能为空");
        }
        if (!StringUtils.hasText(dto.getDefaultImpactLevel())) {
            throw new BizException("EVENT_SOURCE_DEFAULT_IMPACT_EMPTY", "默认影响等级不能为空");
        }
        validateHeadersJson(dto.getRequestHeadersJson(), "EVENT_SOURCE_REQUEST_HEADERS_INVALID", "事件源请求头必须是 JSON 对象");
        validateHeadersJson(dto.getUpstreamHeadersJson(), "EVENT_SOURCE_UPSTREAM_HEADERS_INVALID", "上游请求头必须是 JSON 对象");
        validateObjectJson(dto.getRequestQueryJson(), "EVENT_SOURCE_REQUEST_QUERY_INVALID", "事件源查询参数必须是 JSON 对象");
        validateJson(dto.getRequestBodyJson(), "EVENT_SOURCE_REQUEST_BODY_INVALID", "事件源请求体必须是合法 JSON");
        validateFieldMappingJson(dto.getFieldMappingJson(), "EVENT_SOURCE_FIELD_MAPPING_INVALID", "响应字段映射必须是 JSON 对象");
        validateObjectJson(dto.getUpstreamQueryJson(), "EVENT_SOURCE_UPSTREAM_QUERY_INVALID", "上游查询参数必须是 JSON 对象");
        validateJson(dto.getUpstreamBodyJson(), "EVENT_SOURCE_UPSTREAM_BODY_INVALID", "上游请求体必须是合法 JSON");
        validateFieldMappingJson(dto.getUpstreamFieldMappingJson(), "EVENT_SOURCE_UPSTREAM_FIELD_MAPPING_INVALID", "上游字段映射必须是 JSON 对象");

        String ingestMode = dto.getIngestMode().trim().toUpperCase();
        if (("HTTP_JSON".equalsIgnoreCase(ingestMode)
                || "RSS_XML".equalsIgnoreCase(ingestMode)
                || "GOV_CN_POLICY_HTML".equalsIgnoreCase(ingestMode)
                || "CSRC_RISK_HTML".equalsIgnoreCase(ingestMode)
                || "CNINFO_PROXY".equalsIgnoreCase(ingestMode)
                || "CNINFO_PUBLIC_CRAWLER".equalsIgnoreCase(ingestMode))
                && Boolean.TRUE.equals(dto.getEnabled())
                && !StringUtils.hasText(dto.getEndpointUrl())) {
            throw new BizException("EVENT_SOURCE_ENDPOINT_URL_EMPTY", "HTTP 事件源地址不能为空");
        }

        Path path = resolveConfigPath();
        Map<String, Object> root = readRootConfig();
        List<Map<String, Object>> sources = readSourceMaps(root.get("sources"));
        boolean updated = false;

        for (Map<String, Object> item : sources) {
            if (sourceCode.trim().equalsIgnoreCase(normalize(item.get("sourceCode")))) {
                Map<String, Object> before = new LinkedHashMap<>(item);
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

                configChangeAuditService.appendAudit(
                        "EVENT_SOURCE_CONFIG",
                        sourceCode.trim(),
                        dto.getSourceName().trim(),
                        "UPDATE",
                        path.toString(),
                        "更新事件源配置",
                        diffFields(before, item)
                );
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new BizException("EVENT_SOURCE_NOT_FOUND", "未找到事件源配置: " + sourceCode);
        }

        root.put("sources", sources);
        writeRootConfig(path, root);
    }

    public String resolveConfigPathForDisplay() {
        return resolveConfigPath().toString();
    }

    private Map<String, Object> readRootConfig() {
        Path path = resolveConfigPath();
        if (!Files.exists(path)) {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("sources", new ArrayList<>());
            return root;
        }
        try {
            return objectMapper.readValue(
                    Files.readString(path, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new BizException("EVENT_SOURCE_CONFIG_READ_FAILED", "读取事件源配置失败");
        }
    }

    private void writeRootConfig(Path path, Map<String, Object> root) {
        try {
            Files.createDirectories(path.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(path, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("EVENT_SOURCE_CONFIG_SAVE_FAILED", "保存事件源配置失败");
        }
    }

    private List<Map<String, Object>> readSourceMaps(Object value) {
        if (!(value instanceof List<?> rawSources)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object rawSource : rawSources) {
            if (rawSource instanceof Map<?, ?> rawMap) {
                result.add(new LinkedHashMap<>(objectMapper.convertValue(
                        rawMap,
                        new TypeReference<LinkedHashMap<String, Object>>() {}
                )));
            }
        }
        return result;
    }

    private List<String> diffFields(Map<String, Object> before, Map<String, Object> after) {
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
        String value = normalize(rawHeadersJson);
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

    private Path resolveConfigPath() {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(configPath);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("ai-config").resolve("event-source-configs.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("event-source-configs.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("event-source-configs.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("event-source-configs.json").normalize());
            current = current.getParent();
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return candidates.iterator().next();
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
