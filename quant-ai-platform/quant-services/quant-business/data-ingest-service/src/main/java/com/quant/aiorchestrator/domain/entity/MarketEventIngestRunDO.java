package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("market_event_ingest_run")
public class MarketEventIngestRunDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ingestRunId;
    private String sourceCode;
    private String sourceName;
    private String sourceCategory;
    private String sourceChannel;
    private String ingestMode;
    private String requestTarget;
    private String fetchStatus;
    private String rawPayloadRef;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Integer deadlettered;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer duplicateCount;
    private Integer autoTriggeredCount;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
