package com.quant.config.port;

import lombok.Data;

import java.util.List;

public interface EventAutoTriggerConfigPort {

    EventAutoTriggerConfig loadConfig();

    EventAutoTriggerRule resolveMatchedRule(String eventType, String impactLevel);

    EventAutoTriggerRule findEnabledRuleByCode(String ruleCode);

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
