package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class TaskCancelDTO {
    private String cancelReason;
    private String operatorId;
}