package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.consumer.AiTaskResultConsumer;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.manager.TaskTraceManager;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.service.AiResultDomainProjectionService;
import com.quant.aiorchestrator.service.AiTaskInboundMessageSupportService;
import com.quant.aiorchestrator.service.TaskDomainEventPublisherService;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiTaskResultConsumerTests {

    @Test
    void staleFinalUpdateShouldRecordSkippedAndAvoidProjectionSideEffects() throws Exception {
        TestDeps deps = new TestDeps();
        AiTaskResultConsumer consumer = newConsumer(deps);
        AiTaskResultMessage message = buildSuccessMessage("msg-stale-final", 1);
        ResearchTaskDO task = buildTask(TaskStatusEnum.RUNNING.name(), 1);

        when(deps.inboundMessageSupportService.parseOrNull(
                eq("raw-message"),
                eq(AiTaskResultMessage.class),
                eq(KafkaTopicConstants.AI_TASK_RESULT),
                any(),
                any()
        )).thenReturn(message);
        when(deps.inboundMessageSupportService.rejectIfInvalidEnvelope(
                eq(message),
                eq(true),
                eq("raw-message"),
                eq(KafkaTopicConstants.AI_TASK_RESULT),
                any(),
                any()
        )).thenReturn(false);
        when(deps.taskMessageLogService.beginConsume(KafkaTopicConstants.AI_TASK_RESULT, message, "ai-orchestration-service"))
                .thenReturn(true);
        when(deps.researchTaskMapper.selectOne(any())).thenReturn(task);
        when(deps.taskStateManager.canTransfer(TaskStatusEnum.RUNNING.name(), TaskStatusEnum.SUCCESS.name()))
                .thenReturn(true);
        when(deps.researchTaskMapper.update(any(ResearchTaskDO.class), any())).thenReturn(0);

        consumer.onMessage("raw-message");

        verify(deps.researchTaskMapper).update(any(ResearchTaskDO.class), any());
        verify(deps.taskMessageLogService).recordSkipped(
                KafkaTopicConstants.AI_TASK_RESULT,
                message,
                "ai-orchestration-service",
                "TASK_FINAL_STATE_UPDATE_SKIPPED"
        );
        verifyNoProjectionSideEffects(deps);
    }

    @Test
    void duplicateMessageShouldRecordSkippedBeforeTaskLookup() throws Exception {
        TestDeps deps = new TestDeps();
        AiTaskResultConsumer consumer = newConsumer(deps);
        AiTaskResultMessage message = buildSuccessMessage("msg-duplicate", 0);

        when(deps.inboundMessageSupportService.parseOrNull(
                eq("raw-message"),
                eq(AiTaskResultMessage.class),
                eq(KafkaTopicConstants.AI_TASK_RESULT),
                any(),
                any()
        )).thenReturn(message);
        when(deps.inboundMessageSupportService.rejectIfInvalidEnvelope(
                eq(message),
                eq(true),
                eq("raw-message"),
                eq(KafkaTopicConstants.AI_TASK_RESULT),
                any(),
                any()
        )).thenReturn(false);
        when(deps.taskMessageLogService.beginConsume(KafkaTopicConstants.AI_TASK_RESULT, message, "ai-orchestration-service"))
                .thenReturn(false);

        consumer.onMessage("raw-message");

        verify(deps.taskMessageLogService).recordSkipped(
                KafkaTopicConstants.AI_TASK_RESULT,
                message,
                "ai-orchestration-service",
                "DUPLICATE_MESSAGE"
        );
        verifyNoInteractions(deps.researchTaskMapper);
        verifyNoProjectionSideEffects(deps);
    }

    @Test
    void staleRetryGenerationShouldRecordSkippedBeforeFinalUpdate() throws Exception {
        TestDeps deps = new TestDeps();
        AiTaskResultConsumer consumer = newConsumer(deps);
        AiTaskResultMessage message = buildSuccessMessage("msg-stale-retry", 1);
        ResearchTaskDO task = buildTask(TaskStatusEnum.RUNNING.name(), 2);

        when(deps.inboundMessageSupportService.parseOrNull(
                eq("raw-message"),
                eq(AiTaskResultMessage.class),
                eq(KafkaTopicConstants.AI_TASK_RESULT),
                any(),
                any()
        )).thenReturn(message);
        when(deps.inboundMessageSupportService.rejectIfInvalidEnvelope(
                eq(message),
                eq(true),
                eq("raw-message"),
                eq(KafkaTopicConstants.AI_TASK_RESULT),
                any(),
                any()
        )).thenReturn(false);
        when(deps.taskMessageLogService.beginConsume(KafkaTopicConstants.AI_TASK_RESULT, message, "ai-orchestration-service"))
                .thenReturn(true);
        when(deps.researchTaskMapper.selectOne(any())).thenReturn(task);

        consumer.onMessage("raw-message");

        verify(deps.researchTaskMapper, never()).update(any(ResearchTaskDO.class), any());
        verify(deps.taskMessageLogService).recordSkipped(
                KafkaTopicConstants.AI_TASK_RESULT,
                message,
                "ai-orchestration-service",
                "RETRY_COUNT_MISMATCH"
        );
        verifyNoProjectionSideEffects(deps);
    }

    private static AiTaskResultConsumer newConsumer(TestDeps deps) {
        return new AiTaskResultConsumer(
                new ObjectMapper(),
                deps.researchTaskMapper,
                deps.taskStateManager,
                deps.taskTraceManager,
                deps.stringRedisTemplate,
                deps.taskCacheVersionManager,
                deps.researchReportMapper,
                deps.retryLogMapper,
                deps.aiResultDomainProjectionService,
                deps.taskDomainEventPublisherService,
                deps.taskMessageLogService,
                deps.inboundMessageSupportService
        );
    }

    private static AiTaskResultMessage buildSuccessMessage(String messageId, int retryCount) {
        AiTaskResultMessage message = new AiTaskResultMessage();
        message.setMessageId(messageId);
        message.setTaskId("task-1");
        message.setMessageType("AI_TASK_RESULT");
        message.setSourceService("quant-ai-engine");
        message.setTargetService("ai-orchestration-service");
        message.setTraceId("trace-1");
        message.setTenantId("default");
        message.setRetryCount(retryCount);

        AiTaskResultMessage.ResultPayload payload = new AiTaskResultMessage.ResultPayload();
        payload.setWorkflowInstanceId("workflow-1");
        payload.setFinalStatus(TaskStatusEnum.SUCCESS.name());
        payload.setFinalStage(TaskStageEnum.FINISHED.name());
        payload.setTaskType("RESEARCH");
        payload.setResultRef("result-ref-1");
        payload.setSummary("final summary");
        payload.setNeedHumanReview(false);
        message.setPayload(payload);
        return message;
    }

    private static ResearchTaskDO buildTask(String status, int retryCount) {
        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId("task-1");
        task.setStatus(status);
        task.setRetryCount(retryCount);
        task.setDeleted(0);
        return task;
    }

    private static void verifyNoProjectionSideEffects(TestDeps deps) {
        verifyNoInteractions(deps.researchReportMapper);
        verifyNoInteractions(deps.aiResultDomainProjectionService);
        verifyNoInteractions(deps.taskDomainEventPublisherService);
        verifyNoInteractions(deps.taskTraceManager);
        verifyNoInteractions(deps.stringRedisTemplate);
        verifyNoInteractions(deps.taskCacheVersionManager);
        verifyNoInteractions(deps.retryLogMapper);
    }

    private static final class TestDeps {
        private final ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        private final TaskStateManager taskStateManager = mock(TaskStateManager.class);
        private final TaskTraceManager taskTraceManager = mock(TaskTraceManager.class);
        private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        private final TaskCacheVersionManager taskCacheVersionManager = mock(TaskCacheVersionManager.class);
        private final ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        private final ResearchTaskRetryLogMapper retryLogMapper = mock(ResearchTaskRetryLogMapper.class);
        private final AiResultDomainProjectionService aiResultDomainProjectionService = mock(AiResultDomainProjectionService.class);
        private final TaskDomainEventPublisherService taskDomainEventPublisherService = mock(TaskDomainEventPublisherService.class);
        private final TaskMessageLogService taskMessageLogService = mock(TaskMessageLogService.class);
        private final AiTaskInboundMessageSupportService inboundMessageSupportService = mock(AiTaskInboundMessageSupportService.class);
    }
}
