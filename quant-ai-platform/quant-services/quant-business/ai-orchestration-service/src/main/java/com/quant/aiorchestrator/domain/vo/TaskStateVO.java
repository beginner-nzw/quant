package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class TaskStateVO {
    private String taskId;
    private String status;
    private String currentStage;
    private Integer progress;
    private String source;
}