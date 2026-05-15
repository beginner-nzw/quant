package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_record")
public class AuditRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

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
    private LocalDateTime updatedAt;
    private Integer deleted;
}