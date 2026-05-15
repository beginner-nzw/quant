package com.quant.common.model.message;

import lombok.Data;

@Data
public abstract class MessageEnvelope {

    private String messageId;
    private String traceId;
    private String taskId;
    private String eventId;
    private String messageType;
    private String sourceService;
    private String targetService;
    private String tenantId;
    private String bizKey;
    private Long timestamp;
    private String version;
    private Integer retryCount;
}
