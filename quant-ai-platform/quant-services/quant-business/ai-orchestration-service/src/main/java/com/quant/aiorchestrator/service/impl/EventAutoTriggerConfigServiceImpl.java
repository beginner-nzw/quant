package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.*;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService.EventAutoTriggerConfig;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService.EventAutoTriggerRule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerConfigVO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerRuleItemVO;
import com.quant.common.core.exception.BizException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EventAutoTriggerConfigServiceImpl implements EventAutoTriggerConfigService {

    private final String configPath;
    private final ObjectMapper objectMapper;
    private final ConfigChangeAuditService configChangeAuditService;

    public EventAutoTriggerConfigServiceImpl(
            @Value("${quant.ai.event-auto-trigger-config:../../../ai-config/event-auto-trigger-configs.json}") String configPath,
            ObjectMapper objectMapper,
            ConfigChangeAuditService configChangeAuditService
    ) {
        this.configPath = configPath;
        this.objectMapper = objectMapper;
        this.configChangeAuditService = configChangeAuditService;
    }

    public EventAutoTriggerConfig loadConfig() {
        Map<String, Object> root = readRootConfig();
        EventAutoTriggerConfig config = new EventAutoTriggerConfig();
        config.setEnabled(readBoolean(root.get("enabled"), false));
        config.setRules(readRules(root.get("rules")));
        return config;
    }

    public EventAutoTriggerConfigVO loadConfigView() {
        EventAutoTriggerConfig config = loadConfig();
        EventAutoTriggerConfigVO vo = new EventAutoTriggerConfigVO();
        vo.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        vo.setConfigPath(resolveConfigPathForDisplay());
        vo.setRules(config.getRules().stream().map(this::toRuleItem).toList());
        return vo;
    }

    public void saveRule(String ruleCode, EventAutoTriggerRuleUpdateDTO dto) {
        if (dto == null) {
            throw new BizException("EVENT_AUTO_TRIGGER_RULE_EMPTY", "事件自动触发规则不能为空");
        }
        if (!StringUtils.hasText(ruleCode)) {
            throw new BizException("EVENT_AUTO_TRIGGER_RULE_CODE_EMPTY", "事件自动触发规则编码不能为空");
        }
        if (!StringUtils.hasText(dto.getRuleName())) {
            throw new BizException("EVENT_AUTO_TRIGGER_RULE_NAME_EMPTY", "事件自动触发规则名称不能为空");
        }
        if (sanitizeList(dto.getEventTypes()).isEmpty()) {
            throw new BizException("EVENT_AUTO_TRIGGER_EVENT_TYPES_EMPTY", "事件类型不能为空");
        }
        if (sanitizeList(dto.getImpactLevels()).isEmpty()) {
            throw new BizException("EVENT_AUTO_TRIGGER_IMPACT_LEVELS_EMPTY", "影响等级不能为空");
        }
        if (!StringUtils.hasText(dto.getTaskType())) {
            throw new BizException("EVENT_AUTO_TRIGGER_TASK_TYPE_EMPTY", "任务类型不能为空");
        }
        if (!StringUtils.hasText(dto.getAnalysisScope())) {
            throw new BizException("EVENT_AUTO_TRIGGER_ANALYSIS_SCOPE_EMPTY", "分析范围不能为空");
        }
        if (!StringUtils.hasText(dto.getPriority())) {
            throw new BizException("EVENT_AUTO_TRIGGER_PRIORITY_EMPTY", "任务优先级不能为空");
        }
        if (!StringUtils.hasText(dto.getSourceChannel())) {
            throw new BizException("EVENT_AUTO_TRIGGER_SOURCE_CHANNEL_EMPTY", "来源渠道不能为空");
        }
        if (!StringUtils.hasText(dto.getTitleTemplate())) {
            throw new BizException("EVENT_AUTO_TRIGGER_TITLE_TEMPLATE_EMPTY", "任务标题模板不能为空");
        }

        Path path = resolveConfigPath();
        Map<String, Object> root = readRootConfig();
        List<Map<String, Object>> rules = readRuleMaps(root.get("rules"));
        boolean updated = false;

        for (Map<String, Object> item : rules) {
            if (Objects.equals(normalize(item.get("ruleCode")), ruleCode.trim())) {
                Map<String, Object> before = new LinkedHashMap<>(item);
                Boolean beforeEnabled = readBoolean(root.get("enabled"), false);
                applyRuleUpdate(item, dto);
                if (dto.getConfigEnabled() != null) {
                    root.put("enabled", dto.getConfigEnabled());
                }

                List<String> changedFields = diffFields(before, item);
                if (!Objects.equals(beforeEnabled, readBoolean(root.get("enabled"), false))) {
                    changedFields = new ArrayList<>(changedFields);
                    changedFields.add(0, "configEnabled");
                }

                configChangeAuditService.appendAudit(
                        "EVENT_AUTO_TRIGGER_RULE",
                        ruleCode,
                        dto.getRuleName().trim(),
                        "UPDATE",
                        path.toString(),
                        "更新事件自动触发规则",
                        changedFields
                );
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new BizException("EVENT_AUTO_TRIGGER_RULE_NOT_FOUND", "未找到事件自动触发规则: " + ruleCode);
        }

        root.put("rules", rules);
        writeRootConfig(path, root);
    }

    public EventAutoTriggerRule resolveMatchedRule(String eventType, String impactLevel) {
        EventAutoTriggerConfig config = loadConfig();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return null;
        }

        String normalizedEventType = normalize(eventType);
        String normalizedImpactLevel = normalize(impactLevel);

        return config.getRules().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> matches(rule.getEventTypes(), normalizedEventType))
                .filter(rule -> matches(rule.getImpactLevels(), normalizedImpactLevel))
                .findFirst()
                .orElse(null);
    }

    public EventAutoTriggerRule findEnabledRuleByCode(String ruleCode) {
        if (!StringUtils.hasText(ruleCode)) {
            return null;
        }
        EventAutoTriggerConfig config = loadConfig();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return null;
        }
        String normalizedRuleCode = normalize(ruleCode);
        return config.getRules().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> Objects.equals(normalize(rule.getRuleCode()), normalizedRuleCode))
                .findFirst()
                .orElse(null);
    }

    public String resolveConfigPathForDisplay() {
        return resolveConfigPath().toString();
    }

    private void applyRuleUpdate(Map<String, Object> item, EventAutoTriggerRuleUpdateDTO dto) {
        item.put("ruleName", dto.getRuleName().trim());
        item.put("enabled", dto.getEnabled() == null || Boolean.TRUE.equals(dto.getEnabled()));
        item.put("eventTypes", sanitizeList(dto.getEventTypes()));
        item.put("impactLevels", sanitizeList(dto.getImpactLevels()));
        item.put("taskType", dto.getTaskType().trim());
        item.put("analysisScope", dto.getAnalysisScope().trim());
        item.put("priority", dto.getPriority().trim());
        item.put("sourceChannel", dto.getSourceChannel().trim());
        item.put("titleTemplate", dto.getTitleTemplate().trim());
        item.put("remark", normalize(dto.getRemark()));
    }

    private EventAutoTriggerRuleItemVO toRuleItem(EventAutoTriggerRule rule) {
        EventAutoTriggerRuleItemVO vo = new EventAutoTriggerRuleItemVO();
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setEnabled(rule.getEnabled());
        vo.setEventTypes(rule.getEventTypes());
        vo.setImpactLevels(rule.getImpactLevels());
        vo.setTaskType(rule.getTaskType());
        vo.setAnalysisScope(rule.getAnalysisScope());
        vo.setPriority(rule.getPriority());
        vo.setSourceChannel(rule.getSourceChannel());
        vo.setTitleTemplate(rule.getTitleTemplate());
        vo.setRemark(rule.getRemark());
        return vo;
    }

    private List<EventAutoTriggerRule> readRules(Object value) {
        List<Map<String, Object>> rawRules = readRuleMaps(value);
        List<EventAutoTriggerRule> result = new ArrayList<>();
        for (Map<String, Object> item : rawRules) {
            EventAutoTriggerRule rule = new EventAutoTriggerRule();
            rule.setRuleCode(normalize(item.get("ruleCode")));
            rule.setRuleName(normalize(item.get("ruleName")));
            rule.setEnabled(readBoolean(item.get("enabled"), true));
            rule.setEventTypes(sanitizeList(castList(item.get("eventTypes"))));
            rule.setImpactLevels(sanitizeList(castList(item.get("impactLevels"))));
            rule.setTaskType(normalize(item.get("taskType")));
            rule.setAnalysisScope(normalize(item.get("analysisScope")));
            rule.setPriority(normalize(item.get("priority")));
            rule.setSourceChannel(normalize(item.get("sourceChannel")));
            rule.setTitleTemplate(normalize(item.get("titleTemplate")));
            rule.setRemark(normalize(item.get("remark")));
            result.add(rule);
        }
        return result;
    }

    private List<Map<String, Object>> readRuleMaps(Object value) {
        if (!(value instanceof List<?> rawRules)) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object rawRule : rawRules) {
            if (rawRule instanceof Map<?, ?> rawMap) {
                result.add(new LinkedHashMap<>(objectMapper.convertValue(
                        rawMap,
                        new TypeReference<LinkedHashMap<String, Object>>() {}
                )));
            }
        }
        return result;
    }

    private Map<String, Object> readRootConfig() {
        Path path = resolveConfigPath();
        if (!Files.exists(path)) {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("enabled", false);
            root.put("rules", new ArrayList<>());
            return root;
        }
        try {
            return objectMapper.readValue(
                    Files.readString(path, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new BizException("EVENT_AUTO_TRIGGER_READ_FAILED", "读取事件自动触发配置失败");
        }
    }

    private void writeRootConfig(Path path, Map<String, Object> root) {
        try {
            Files.createDirectories(path.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(path, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("EVENT_AUTO_TRIGGER_SAVE_FAILED", "保存事件自动触发配置失败");
        }
    }

    private boolean matches(List<String> expectedValues, String actualValue) {
        if (expectedValues == null || expectedValues.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(actualValue)) {
            return false;
        }
        return expectedValues.stream().anyMatch(item -> actualValue.equalsIgnoreCase(item));
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

    private List<String> castList(Object value) {
        if (!(value instanceof List<?> items)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            String normalized = normalize(item);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return new ArrayList<>(result);
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

    private Path resolveConfigPath() {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(configPath);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("ai-config").resolve("event-auto-trigger-configs.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("event-auto-trigger-configs.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("event-auto-trigger-configs.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("event-auto-trigger-configs.json").normalize());
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
