package com.quant.task.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_message_log")
public class TaskMessageLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageLogId;
    private String messageId;
    private String taskId;
    private String eventId;
    private String topicName;
    private String messageType;
    private String producerService;
    private String consumerService;
    private String consumeStatus;
    private Integer retryCount;
    private String errorMessage;
    private String rawMessageRef;
    private Long messageTimestamp;
    private String traceId;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
