package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentExecutionVO {
    private String executionId;
    private String workflowInstanceId;
    private String taskId;
    private String agentCode;
    private String agentName;
    private String nodeCode;
    private String status;
    private BigDecimal confidenceScore;
    private Integer needHumanReview;
    private Long durationMs;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}