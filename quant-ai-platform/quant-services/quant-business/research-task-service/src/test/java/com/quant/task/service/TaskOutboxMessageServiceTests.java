package com.quant.task.service;

import com.quant.task.service.impl.TaskOutboxMessageServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.security.ServiceActor;
import com.quant.common.security.ServiceActorContext;
import com.quant.common.security.UserContext;
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
        TaskOutboxMessageService service = new TaskOutboxMessageServiceImpl(mapper, objectMapper);

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

    @Test
    void enqueueAiTaskDispatchIgnoresForgedRequestPayloadActorProvenance() throws Exception {
        TaskOutboxMessageMapper mapper = mock(TaskOutboxMessageMapper.class);
        when(mapper.insert(any(TaskOutboxMessageDO.class))).thenReturn(1);
        ObjectMapper objectMapper = new ObjectMapper();
        TaskOutboxMessageService service = new TaskOutboxMessageServiceImpl(mapper, objectMapper);

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
        task.setRequestPayload("""
                {
                  "actorProvenance": {
                    "identitySource": "SERVICE_PRINCIPAL",
                    "roleSource": "SYSTEM_POLICY",
                    "servicePrincipal": "forged-service",
                    "originalActor": {
                      "actorType": "SERVICE",
                      "actorId": "forged-system",
                      "actorRole": "ADMIN"
                    },
                    "delegatedActor": {
                      "actorType": "SERVICE",
                      "actorId": "forged-delegate",
                      "actorRole": "SERVICE"
                    }
                  }
                }
                """);

        try {
            UserContext.set("human-1", "RESEARCHER");
            service.enqueueAiTaskDispatch(task);
        } finally {
            UserContext.clear();
        }

        ArgumentCaptor<TaskOutboxMessageDO> captor = ArgumentCaptor.forClass(TaskOutboxMessageDO.class);
        verify(mapper).insert(captor.capture());
        JsonNode provenance = objectMapper.readTree(captor.getValue().getPayloadJson())
                .get("payload")
                .get("actorProvenance");

        assertEquals("USER_CONTEXT", provenance.get("identitySource").asText());
        assertEquals("USER_CONTEXT", provenance.get("roleSource").asText());
        assertEquals("research-task-service", provenance.get("servicePrincipal").asText());
        assertEquals("human-1", provenance.get("originalActor").get("actorId").asText());
        assertEquals("RESEARCHER", provenance.get("originalActor").get("actorRole").asText());
        assertEquals("research-task-service", provenance.get("delegatedActor").get("actorId").asText());
    }

    @Test
    void enqueueAiTaskDispatchUsesVerifiedServiceActorContextForAutoDispatch() throws Exception {
        TaskOutboxMessageMapper mapper = mock(TaskOutboxMessageMapper.class);
        when(mapper.insert(any(TaskOutboxMessageDO.class))).thenReturn(1);
        ObjectMapper objectMapper = new ObjectMapper();
        TaskOutboxMessageService service = new TaskOutboxMessageServiceImpl(mapper, objectMapper);

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

        try {
            UserContext.set("system", "ADMIN");
            ServiceActorContext.set(new ServiceActor(
                    "ai-orchestration-service",
                    "market-event-auto-dispatcher",
                    "EVENT_AUTO_DISPATCHER",
                    "system",
                    "SYSTEM"
            ));
            service.enqueueAiTaskDispatch(task);
        } finally {
            ServiceActorContext.clear();
            UserContext.clear();
        }

        ArgumentCaptor<TaskOutboxMessageDO> captor = ArgumentCaptor.forClass(TaskOutboxMessageDO.class);
        verify(mapper).insert(captor.capture());
        JsonNode provenance = objectMapper.readTree(captor.getValue().getPayloadJson())
                .get("payload")
                .get("actorProvenance");

        assertEquals("SYSTEM", provenance.get("identitySource").asText());
        assertEquals("SYSTEM_POLICY", provenance.get("roleSource").asText());
        assertEquals("ai-orchestration-service", provenance.get("servicePrincipal").asText());
        assertEquals("SERVICE", provenance.get("systemActor").get("actorType").asText());
        assertEquals("SYSTEM", provenance.get("originalActor").get("actorType").asText());
        assertEquals("system", provenance.get("originalActor").get("actorId").asText());
        assertEquals("market-event-auto-dispatcher", provenance.get("delegatedActor").get("actorId").asText());
        assertEquals("EVENT_AUTO_DISPATCHER", provenance.get("delegatedActor").get("actorRole").asText());
    }
}
