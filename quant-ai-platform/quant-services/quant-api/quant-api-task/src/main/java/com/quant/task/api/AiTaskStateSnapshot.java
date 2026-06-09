package com.quant.task.api;

import lombok.Data;

@Data
public class AiTaskStateSnapshot {

    private String taskId;
    private String taskType;
    private String status;
    private Integer retryCount;
}
