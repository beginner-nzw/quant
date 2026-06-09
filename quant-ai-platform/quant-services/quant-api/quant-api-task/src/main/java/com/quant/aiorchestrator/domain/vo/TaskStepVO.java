package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskStepVO {
    private String taskId;
    private String stepCode;
    private String stepName;
    private String agentCode;
    private Integer executionOrder;
    private String status;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}