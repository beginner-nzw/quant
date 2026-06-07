package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class TaskWorkflowControlDTO {
    private String operatorId;
    private String reason;
    private String nodeName;
}
