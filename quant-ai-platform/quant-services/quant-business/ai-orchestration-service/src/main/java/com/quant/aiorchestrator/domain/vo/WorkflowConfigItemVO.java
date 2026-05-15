package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowConfigItemVO {

    private String workflowCode;
    private String workflowVersion;
    private String workflowType;
    private List<String> taskTypes;
    private String entryAgent;
    private Integer nodeCount;
    private Boolean enabled;
    private Boolean defaultSelected;
    private List<String> nodeSequence;
    private String nodeTimeoutSummary;
    private String remark;
}
