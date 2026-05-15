package com.quant.aiorchestrationservice;

import com.quant.aiorchestrator.service.impl.AiTaskInboundMessageSupportServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.service.AiTaskDeadLetterPublisherService;
import com.quant.aiorchestrator.service.AiTaskInboundMessageSupportService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.model.message.AiTaskStatusMessage;
import com.quant.common.model.message.MessageEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiTaskInboundMessageSupportServiceTests {

    @Test
    void invalidJsonShouldRecordFailureAndPublishDeadletter() {
        TaskMessageLogService taskMessageLogService = mock(TaskMessageLogService.class);
        AiTaskDeadLetterPublisherService deadLetterPublisherService = mock(AiTaskDeadLetterPublisherService.class);

        AiTaskInboundMessageSupportService service = new AiTaskInboundMessageSupportServiceImpl(
                new ObjectMapper(),
                taskMessageLogService,
                deadLetterPublisherService
        );

        String malformedMessage = "{messageId:deadletter-msg-1,traceId:deadletter-trace-1,taskId:deadletter-task-1,eventId:deadletter-event-1,messageType:AI_TASK_STATUS,sourceService:manual-test,targetService:ai-orchestration-service,tenantId:default,bizKey:deadletter-test,timestamp:1778131148942,version:1.0,retryCount:0}";
        AiTaskStatusMessage result = service.parseOrNull(
                malformedMessage,
                AiTaskStatusMessage.class,
                "ai.task.status",
                "status-group",
                "ai-orchestration-service"
        );

        assertNull(result);

        ArgumentCaptor<MessageEnvelope> envelopeCaptor = ArgumentCaptor.forClass(MessageEnvelope.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskMessageLogService).recordFailed(
                eq("ai.task.status"),
                envelopeCaptor.capture(),
                eq("ai-orchestration-service"),
                errorCaptor.capture()
        );
        assertEquals("deadletter-msg-1", envelopeCaptor.getValue().getMessageId());
        assertEquals("deadletter-task-1", envelopeCaptor.getValue().getTaskId());
        assertEquals("deadletter-event-1", envelopeCaptor.getValue().getEventId());
        assertTrue(errorCaptor.getValue().startsWith("INVALID_JSON:"));

        verify(deadLetterPublisherService).publishInvalidMessage(
                eq("ai.task.status"),
                eq("status-group"),
                eq("ai-orchestration-service"),
                any(MessageEnvelope.class),
                eq(malformedMessage),
                eq("INVALID_JSON"),
                any(String.class)
        );
    }

    @Test
    void missingPayloadShouldBeRejected() {
        AiTaskInboundMessageSupportService service = new AiTaskInboundMessageSupportServiceImpl(
                new ObjectMapper(),
                mock(TaskMessageLogService.class),
                mock(AiTaskDeadLetterPublisherService.class)
        );

        AiTaskStatusMessage message = new AiTaskStatusMessage();
        message.setMessageId("msg-2");
        message.setTaskId("task-2");

        boolean rejected = service.rejectIfInvalidEnvelope(
                message,
                false,
                "{\"messageId\":\"msg-2\",\"taskId\":\"task-2\"}",
                "ai.task.status",
                "status-group",
                "ai-orchestration-service"
        );

        assertTrue(rejected);
    }
}
