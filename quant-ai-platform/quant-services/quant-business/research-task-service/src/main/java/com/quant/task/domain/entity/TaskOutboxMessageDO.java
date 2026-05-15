package com.quant.task.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_outbox_message")
public class TaskOutboxMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String outboxId;
    private String messageId;
    private String taskId;
    private String eventId;
    private String topicName;
    private String messageKey;
    private String messageType;
    private String producerService;
    private String targetService;
    private String payloadJson;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private Long messageTimestamp;
    private String traceId;
    private String tenantId;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
