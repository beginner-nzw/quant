package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class EventAutoTriggerConfigVO {

    private Boolean enabled;
    private String configPath;
    private List<EventAutoTriggerRuleItemVO> rules;
}
