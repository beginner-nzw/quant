package com.quant.task.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.model.message.AiTaskDispatchMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiTaskDispatchCompatibilityTests {

    @Test
    void legacyDispatchMessageWithoutActorProvenanceStillDeserializes() throws Exception {
        String json = """
                {
                  "messageId": "message-1",
                  "traceId": "trace-1",
                  "taskId": "task-1",
                  "messageType": "AI_TASK_DISPATCH",
                  "sourceService": "research-task-service",
                  "timestamp": 1,
                  "version": "1.0",
                  "retryCount": 0,
                  "payload": {
                    "taskType": "EQUITY_RESEARCH",
                    "taskTitle": "legacy",
                    "targetType": "STOCK",
                    "targetCode": "000001",
                    "targetName": "Ping An Bank",
                    "priority": "HIGH"
                  }
                }
                """;

        AiTaskDispatchMessage message = new ObjectMapper().readValue(json, AiTaskDispatchMessage.class);

        assertEquals("task-1", message.getTaskId());
        assertNull(message.getPayload().getActorProvenance());
    }
}
