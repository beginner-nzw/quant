package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerConfigVO;
import lombok.Data;

import java.util.List;

public interface EventAutoTriggerConfigService {

    EventAutoTriggerConfig loadConfig();

    EventAutoTriggerConfigVO loadConfigView();

    void saveRule(String ruleCode, EventAutoTriggerRuleUpdateDTO dto);

    EventAutoTriggerRule resolveMatchedRule(String eventType, String impactLevel);

    EventAutoTriggerRule findEnabledRuleByCode(String ruleCode);

    String resolveConfigPathForDisplay();

    @Data
    class EventAutoTriggerConfig {
        private Boolean enabled = false;
        private List<EventAutoTriggerRule> rules = List.of();
    }

    @Data
    class EventAutoTriggerRule {
        private String ruleCode;
        private String ruleName;
        private Boolean enabled = true;
        private List<String> eventTypes = List.of();
        private List<String> impactLevels = List.of();
        private String taskType;
        private String analysisScope;
        private String priority;
        private String sourceChannel;
        private String titleTemplate;
        private String remark;
    }
}
