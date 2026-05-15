package com.quant.aiorchestrationservice;

import com.quant.aiorchestrator.service.impl.TaskMessageLogServiceImpl;

import com.quant.aiorchestrator.domain.entity.TaskMessageLogDO;
import com.quant.aiorchestrator.mapper.TaskMessageLogMapper;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageConsumeStatusConstants;
import com.quant.common.model.message.SimpleMessageEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskMessageLogServiceTests {

    @Test
    void beginConsumeShouldInsertProcessingForFirstDelivery() {
        TaskMessageLogMapper mapper = mock(TaskMessageLogMapper.class);
        TaskMessageLogService service = new TaskMessageLogServiceImpl(mapper);
        SimpleMessageEnvelope message = buildMessage();

        when(mapper.selectConsumerLog(any(), any(), any())).thenReturn(null);
        when(mapper.insert(any(TaskMessageLogDO.class))).thenReturn(1);

        assertTrue(service.beginConsume(KafkaTopicConstants.AI_TASK_STATUS, message, "ai-orchestration-service"));

        ArgumentCaptor<TaskMessageLogDO> captor = ArgumentCaptor.forClass(TaskMessageLogDO.class);
        verify(mapper).insert(captor.capture());
        assertTrue(captor.getValue().getMessageLogId() != null && !captor.getValue().getMessageLogId().isBlank());
        assertTrue(MessageConsumeStatusConstants.PROCESSING.equals(captor.getValue().getConsumeStatus()));
    }

    @Test
    void beginConsumeShouldSkipSuccessfulDelivery() {
        TaskMessageLogMapper mapper = mock(TaskMessageLogMapper.class);
        TaskMessageLogService service = new TaskMessageLogServiceImpl(mapper);
        TaskMessageLogDO existing = new TaskMessageLogDO();
        existing.setId(1L);
        existing.setConsumeStatus(MessageConsumeStatusConstants.SUCCESS);

        when(mapper.selectConsumerLog(any(), any(), any())).thenReturn(existing);

        assertFalse(service.beginConsume(KafkaTopicConstants.AI_TASK_STATUS, buildMessage(), "ai-orchestration-service"));
        verify(mapper, never()).insert(any(TaskMessageLogDO.class));
        verify(mapper, never()).resetFailedToProcessing(any(), any(), any(), any(), any());
    }

    @Test
    void beginConsumeShouldResetFailedDeliveryToProcessing() {
        TaskMessageLogMapper mapper = mock(TaskMessageLogMapper.class);
        TaskMessageLogService service = new TaskMessageLogServiceImpl(mapper);
        TaskMessageLogDO existing = new TaskMessageLogDO();
        existing.setId(7L);
        existing.setRetryCount(0);
        existing.setConsumeStatus(MessageConsumeStatusConstants.FAILED);

        when(mapper.selectConsumerLog(any(), any(), any())).thenReturn(existing);
        when(mapper.resetFailedToProcessing(eq(7L), eq(1), any(), eq("trace-1"), eq("default"))).thenReturn(1);

        SimpleMessageEnvelope message = buildMessage();
        message.setRetryCount(1);

        assertTrue(service.beginConsume(KafkaTopicConstants.AI_TASK_STATUS, message, "ai-orchestration-service"));
        verify(mapper).resetFailedToProcessing(eq(7L), eq(1), any(), eq("trace-1"), eq("default"));
    }

    @Test
    void recordConsumedShouldCompleteProcessingAsSuccess() {
        TaskMessageLogMapper mapper = mock(TaskMessageLogMapper.class);
        TaskMessageLogService service = new TaskMessageLogServiceImpl(mapper);
        SimpleMessageEnvelope message = buildMessage();

        when(mapper.completeConsumerLog(
                KafkaTopicConstants.AI_TASK_STATUS,
                "msg-1",
                "ai-orchestration-service",
                MessageConsumeStatusConstants.SUCCESS,
                null
        )).thenReturn(1);

        service.recordConsumed(KafkaTopicConstants.AI_TASK_STATUS, message, "ai-orchestration-service");

        verify(mapper).completeConsumerLog(
                KafkaTopicConstants.AI_TASK_STATUS,
                "msg-1",
                "ai-orchestration-service",
                MessageConsumeStatusConstants.SUCCESS,
                null
        );
        verify(mapper, never()).insert(any(TaskMessageLogDO.class));
    }

    private SimpleMessageEnvelope buildMessage() {
        SimpleMessageEnvelope message = new SimpleMessageEnvelope();
        message.setMessageId("msg-1");
        message.setTaskId("task-1");
        message.setEventId("event-1");
        message.setMessageType("AI_TASK_STATUS");
        message.setSourceService("python-ai-engine");
        message.setTargetService("ai-orchestration-service");
        message.setRetryCount(0);
        message.setTimestamp(1778131148942L);
        message.setTraceId("trace-1");
        message.setTenantId("default");
        return message;
    }
}
