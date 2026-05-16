package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.Map;

@Data
public class ReportVersionVO {
    private String versionId;
    private String reportId;
    private String taskId;
    private Integer versionNo;
    private String snapshotSource;
    private Map<String, Object> snapshot;
    private String createdAt;
}
