package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class EventAutoTriggerRuleItemVO {

    private String ruleCode;
    private String ruleName;
    private Boolean enabled;
    private List<String> eventTypes;
    private List<String> impactLevels;
    private String taskType;
    private String analysisScope;
    private String priority;
    private String sourceChannel;
    private String titleTemplate;
    private String remark;
}
