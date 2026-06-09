package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("market_event_auto_trigger_attempt")
public class MarketEventAutoTriggerAttemptDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String attemptId;
    private String eventId;
    private String ruleCode;
    private String status;
    private String taskId;
    private String reason;
    private String source;
    private String failureCode;
    private Integer retryCount;
    private String message;
    private String traceId;
    private String tenantId;
    private LocalDateTime attemptedAt;
    private LocalDateTime createdAt;
    private Integer deleted;
}
