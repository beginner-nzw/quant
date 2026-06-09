package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.configstore.ConfigStoreAuditAppender;
import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerConfigVO;
import com.quant.common.core.exception.BizException;
import com.quant.config.port.EventAutoTriggerConfigPort.EventAutoTriggerConfig;
import com.quant.config.port.EventAutoTriggerConfigPort.EventAutoTriggerRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EventAutoTriggerConfigManager {

    private final EventAutoTriggerConfigStoreManager configStoreManager;
    private final EventAutoTriggerConfigPolicyManager configPolicyManager;
    private final ConfigStoreAuditAppender configStoreAuditAppender;

    public EventAutoTriggerConfig loadConfig(String configPath) {
        Map<String, Object> root = configStoreManager.readRootConfig(configPath);
        EventAutoTriggerConfig config = new EventAutoTriggerConfig();
        config.setEnabled(configPolicyManager.readBoolean(root.get("enabled"), false));
        config.setRules(readRules(root.get("rules")));
        return config;
    }

    public EventAutoTriggerConfigVO loadConfigView(String configPath) {
        EventAutoTriggerConfig config = loadConfig(configPath);
        EventAutoTriggerConfigVO vo = new EventAutoTriggerConfigVO();
        vo.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        vo.setConfigPath(resolveConfigPathForDisplay(configPath));
        vo.setRules(config.getRules().stream().map(configPolicyManager::toRuleItem).toList());
        return vo;
    }

    public void saveRule(String configPath, String ruleCode, EventAutoTriggerRuleUpdateDTO dto) {
        configPolicyManager.validateRuleUpdate(ruleCode, dto);

        Path path = configStoreManager.resolveConfigPath(configPath);
        Map<String, Object> root = configStoreManager.readRootConfig(configPath);
        List<Map<String, Object>> rules = configStoreManager.readRuleMaps(root.get("rules"));
        boolean updated = false;

        for (Map<String, Object> item : rules) {
            if (Objects.equals(configPolicyManager.normalize(item.get("ruleCode")), ruleCode.trim())) {
                Map<String, Object> before = configPolicyManager.applyRuleUpdate(item, dto);
                Boolean beforeEnabled = configPolicyManager.readBoolean(root.get("enabled"), false);
                if (dto.getConfigEnabled() != null) {
                    root.put("enabled", dto.getConfigEnabled());
                }

                List<String> changedFields = configPolicyManager.diffFields(before, item);
                if (!Objects.equals(beforeEnabled, configPolicyManager.readBoolean(root.get("enabled"), false))) {
                    changedFields = new ArrayList<>(changedFields);
                    changedFields.add(0, "configEnabled");
                }

                configStoreAuditAppender.appendAudit(
                        "EVENT_AUTO_TRIGGER_RULE",
                        ruleCode,
                        dto.getRuleName().trim(),
                        "UPDATE",
                        path.toString(),
                        "update event auto trigger rule",
                        changedFields
                );
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new BizException("EVENT_AUTO_TRIGGER_RULE_NOT_FOUND", "event auto trigger rule not found: " + ruleCode);
        }

        root.put("rules", rules);
        configStoreManager.writeRootConfig(path, root);
    }

    public EventAutoTriggerRule resolveMatchedRule(String configPath, String eventType, String impactLevel) {
        EventAutoTriggerConfig config = loadConfig(configPath);
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return null;
        }

        String normalizedEventType = configPolicyManager.normalize(eventType);
        String normalizedImpactLevel = configPolicyManager.normalize(impactLevel);

        return config.getRules().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> configPolicyManager.matches(rule.getEventTypes(), normalizedEventType))
                .filter(rule -> configPolicyManager.matches(rule.getImpactLevels(), normalizedImpactLevel))
                .findFirst()
                .orElse(null);
    }

    public EventAutoTriggerRule findEnabledRuleByCode(String configPath, String ruleCode) {
        if (!StringUtils.hasText(ruleCode)) {
            return null;
        }
        EventAutoTriggerConfig config = loadConfig(configPath);
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return null;
        }
        String normalizedRuleCode = configPolicyManager.normalize(ruleCode);
        return config.getRules().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> Objects.equals(configPolicyManager.normalize(rule.getRuleCode()), normalizedRuleCode))
                .findFirst()
                .orElse(null);
    }

    public String resolveConfigPathForDisplay(String configPath) {
        return configStoreManager.resolveConfigPath(configPath).toString();
    }

    private List<EventAutoTriggerRule> readRules(Object value) {
        List<Map<String, Object>> rawRules = configStoreManager.readRuleMaps(value);
        return rawRules.stream()
                .map(configPolicyManager::toRule)
                .toList();
    }
}
