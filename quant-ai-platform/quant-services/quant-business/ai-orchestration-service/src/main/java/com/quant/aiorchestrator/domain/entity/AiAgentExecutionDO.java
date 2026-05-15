package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_agent_execution")
public class AiAgentExecutionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String executionId;
    private String workflowInstanceId;
    private String taskId;
    private String agentCode;
    private String agentName;
    private String nodeCode;
    private String inputRef;
    private String outputRef;
    private String decisionRef;
    private String status;
    private BigDecimal confidenceScore;
    private Integer needHumanReview;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private Long durationMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}