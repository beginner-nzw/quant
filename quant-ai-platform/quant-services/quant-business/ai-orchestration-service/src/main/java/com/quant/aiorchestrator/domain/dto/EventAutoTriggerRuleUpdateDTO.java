package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class EventAutoTriggerRuleUpdateDTO {

    private Boolean configEnabled;
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
