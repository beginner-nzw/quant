package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class TaskRetryDTO {
    private String retryReason;
    private String operatorId;
}