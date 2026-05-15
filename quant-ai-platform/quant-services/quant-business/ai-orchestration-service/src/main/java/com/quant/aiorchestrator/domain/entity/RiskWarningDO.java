package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("risk_warning")
public class RiskWarningDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String warningId;
    private String taskId;
    private String warningType;
    private String warningLevel;
    private String entityType;
    private String entityCode;
    private String entityName;
    private String portfolioId;
    private String triggerSource;
    private String triggerEventId;
    private String warningSummary;
    private String warningReason;
    private String suggestAction;
    private BigDecimal confidenceScore;
    private String status;
    private String reviewStatus;
    private String reviewerId;
    private LocalDateTime reviewTime;
    private String traceId;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
