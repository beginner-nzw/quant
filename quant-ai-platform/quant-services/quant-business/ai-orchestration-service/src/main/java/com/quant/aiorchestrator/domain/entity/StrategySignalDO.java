package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("strategy_signal")
public class StrategySignalDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String signalId;
    private String taskId;
    private String signalType;
    private String entityCode;
    private String entityName;
    private LocalDate signalDate;
    private Integer signalScore;
    private String signalLevel;
    private String signalDirection;
    private String reasonSummary;
    private BigDecimal confidenceScore;
    private String sourceEventId;
    private String status;
    private String traceId;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
