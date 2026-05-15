package com.quant.aiorchestrator.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
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
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.web.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskResultConsumer {

    private static final String SERVICE_NAME = "ai-orchestration-service";
    private static final String CONSUMER_GROUP = "ai-orchestration-result-group";

    private final ObjectMapper objectMapper;
    private final ResearchTaskMapper researchTaskMapper;
    private final TaskStateManager taskStateManager;
    private final TaskTraceManager taskTraceManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final ResearchReportMapper researchReportMapper;
    private final ResearchTaskRetryLogMapper retryLogMapper;
    private final AiResultDomainProjectionService aiResultDomainProjectionService;
    private final TaskDomainEventPublisherService taskDomainEventPublisherService;
    private final TaskMessageLogService taskMessageLogService;
    private final AiTaskInboundMessageSupportService inboundMessageSupportService;

    @KafkaListener(topics = KafkaTopicConstants.AI_TASK_RESULT, groupId = CONSUMER_GROUP)
    public void onMessage(String rawMessage) throws Exception {
        AiTaskResultMessage message = inboundMessageSupportService.parseOrNull(
                rawMessage,
                AiTaskResultMessage.class,
                KafkaTopicConstants.AI_TASK_RESULT,
                CONSUMER_GROUP,
                SERVICE_NAME
        );
        if (message == null) {
            return;
        }
        if (inboundMessageSupportService.rejectIfInvalidEnvelope(
                message,
                message.getPayload() != null,
                rawMessage,
                KafkaTopicConstants.AI_TASK_RESULT,
                CONSUMER_GROUP,
                SERVICE_NAME
        )) {
            return;
        }
        if (!StringUtils.hasText(message.getPayload().getFinalStatus())) {
            inboundMessageSupportService.rejectInvalidMessage(
                    message,
                    rawMessage,
                    KafkaTopicConstants.AI_TASK_RESULT,
                    CONSUMER_GROUP,
                    SERVICE_NAME,
                    "FINAL_STATUS_MISSING",
                    "payload.finalStatus is blank"
            );
            return;
        }
        log.info("consume ai task result, taskId={}, finalStatus={}",
                message.getTaskId(),
                message.getPayload().getFinalStatus());
        TraceContext.bind(message.getTraceId());
        String skipReason = null;
        boolean failed = false;
        try {
            if (!taskMessageLogService.beginConsume(KafkaTopicConstants.AI_TASK_RESULT, message, SERVICE_NAME)) {
                skipReason = "DUPLICATE_MESSAGE";
                return;
            }

            ResearchTaskDO task = researchTaskMapper.selectOne(
                    new LambdaQueryWrapper<ResearchTaskDO>()
                            .eq(ResearchTaskDO::getTaskId, message.getTaskId())
                            .last("limit 1")
            );
            if (task == null) {
                skipReason = "TASK_NOT_FOUND";
                return;
            }

            int currentRetryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
            int messageRetryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
            if (messageRetryCount != currentRetryCount) {
                log.warn("ignore ai task result because retry count mismatched, taskId={}, currentRetryCount={}, messageRetryCount={}",
                        message.getTaskId(), currentRetryCount, messageRetryCount);
                skipReason = "RETRY_COUNT_MISMATCH";
                return;
            }

            String finalStatus = message.getPayload().getFinalStatus();
            String finalStage = resolveFinalStage(message);
            String expectedStatus = task.getStatus();
            if (!taskStateManager.canTransfer(task.getStatus(), finalStatus)) {
                skipReason = "STATUS_TRANSFER_NOT_ALLOWED";
                return;
            }

            String workflowInstanceId = message.getPayload().getWorkflowInstanceId();
            if (workflowInstanceId == null || workflowInstanceId.isBlank()) {
                workflowInstanceId = "wf-" + message.getTaskId();
            }

            if (TaskStatusEnum.FAILED.name().equals(finalStatus)) {
                ResearchTaskDO update = new ResearchTaskDO();
                update.setStatus(TaskStatusEnum.FAILED.name());
                update.setCurrentStage(finalStage);
                update.setErrorMessage(message.getPayload().getSummary());
                update.setFinishTime(LocalDateTime.now());
                update.setUpdatedAt(LocalDateTime.now());
                LambdaUpdateWrapper<ResearchTaskDO> guard = finalStateGuard(
                        message.getTaskId(),
                        expectedStatus,
                        messageRetryCount
                );
                int updated = researchTaskMapper.update(
                        update,
                        guard
                );
                if (updated <= 0) {
                    skipReason = "TASK_FINAL_STATE_UPDATE_SKIPPED";
                    return;
                }
                taskTraceManager.finishWorkflow(workflowInstanceId, finalStage, finalStatus);

                stringRedisTemplate.opsForValue().set(
                        RedisKeyBuilder.taskState(message.getTaskId()),
                        """
                        {"status":"%s","currentStage":"%s","progress":100}
                        """.formatted(TaskStatusEnum.FAILED.name(), finalStage),
                        Duration.ofHours(24)
                );
                stringRedisTemplate.delete(RedisKeyBuilder.taskFull(message.getTaskId()));
                stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
                taskCacheVersionManager.bumpVersion();
                updateRetryLogStatus(message, TaskDomainConstants.RetryStatus.FAILED.name());
                return;
            }

            if (TaskStatusEnum.CANCELLED.name().equals(finalStatus)) {
                ResearchTaskDO update = new ResearchTaskDO();
                update.setStatus(TaskStatusEnum.CANCELLED.name());
                update.setCurrentStage(TaskStageEnum.CANCELLED.name());
                update.setErrorMessage(message.getPayload().getSummary());
                update.setFinishTime(LocalDateTime.now());
                update.setUpdatedAt(LocalDateTime.now());
                LambdaUpdateWrapper<ResearchTaskDO> guard = finalStateGuard(
                        message.getTaskId(),
                        expectedStatus,
                        messageRetryCount
                );
                int updated = researchTaskMapper.update(
                        update,
                        guard
                );
                if (updated <= 0) {
                    skipReason = "TASK_FINAL_STATE_UPDATE_SKIPPED";
                    return;
                }
                taskTraceManager.finishWorkflow(workflowInstanceId, finalStage, finalStatus);

                stringRedisTemplate.opsForValue().set(
                        RedisKeyBuilder.taskState(message.getTaskId()),
                        """
                        {"status":"%s","currentStage":"%s","progress":100}
                        """.formatted(TaskStatusEnum.CANCELLED.name(), finalStage),
                        Duration.ofHours(24)
                );
                stringRedisTemplate.delete(RedisKeyBuilder.taskFull(message.getTaskId()));
                stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
                taskCacheVersionManager.bumpVersion();
                updateRetryLogStatus(message, TaskDomainConstants.RetryStatus.CANCELLED.name());
                return;
            }

            ResearchTaskDO update = new ResearchTaskDO();
            update.setStatus(finalStatus);
            update.setCurrentStage(finalStage);
            update.setResultRef(message.getPayload().getResultRef());
            update.setErrorMessage(null);
            update.setFinishTime(LocalDateTime.now());
            update.setUpdatedAt(LocalDateTime.now());
            LambdaUpdateWrapper<ResearchTaskDO> guard = finalStateGuard(
                    message.getTaskId(),
                    expectedStatus,
                    messageRetryCount
            );
            int updated = researchTaskMapper.update(
                    update,
                    guard
            );
            if (updated <= 0) {
                skipReason = "TASK_FINAL_STATE_UPDATE_SKIPPED";
                return;
            }

            ResearchReportDO report = saveReport(message);
            aiResultDomainProjectionService.project(message, report);
            taskDomainEventPublisherService.publishGeneratedEvents(message, report);

            taskTraceManager.finishWorkflow(workflowInstanceId, finalStage, finalStatus);

            stringRedisTemplate.opsForValue().set(
                    RedisKeyBuilder.taskResult(message.getTaskId()),
                    objectMapper.writeValueAsString(message.getPayload()),
                    Duration.ofHours(12)
            );

            stringRedisTemplate.opsForValue().set(
                    RedisKeyBuilder.taskState(message.getTaskId()),
                    """
                    {"status":"%s","currentStage":"%s","progress":100}
                    """.formatted(finalStatus, finalStage),
                    Duration.ofHours(24)
            );
            stringRedisTemplate.delete(RedisKeyBuilder.taskFull(message.getTaskId()));
            stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
            taskCacheVersionManager.bumpVersion();
            updateRetryLogStatus(message, TaskDomainConstants.RetryStatus.SUCCESS.name());
        } catch (Exception e) {
            failed = true;
            taskMessageLogService.recordFailed(KafkaTopicConstants.AI_TASK_RESULT, message, SERVICE_NAME, e.getMessage());
            throw e;
        } finally {
            if (!failed) {
                if (skipReason == null) {
                    taskMessageLogService.recordConsumed(KafkaTopicConstants.AI_TASK_RESULT, message, SERVICE_NAME);
                } else {
                    taskMessageLogService.recordSkipped(KafkaTopicConstants.AI_TASK_RESULT, message, SERVICE_NAME, skipReason);
                }
            }
            TraceContext.clear();
        }
    }

    private String resolveFinalStage(AiTaskResultMessage message) {
        String finalStage = TaskStageEnum.normalize(message.getPayload().getFinalStage());
        if (finalStage != null && !finalStage.isBlank()) {
            return finalStage;
        }
        if (TaskStatusEnum.CANCELLED.name().equals(message.getPayload().getFinalStatus())) {
            return TaskStageEnum.CANCELLED.name();
        }
        if (TaskStatusEnum.FAILED.name().equals(message.getPayload().getFinalStatus())) {
            return TaskStageEnum.FAILED.name();
        }
        return TaskStageEnum.FINISHED.name();
    }

    private LambdaUpdateWrapper<ResearchTaskDO> finalStateGuard(String taskId,
                                                                String expectedStatus,
                                                                int expectedRetryCount) {
        LambdaUpdateWrapper<ResearchTaskDO> wrapper = new LambdaUpdateWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getTaskId, taskId)
                .eq(ResearchTaskDO::getStatus, expectedStatus)
                .eq(ResearchTaskDO::getDeleted, 0);
        if (expectedRetryCount == 0) {
            wrapper.and(retry -> retry.eq(ResearchTaskDO::getRetryCount, 0)
                    .or()
                    .isNull(ResearchTaskDO::getRetryCount));
        } else {
            wrapper.eq(ResearchTaskDO::getRetryCount, expectedRetryCount);
        }
        return wrapper;
    }

    private void updateRetryLogStatus(AiTaskResultMessage message, String retryStatus) {
        int retryNo = message.getRetryCount() == null ? 0 : message.getRetryCount();
        if (retryNo <= 0) {
            return;
        }

        ResearchTaskRetryLogDO retryLog = retryLogMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskRetryLogDO>()
                        .eq(ResearchTaskRetryLogDO::getTaskId, message.getTaskId())
                        .eq(ResearchTaskRetryLogDO::getRetryNo, retryNo)
                        .eq(ResearchTaskRetryLogDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (retryLog == null) {
            return;
        }

        retryLog.setRetryStatus(retryStatus);
        retryLogMapper.updateById(retryLog);
    }

    private ResearchReportDO saveReport(AiTaskResultMessage message) throws Exception {
        if (!TaskStatusEnum.SUCCESS.name().equals(message.getPayload().getFinalStatus())) {
            return null;
        }

        ResearchReportDO report = researchReportMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getTaskId, message.getTaskId())
                        .eq(ResearchReportDO::getDeleted, 0)
                        .last("limit 1")
        );

        boolean isNew = false;
        if (report == null) {
            report = new ResearchReportDO();
            report.setReportId(java.util.UUID.randomUUID().toString());
            report.setTaskId(message.getTaskId());
            report.setVersionNo(1);
            report.setDeleted(0);
            isNew = true;
        } else {
            report.setVersionNo(resolveNextVersionNo(report.getVersionNo()));
        }

        report.setTaskType(message.getPayload().getTaskType());
        report.setFinalStatus(message.getPayload().getFinalStatus());
        report.setSummary(message.getPayload().getSummary());
        report.setConfidenceScore(
                message.getPayload().getConfidenceScore() == null
                        ? null
                        : java.math.BigDecimal.valueOf(message.getPayload().getConfidenceScore())
        );
        report.setNeedHumanReview(Boolean.TRUE.equals(message.getPayload().getNeedHumanReview()) ? 1 : 0);
        report.setResultRef(message.getPayload().getResultRef());

        // A regenerated report must re-enter the review flow instead of inheriting the last review result.
        report.setReviewStatus(null);
        report.setReviewedBy(null);
        report.setReviewedAt(null);
        report.setRevisedSummary(null);
        report.setRevisedHighlights(null);
        report.setRevisedRiskPoints(null);
        report.setReviewComment(null);

        Map<String, Object> reportMeta = message.getPayload().getReportMeta();
        Object reportType = reportMeta == null ? null : reportMeta.get("reportType");
        report.setReportType(reportType == null ? null : String.valueOf(reportType));

        Object highlights = reportMeta == null ? null : reportMeta.get("highlights");
        report.setHighlights(highlights == null ? null : objectMapper.writeValueAsString(highlights));

        Object riskPoints = reportMeta == null ? null : reportMeta.get("riskPoints");
        report.setRiskPoints(riskPoints == null ? null : objectMapper.writeValueAsString(riskPoints));

        if (message.getPayload().getRiskWarnings() != null) {
            report.setRiskWarnings(objectMapper.writeValueAsString(message.getPayload().getRiskWarnings()));
        } else {
            report.setRiskWarnings("[]");
        }

        report.setRawPayload(objectMapper.writeValueAsString(message.getPayload()));

        if (isNew) {
            researchReportMapper.insert(report);
        } else {
            researchReportMapper.updateById(report);
        }
        return report;
    }

    private int resolveNextVersionNo(Integer currentVersionNo) {
        if (currentVersionNo == null || currentVersionNo < 1) {
            return 2;
        }
        return currentVersionNo + 1;
    }
}
