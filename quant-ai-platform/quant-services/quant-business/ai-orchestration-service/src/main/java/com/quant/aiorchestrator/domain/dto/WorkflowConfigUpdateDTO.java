package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowConfigUpdateDTO {

    private String workflowVersion;
    private String workflowType;
    private Boolean enabled;
    private Boolean defaultSelected;
    private List<String> taskTypes;
    private List<String> nodeSequence;
    private String remark;
}
