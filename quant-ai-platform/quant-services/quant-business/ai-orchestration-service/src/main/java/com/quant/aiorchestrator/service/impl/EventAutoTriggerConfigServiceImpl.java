package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerConfigVO;
import com.quant.aiorchestrator.manager.EventAutoTriggerConfigManager;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventAutoTriggerConfigServiceImpl implements EventAutoTriggerConfigService {

    @Value("${quant.ai.event-auto-trigger-config:../../../ai-config/event-auto-trigger-configs.json}")
    private String configPath;

    private final EventAutoTriggerConfigManager configManager;

    @Override
    public EventAutoTriggerConfig loadConfig() {
        return configManager.loadConfig(configPath);
    }

    @Override
    public EventAutoTriggerConfigVO loadConfigView() {
        return configManager.loadConfigView(configPath);
    }

    @Override
    public void saveRule(String ruleCode, EventAutoTriggerRuleUpdateDTO dto) {
        configManager.saveRule(configPath, ruleCode, dto);
    }

    @Override
    public EventAutoTriggerRule resolveMatchedRule(String eventType, String impactLevel) {
        return configManager.resolveMatchedRule(configPath, eventType, impactLevel);
    }

    @Override
    public EventAutoTriggerRule findEnabledRuleByCode(String ruleCode) {
        return configManager.findEnabledRuleByCode(configPath, ruleCode);
    }

    @Override
    public String resolveConfigPathForDisplay() {
        return configManager.resolveConfigPathForDisplay(configPath);
    }
}
