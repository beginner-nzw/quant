package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditRecordVO {
    private String auditId;
    private String taskId;
    private String auditType;
    private String auditStage;
    private String operatorType;
    private String operatorId;
    private String actionCode;
    private String actionDesc;
    private String resultStatus;
    private String remark;
    private LocalDateTime createdAt;
}