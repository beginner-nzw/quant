package com.quant.quantcommonmodel;

import com.quant.common.model.message.AiTaskAuditMessage;
import com.quant.common.model.message.AiTaskDispatchMessage;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.AiTaskStatusMessage;
import com.quant.common.model.message.MessageEnvelope;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiTaskMessageContractTests {

    @Test
    void envelopeFieldsMatchPythonMirrorContract() {
        assertFieldNames(MessageEnvelope.class, List.of(
                "messageId",
                "traceId",
                "taskId",
                "eventId",
                "messageType",
                "sourceService",
                "targetService",
                "tenantId",
                "bizKey",
                "timestamp",
                "version",
                "retryCount"
        ));
    }

    @Test
    void dispatchPayloadFieldsMatchPythonMirrorContract() {
        assertFieldNames(AiTaskDispatchMessage.AiTaskDispatchPayload.class, List.of(
                "taskType",
                "taskTitle",
                "targetType",
                "targetCode",
                "targetName",
                "priority",
                "sourceTaskId",
                "sourceReportId",
                "sourceEventId",
                "sourceDomain",
                "sourceReviewStatus",
                "analysisScope"
        ));
    }

    @Test
    void statusPayloadFieldsMatchPythonMirrorContract() {
        assertFieldNames(AiTaskStatusMessage.StatusPayload.class, List.of(
                "workflowInstanceId",
                "status",
                "currentStage",
                "currentNode",
                "progress"
        ));
    }

    @Test
    void resultPayloadFieldsMatchPythonMirrorContract() {
        assertFieldNames(AiTaskResultMessage.ResultPayload.class, List.of(
                "workflowInstanceId",
                "taskType",
                "taskTitle",
                "analysisScope",
                "targetType",
                "targetCode",
                "targetName",
                "priority",
                "sourceTaskId",
                "sourceReportId",
                "sourceEventId",
                "sourceDomain",
                "sourceReviewStatus",
                "finalStatus",
                "finalStage",
                "summary",
                "confidenceScore",
                "needHumanReview",
                "riskWarnings",
                "reportMeta",
                "resultRef"
        ));
    }

    @Test
    void auditPayloadFieldsMatchPythonMirrorContract() {
        assertFieldNames(AiTaskAuditMessage.AuditPayload.class, List.of(
                "workflowInstanceId",
                "agents",
                "reviewSuggestion",
                "evidenceRefs"
        ));
        assertFieldNames(AiTaskAuditMessage.AgentAuditItem.class, List.of(
                "executionId",
                "agentCode",
                "agentName",
                "nodeCode",
                "status",
                "confidenceScore",
                "needHumanReview",
                "startTimestamp",
                "finishTimestamp",
                "durationMs"
        ));
    }

    private static void assertFieldNames(Class<?> type, List<String> expected) {
        List<String> actual = Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertEquals(expected, actual);
    }
}
