package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.aiorchestrator.service.TaskRetryService;
import com.quant.common.core.exception.BizException;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskDispatchMessage;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.redis.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskRetryServiceImpl implements TaskRetryService {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchTaskRetryLogMapper retryLogMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final TaskStateManager taskStateManager;
    private final TaskMessageLogService taskMessageLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retryTask(String taskId, TaskRetryDTO dto) {
        ResearchTaskDO task = researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (task == null) {
            throw new BizException("TASK_NOT_FOUND", "任务不存在");
        }
        if (!taskStateManager.canRetry(task.getStatus())) {
            throw new BizException("TASK_STATUS_INVALID", "仅失败任务允许重试");
        }

        int currentRetryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int nextRetryNo = currentRetryCount + 1;

        ResearchTaskRetryLogDO retryLog = new ResearchTaskRetryLogDO();
        retryLog.setTaskId(taskId);
        retryLog.setRetryNo(nextRetryNo);
        retryLog.setRetryReason(dto == null ? null : dto.getRetryReason());
        retryLog.setRetrySource(TaskDomainConstants.RetrySource.MANUAL.name());
        retryLog.setRetryStatus(TaskDomainConstants.RetryStatus.SUBMITTED.name());
        retryLog.setOperatorId(dto == null ? null : dto.getOperatorId());
        retryLog.setDeleted(0);
        retryLogMapper.insert(retryLog);

        int updated = researchTaskMapper.updateTaskRetryDispatched(
                taskId,
                TaskStatusEnum.FAILED.name(),
                currentRetryCount,
                nextRetryNo,
                TaskStatusEnum.DISPATCHED.name(),
                TaskStageEnum.RETRY_DISPATCHED.name()
        );
        if (updated != 1) {
            throw new BizException("TASK_RETRY_STATE_CHANGED", "任务状态已变化，请刷新后重试");
        }

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
        message.setSourceService("ai-orchestration-service");
        message.setTargetService("python-ai-engine");
        message.setTenantId(task.getTenantId());
        message.setBizKey(task.getTargetType() + ":" + task.getTargetCode());
        message.setTimestamp(System.currentTimeMillis());
        message.setVersion("1.0");
        message.setRetryCount(nextRetryNo);
        message.setPayload(payload);

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(KafkaTopicConstants.AI_TASK_DISPATCH, task.getTaskId(), messageJson);
            taskMessageLogService.recordProduced(KafkaTopicConstants.AI_TASK_DISPATCH, message);
        } catch (Exception e) {
            taskMessageLogService.recordFailed(KafkaTopicConstants.AI_TASK_DISPATCH, message, e.getMessage());
            throw new BizException("TASK_RETRY_DISPATCH_FAILED", "任务重试派发失败: " + e.getMessage());
        }

        stringRedisTemplate.delete(RedisKeyBuilder.taskFull(taskId));
        stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
        taskCacheVersionManager.bumpVersion();
        retryLog.setRetryStatus(TaskDomainConstants.RetryStatus.DISPATCHED.name());
        retryLogMapper.updateById(retryLog);

        return taskId;
    }
}
