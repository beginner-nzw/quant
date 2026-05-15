package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ConfigChangeAuditItemVO {

    private String auditId;
    private String configType;
    private String targetCode;
    private String targetName;
    private String operation;
    private String operatorId;
    private String operatorRole;
    private String configPath;
    private String changeSummary;
    private List<String> changedFields;
    private String createdAt;
}
