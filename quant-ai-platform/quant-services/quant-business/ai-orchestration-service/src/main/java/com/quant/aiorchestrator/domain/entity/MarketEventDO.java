package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("market_event")
public class MarketEventDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private String targetType;
    private String targetCode;
    private String targetName;
    private String eventType;
    private String eventTitle;
    private String eventSummary;
    private String sourceChannel;
    private String sourceUrl;
    private String impactLevel;
    private String eventStatus;
    private String autoTriggerRuleCode;
    private String autoTriggerStatus;
    private String autoTriggerTaskId;
    private String autoTriggerMessage;
    private LocalDateTime autoTriggerAttemptedAt;
    private LocalDateTime occurredAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
