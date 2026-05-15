package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.service.AiTaskDeadLetterPublisherService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.message.AiTaskDeadLetterMessage;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.message.SimpleMessageEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTaskDeadLetterPublisherServiceTests {

    @Test
    void publishesDeadletterAndLogsProduced() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        TaskMessageLogService taskMessageLogService = mock(TaskMessageLogService.class);

        AiTaskDeadLetterPublisherService service = new AiTaskDeadLetterPublisherService(
                objectMapper,
                kafkaTemplate,
                taskMessageLogService
        );

        SimpleMessageEnvelope original = new SimpleMessageEnvelope();
        original.setMessageId("msg-1");
        original.setTaskId("task-1");
        original.setEventId("event-1");
        original.setMessageType("AI_TASK_STATUS");
        original.setSourceService("python-ai-engine");
        original.setTenantId("default");
        original.setRetryCount(0);

        when(objectMapper.writeValueAsString(any(AiTaskDeadLetterMessage.class))).thenReturn("{}");
        doReturn(CompletableFuture.completedFuture(null)).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        service.publishInvalidMessage(
                "ai.task.status",
                "status-group",
                "ai-orchestration-service",
                original,
                "{\"messageId\":\"msg-1\"}",
                "INVALID_JSON",
                "bad json"
        );

        verify(kafkaTemplate, times(1)).send(eq(KafkaTopicConstants.AI_TASK_DEADLETTER), anyString(), anyString());

        ArgumentCaptor<MessageEnvelope> envelopeCaptor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(taskMessageLogService).recordProduced(eq(KafkaTopicConstants.AI_TASK_DEADLETTER), envelopeCaptor.capture());
        assertEquals("task-1", envelopeCaptor.getValue().getTaskId());
        assertEquals("event-1", envelopeCaptor.getValue().getEventId());
        assertEquals("AI_TASK_DEADLETTER", envelopeCaptor.getValue().getMessageType());
    }
}
