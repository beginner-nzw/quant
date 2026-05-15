package com.quant.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.message.AiTaskDispatchMessage;
import com.quant.task.domain.entity.ResearchTaskDO;
import com.quant.task.domain.entity.TaskOutboxMessageDO;
import com.quant.task.mapper.TaskOutboxMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskOutboxMessageService {

    private static final String PRODUCER_SERVICE = "research-task-service";
    private static final String TARGET_SERVICE = "python-ai-engine";
    private static final int DEFAULT_MAX_RETRY_COUNT = 10;

    private final TaskOutboxMessageMapper taskOutboxMessageMapper;
    private final ObjectMapper objectMapper;

    public TaskOutboxMessageDO enqueueAiTaskDispatch(ResearchTaskDO task) {
        AiTaskDispatchMessage message = buildDispatchMessage(task);
        String payloadJson = serialize(message);

        TaskOutboxMessageDO outbox = new TaskOutboxMessageDO();
        outbox.setOutboxId(UUID.randomUUID().toString());
        outbox.setMessageId(message.getMessageId());
        outbox.setTaskId(message.getTaskId());
        outbox.setEventId(message.getEventId());
        outbox.setTopicName(KafkaTopicConstants.AI_TASK_DISPATCH);
        outbox.setMessageKey(task.getTaskId());
        outbox.setMessageType(MessageTypeConstants.AI_TASK_DISPATCH);
        outbox.setProducerService(PRODUCER_SERVICE);
        outbox.setTargetService(TARGET_SERVICE);
        outbox.setPayloadJson(payloadJson);
        outbox.setStatus(TaskOutboxStatusConstants.PENDING);
        outbox.setRetryCount(0);
        outbox.setMaxRetryCount(DEFAULT_MAX_RETRY_COUNT);
        outbox.setMessageTimestamp(message.getTimestamp());
        outbox.setTraceId(message.getTraceId());
        outbox.setTenantId(message.getTenantId());
        outbox.setDeleted(0);
        taskOutboxMessageMapper.insert(outbox);
        return outbox;
    }

    public int markSending(TaskOutboxMessageDO outbox) {
        return taskOutboxMessageMapper.markSending(outbox.getOutboxId());
    }

    public void markSent(TaskOutboxMessageDO outbox) {
        taskOutboxMessageMapper.markSent(outbox.getOutboxId());
    }

    public void markFailed(TaskOutboxMessageDO outbox, Throwable throwable) {
        int retryCount = outbox.getRetryCount() == null ? 0 : outbox.getRetryCount();
        LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(nextRetryDelaySeconds(retryCount));
        taskOutboxMessageMapper.markFailed(outbox.getOutboxId(), nextRetryAt, safeTruncate(errorMessage(throwable)));
    }

    public java.util.List<TaskOutboxMessageDO> selectReadyToPublish(int limit, LocalDateTime staleBefore) {
        return taskOutboxMessageMapper.selectReadyToPublish(limit, staleBefore);
    }

    private AiTaskDispatchMessage buildDispatchMessage(ResearchTaskDO task) {
        AiTaskDispatchMessage.AiTaskDispatchPayload payload = new AiTaskDispatchMessage.AiTaskDispatchPayload();
        payload.setTaskType(task.getTaskType());
        payload.setTaskTitle(task.getTaskTitle());
        payload.setTargetType(task.getTargetType());
        payload.setTargetCode(task.getTargetCode());
        payload.setTargetName(task.getTargetName());
        payload.setPriority(task.getPriority());
        payload.setSourceTaskId(task.getSourceTaskId());
        payload.setSourceReportId(task.getSourceReportId());
        payload.setSourceEventId(task.getSourceEventId());
        payload.setSourceDomain(task.getSourceDomain());
        payload.setSourceReviewStatus(task.getSourceReviewStatus());
        payload.setAnalysisScope(task.getAnalysisScope());

        AiTaskDispatchMessage message = new AiTaskDispatchMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTraceId(task.getTraceId());
        message.setTaskId(task.getTaskId());
        message.setEventId(task.getSourceEventId());
        message.setMessageType(MessageTypeConstants.AI_TASK_DISPATCH);
        message.setSourceService(PRODUCER_SERVICE);
        message.setTargetService(TARGET_SERVICE);
        message.setTenantId(task.getTenantId());
        message.setBizKey(task.getTargetType() + ":" + task.getTargetCode());
        message.setTimestamp(System.currentTimeMillis());
        message.setVersion("1.0");
        message.setRetryCount(0);
        message.setPayload(payload);
        return message;
    }

    private String serialize(AiTaskDispatchMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("serialize ai task dispatch outbox message failed", e);
        }
    }

    private long nextRetryDelaySeconds(int retryCount) {
        int boundedRetryCount = Math.min(retryCount + 1, 6);
        return Math.min(300L, 5L * (1L << (boundedRetryCount - 1)));
    }

    private String errorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private String safeTruncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
