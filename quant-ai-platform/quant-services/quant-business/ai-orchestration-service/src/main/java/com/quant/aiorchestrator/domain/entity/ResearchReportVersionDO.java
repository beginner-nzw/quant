package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("research_report_version")
public class ResearchReportVersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String versionId;
    private String reportId;
    private String taskId;
    private Integer versionNo;
    private String snapshotSource;
    private String snapshotPayload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
