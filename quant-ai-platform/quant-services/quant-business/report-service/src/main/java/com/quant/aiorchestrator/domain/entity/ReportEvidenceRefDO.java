package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("report_evidence_ref")
public class ReportEvidenceRefDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String evidenceId;
    private String reportId;
    private String taskId;
    private String sectionCode;
    private String conclusionCode;
    private String sourceType;
    private String sourceRefId;
    private String evidenceSummary;
    private String evidenceUrl;
    private BigDecimal confidenceScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
