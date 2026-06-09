package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("research_task_retry_log")
public class ResearchTaskRetryLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private Integer retryNo;
    private String retryReason;
    private String retrySource;
    private String retryStatus;
    private String operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}