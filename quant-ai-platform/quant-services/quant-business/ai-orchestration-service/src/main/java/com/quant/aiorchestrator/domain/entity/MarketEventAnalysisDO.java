package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("market_event_analysis")
public class MarketEventAnalysisDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String analysisId;
    private String eventId;
    private String taskId;
    private String analysisVersion;
    private String impactDirection;
    private String impactLevel;
    private String shortTermView;
    private String midTermView;
    private Integer riskFlag;
    private BigDecimal confidenceScore;
    private String analysisSummary;
    private String status;
    private String traceId;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
