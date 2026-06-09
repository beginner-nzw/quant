package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_prompt_audit")
public class AiPromptAuditDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String promptAuditId;
    private String taskId;
    private String executionId;
    private String agentCode;
    private String promptTemplateCode;
    private String modelName;
    private String modelVersion;
    private String inputDigest;
    private String promptDigest;
    private String responseDigest;
    private String tokenUsage;
    private Integer riskFlag;
    private String auditStatus;
    private String traceId;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
