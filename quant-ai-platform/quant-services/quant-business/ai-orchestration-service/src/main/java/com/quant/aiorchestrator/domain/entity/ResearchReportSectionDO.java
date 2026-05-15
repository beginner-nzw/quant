package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("research_report_section")
public class ResearchReportSectionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sectionId;
    private String reportId;
    private String taskId;
    private Integer versionNo;
    private String sectionCode;
    private String sectionTitle;
    private Integer sectionOrder;
    private String sectionContent;
    private String sectionItems;
    private String revisedContent;
    private String revisedItems;
    private String reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private BigDecimal confidenceScore;
    private String traceId;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
