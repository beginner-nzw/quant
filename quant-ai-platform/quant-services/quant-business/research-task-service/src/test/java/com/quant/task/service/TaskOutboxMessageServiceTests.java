package com.quant.task.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.task.domain.entity.ResearchTaskDO;
import com.quant.task.domain.entity.TaskOutboxMessageDO;
import com.quant.task.mapper.TaskOutboxMessageMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskOutboxMessageServiceTests {

    @Test
    void enqueueAiTaskDispatchShouldPersistPendingOutboxMessage() throws Exception {
        TaskOutboxMessageMapper mapper = mock(TaskOutboxMessageMapper.class);
        when(mapper.insert(any(TaskOutboxMessageDO.class))).thenReturn(1);
        ObjectMapper objectMapper = new ObjectMapper();
        TaskOutboxMessageService service = new TaskOutboxMessageService(mapper, objectMapper);

        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId("task-1");
        task.setTraceId("trace-1");
        task.setTenantId("default");
        task.setTaskType("STOCK_RESEARCH");
        task.setTaskTitle("Research task");
        task.setTargetType("STOCK");
        task.setTargetCode("600000");
        task.setTargetName("SPDB");
        task.setPriority("HIGH");
        task.setSourceEventId("event-1");
        task.setAnalysisScope("DEEP_RESEARCH");

        TaskOutboxMessageDO outbox = service.enqueueAiTaskDispatch(task);

        ArgumentCaptor<TaskOutboxMessageDO> captor = ArgumentCaptor.forClass(TaskOutboxMessageDO.class);
        verify(mapper).insert(captor.capture());
        TaskOutboxMessageDO inserted = captor.getValue();
        assertEquals(outbox.getOutboxId(), inserted.getOutboxId());
        assertEquals(KafkaTopicConstants.AI_TASK_DISPATCH, inserted.getTopicName());
        assertEquals(MessageTypeConstants.AI_TASK_DISPATCH, inserted.getMessageType());
        assertEquals("task-1", inserted.getMessageKey());
        assertEquals("task-1", inserted.getTaskId());
        assertEquals("event-1", inserted.getEventId());
        assertEquals("research-task-service", inserted.getProducerService());
        assertEquals("python-ai-engine", inserted.getTargetService());
        assertEquals(TaskOutboxStatusConstants.PENDING, inserted.getStatus());
        assertEquals(0, inserted.getRetryCount());
        assertEquals(10, inserted.getMaxRetryCount());
        assertNotNull(inserted.getMessageId());
        assertNotNull(inserted.getMessageTimestamp());

        JsonNode payload = objectMapper.readTree(inserted.getPayloadJson());
        assertEquals(inserted.getMessageId(), payload.get("messageId").asText());
        assertEquals("trace-1", payload.get("traceId").asText());
        assertEquals("task-1", payload.get("taskId").asText());
        assertEquals("600000", payload.get("payload").get("targetCode").asText());
        assertEquals("DEEP_RESEARCH", payload.get("payload").get("analysisScope").asText());
    }
}
