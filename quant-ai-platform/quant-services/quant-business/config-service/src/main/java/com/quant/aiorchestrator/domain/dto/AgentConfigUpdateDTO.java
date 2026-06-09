package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentConfigUpdateDTO {

    private String agentName;
    private String stageCode;
    private Integer executionOrder;
    private Boolean enabled;
    private Integer timeoutSeconds;
    private Boolean needHumanReview;
    private String implementationMode;
    private String version;
    private List<String> toolWhitelist;
    private List<String> inputKeys;
    private List<String> outputKeys;
    private String remark;
}
