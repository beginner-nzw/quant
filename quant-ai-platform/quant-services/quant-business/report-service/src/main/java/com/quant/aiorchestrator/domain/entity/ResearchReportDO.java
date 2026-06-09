package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("research_report")
public class ResearchReportDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reportId;
    private String taskId;
    private Integer versionNo;
    private String taskType;
    private String finalStatus;
    private String summary;
    private BigDecimal confidenceScore;
    private Integer needHumanReview;
    private String reportType;
    private String highlights;
    private String riskPoints;
    private String riskWarnings;
    private String resultRef;
    private String rawPayload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
    private String reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String revisedSummary;
    private String revisedHighlights;
    private String revisedRiskPoints;
    private String reviewComment;
}
