package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.common.core.exception.BizException;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.task.manager.TaskCacheVersionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskRetryCommandManager {

    private final ResearchTaskMapper researchTaskMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final TaskStateManager taskStateManager;
    private final TaskRetryStateManager taskRetryStateManager;
    private final TaskRetryDispatchManager taskRetryDispatchManager;

    public String retryTask(String taskId, TaskRetryDTO dto) {
        ResearchTaskDO task = loadTask(taskId);
        if (task == null) {
            throw new BizException("TASK_NOT_FOUND", "task not found");
        }
        if (!taskStateManager.canRetry(task.getStatus())) {
            throw new BizException("TASK_STATUS_INVALID", "only failed task can be retried");
        }

        int currentRetryCount = taskRetryStateManager.currentRetryCount(task);
        int nextRetryNo = currentRetryCount + 1;
        ResearchTaskRetryLogDO retryLog = taskRetryStateManager.createSubmittedLog(taskId, nextRetryNo, dto);

        int updated = researchTaskMapper.updateTaskRetryDispatched(
                taskId,
                taskRetryStateManager.expectedFailedStatus(),
                currentRetryCount,
                nextRetryNo,
                taskRetryStateManager.dispatchedStatus(),
                taskRetryStateManager.retryDispatchedStage()
        );
        if (updated != 1) {
            throw new BizException("TASK_RETRY_STATE_CHANGED", "task state changed, please refresh and retry");
        }

        taskRetryDispatchManager.dispatchRetry(task, dto, nextRetryNo);
        clearTaskCache(taskId);
        taskRetryStateManager.markDispatched(retryLog);
        return taskId;
    }

    private ResearchTaskDO loadTask(String taskId) {
        return researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .last("limit 1")
        );
    }

    private void clearTaskCache(String taskId) {
        stringRedisTemplate.delete(RedisKeyBuilder.taskFull(taskId));
        stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
        taskCacheVersionManager.bumpVersion();
    }
}
