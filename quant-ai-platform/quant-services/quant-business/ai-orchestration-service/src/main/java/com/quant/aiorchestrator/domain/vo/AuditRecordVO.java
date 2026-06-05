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
    private String identitySource;
    private String roleSource;
    private String servicePrincipal;
    private String originalActorId;
    private String delegatedActorId;
    private String actionCode;
    private String actionDesc;
    private String resultStatus;
    private String remark;
    private LocalDateTime createdAt;
}
