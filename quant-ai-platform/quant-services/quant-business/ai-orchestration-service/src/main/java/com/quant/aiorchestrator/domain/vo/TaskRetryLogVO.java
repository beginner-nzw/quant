package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskRetryLogVO {
    private String taskId;
    private Integer retryNo;
    private String retryReason;
    private String retrySource;
    private String retryStatus;
    private String operatorId;
    private LocalDateTime createdAt;
}