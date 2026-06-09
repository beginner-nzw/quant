package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("research_report_review_log")
public class ResearchReportReviewLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reviewLogId;
    private String reportId;
    private String taskId;
    private Integer versionNo;
    private String reviewStatus;
    private String reviewedBy;
    private String reviewComment;
    private String revisedSummary;
    private String revisedHighlights;
    private String revisedRiskPoints;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
