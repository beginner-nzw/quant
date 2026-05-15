package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkflowInstanceVO {
    private String workflowInstanceId;
    private String taskId;
    private String workflowCode;
    private String workflowVersion;
    private String entryAgent;
    private String currentNode;
    private String status;
    private String graphSnapshot;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}