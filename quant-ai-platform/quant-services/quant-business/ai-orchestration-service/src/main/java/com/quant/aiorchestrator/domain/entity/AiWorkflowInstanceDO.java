package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_workflow_instance")
public class AiWorkflowInstanceDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String workflowInstanceId;
    private String taskId;
    private String workflowCode;
    private String workflowVersion;
    private String entryAgent;
    private String currentNode;
    private String status;
    private String graphSnapshot;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}