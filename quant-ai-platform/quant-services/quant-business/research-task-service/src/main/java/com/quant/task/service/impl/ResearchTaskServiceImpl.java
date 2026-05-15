package com.quant.task.service.impl;

import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisLockHelper;
import com.quant.task.domain.dto.CreateResearchTaskDTO;
import com.quant.task.domain.entity.ResearchTaskDO;
import com.quant.task.manager.ResearchTaskManager;
import com.quant.task.manager.TaskCacheVersionManager;
import com.quant.task.mapper.ResearchTaskMapper;
import com.quant.task.service.ResearchTaskService;
import com.quant.task.service.TaskOutboxMessageService;
import com.quant.task.support.TaskRoutingSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchTaskServiceImpl implements ResearchTaskService {

    private final ResearchTaskManager researchTaskManager;
    private final ResearchTaskMapper researchTaskMapper;
    private final RedisLockHelper redisLockHelper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final TaskOutboxMessageService taskOutboxMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTask(CreateResearchTaskDTO dto) {
        dto.setTaskType(TaskRoutingSupport.resolveTaskType(dto.getTaskType(), dto.getAnalysisScope()));
        dto.setAnalysisScope(TaskRoutingSupport.resolveAnalysisScope(dto.getTaskType(), dto.getAnalysisScope()));

        String hotKey = "hot:create:target:" + dto.getTargetCode();
        Long hotCount = stringRedisTemplate.opsForValue().increment(hotKey);
        if (hotCount != null && hotCount == 1) {
            stringRedisTemplate.expire(hotKey, Duration.ofSeconds(1));
        }

        if (hotCount != null && hotCount > 5) {
            throw new BizException("HOT_TARGET_LIMITED", "该标的任务创建过于频繁，请稍后再试");
        }

        String lockKey = "lock:task:create:" + dto.getTaskType() + ":" + dto.getTargetCode();
        String lockValue = redisLockHelper.tryLock(lockKey, Duration.ofSeconds(10));
        if (lockValue == null) {
            throw new BizException("TASK_DUPLICATE", "任务提交过于频繁，请稍后重试");
        }

        try {
            ResearchTaskDO task = researchTaskManager.createTask(dto);
            log.info("create research task, targetCode={}, taskType={}", dto.getTargetCode(), dto.getTaskType());
            taskOutboxMessageService.enqueueAiTaskDispatch(task);

            researchTaskMapper.updateTaskStage(task.getTaskId(), TaskStatusEnum.DISPATCHED.name(), TaskStageEnum.DISPATCHED.name());
            researchTaskManager.writeTaskStateCache(task.getTaskId(), TaskStatusEnum.DISPATCHED.name(), TaskStageEnum.DISPATCHED.name(), 5);
            stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
            taskCacheVersionManager.bumpVersion();

            return task.getTaskId();
        } finally {
            redisLockHelper.unlock(lockKey, lockValue);
        }
    }
}
