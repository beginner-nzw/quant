package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskDetailVO {
    private String taskId;
    private String taskType;
    private String taskTitle;
    private String targetType;
    private String targetCode;
    private String targetName;
    private String priority;
    private String status;
    private String currentStage;
    private String sourceChannel;
    private String traceId;
    private String sourceTaskId;
    private String sourceReportId;
    private String sourceEventId;
    private String sourceDomain;
    private String sourceReviewStatus;
    private String analysisScope;
    private String resultRef;
    private Integer retryCount;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String errorMessage;
}
