package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("research_task_step")
public class ResearchTaskStepDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String stepCode;
    private String stepName;
    private String agentCode;
    private Integer executionOrder;
    private String status;
    private String inputSnapshot;
    private String outputSnapshot;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private Long durationMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}