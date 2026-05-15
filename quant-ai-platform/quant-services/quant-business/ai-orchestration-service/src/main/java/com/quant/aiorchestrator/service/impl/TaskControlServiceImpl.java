package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.manager.TaskTraceManager;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.service.TaskControlService;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskControlServiceImpl implements TaskControlService {

    private final ResearchTaskMapper researchTaskMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final TaskStateManager taskStateManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final TaskTraceManager taskTraceManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String cancelTask(String taskId, TaskCancelDTO dto) {
        ResearchTaskDO task = researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (task == null) {
            throw new BizException("TASK_NOT_FOUND", "任务不存在");
        }

        if (!taskStateManager.canTransfer(task.getStatus(), TaskStatusEnum.CANCELLED.name())) {
            throw new BizException("TASK_STATUS_INVALID", "当前状态不允许取消");
        }

        String cancelReason = dto == null || dto.getCancelReason() == null || dto.getCancelReason().isBlank()
                ? "手工取消任务"
                : dto.getCancelReason();

        researchTaskMapper.updateTaskCancelled(taskId, cancelReason);

        stringRedisTemplate.opsForValue().set(
                RedisKeyBuilder.taskControl(taskId),
                buildCancelRuntimeSignal(cancelReason),
                Duration.ofHours(24)
        );

        stringRedisTemplate.opsForValue().set(
                RedisKeyBuilder.taskState(taskId),
                buildCancelledTaskState(),
                Duration.ofHours(24)
        );
        stringRedisTemplate.delete(RedisKeyBuilder.taskFull(taskId));
        stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
        taskCacheVersionManager.bumpVersion();


        AuditRecordDO audit = new AuditRecordDO();
        audit.setAuditId(UUID.randomUUID().toString());
        audit.setTaskId(taskId);
        audit.setAuditType(TaskDomainConstants.AuditType.TASK_CONTROL.name());
        audit.setAuditStage(TaskStageEnum.CANCELLED.name());
        audit.setOperatorType(TaskDomainConstants.AuditOperatorType.HUMAN.name());
        audit.setOperatorId(dto == null ? null : dto.getOperatorId());
        audit.setActionCode(TaskDomainConstants.AuditActionCode.TASK_CANCEL.name());
        audit.setActionDesc(cancelReason);
        audit.setResultStatus(TaskDomainConstants.AuditResultStatus.SUCCESS.name());
        audit.setRemark(cancelReason);
        audit.setDeleted(0);
        auditRecordMapper.insert(audit);

        taskTraceManager.finishWorkflow(
                "wf-" + taskId,
                TaskStageEnum.CANCELLED.name(),
                TaskStatusEnum.CANCELLED.name()
        );

        return taskId;
    }

    private String buildCancelRuntimeSignal(String cancelReason) {
        ObjectNode signal = objectMapper.createObjectNode();
        signal.put("cancelled", true);
        signal.put("reason", cancelReason);
        return signal.toString();
    }

    private String buildCancelledTaskState() {
        ObjectNode state = objectMapper.createObjectNode();
        state.put("status", TaskStatusEnum.CANCELLED.name());
        state.put("currentStage", TaskStageEnum.CANCELLED.name());
        state.put("progress", 100);
        return state.toString();
    }
}
