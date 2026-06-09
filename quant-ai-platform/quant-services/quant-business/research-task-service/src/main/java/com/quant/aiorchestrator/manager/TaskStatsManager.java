package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.TaskStatsVO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TaskStatsManager {

    private final ResearchTaskMapper researchTaskMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public TaskStatsVO getTaskStats() {
        String cache = stringRedisTemplate.opsForValue().get(RedisKeyConstants.TASK_STATS_GLOBAL);
        if (cache != null && !cache.isBlank()) {
            try {
                return objectMapper.readValue(cache, TaskStatsVO.class);
            } catch (Exception ignored) {
            }
        }

        TaskStatsVO vo = new TaskStatsVO();
        vo.setTotalCount(countBy(null, false));
        vo.setRunningCount(countBy(TaskStatusEnum.RUNNING.name(), false));
        vo.setSuccessCount(countBy(TaskStatusEnum.SUCCESS.name(), false));
        vo.setFailedCount(countBy(TaskStatusEnum.FAILED.name(), false));
        vo.setRetriedCount(countBy(null, true));

        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.TASK_STATS_GLOBAL,
                    objectMapper.writeValueAsString(vo),
                    Duration.ofSeconds(15)
            );
        } catch (Exception ignored) {
        }

        return vo;
    }

    private Long countBy(String status, boolean onlyRetried) {
        LambdaQueryWrapper<ResearchTaskDO> wrapper = new LambdaQueryWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getDeleted, 0);
        if (status != null) {
            wrapper.eq(ResearchTaskDO::getStatus, status);
        }
        if (onlyRetried) {
            wrapper.gt(ResearchTaskDO::getRetryCount, 0);
        }
        return researchTaskMapper.selectCount(wrapper);
    }
}
