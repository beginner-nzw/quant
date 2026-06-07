package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkflowCheckpointVO {
    private String taskId;
    private String workflowInstanceId;
    private String currentNode;
    private String status;
    private Integer progress;
    private String currentStage;
    private Integer retryCount;
    private Long updatedAt;
    private List<Map<String, Object>> branchDecisions;
    private String failureMessage;
    private Boolean resumable;
}
