package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("research_task")
public class ResearchTaskDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String taskType;
    private String taskTitle;
    private Long initiatorUserId;
    private String tenantId;
    private String targetType;
    private String targetCode;
    private String targetName;
    private String priority;
    private String status;
    private String currentStage;
    private String sourceChannel;
    private String traceId;
    private String requestPayload;
    private String sourceTaskId;
    private String sourceReportId;
    private String sourceEventId;
    private String sourceDomain;
    private String sourceReviewStatus;
    private String analysisScope;
    private String resultRef;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
