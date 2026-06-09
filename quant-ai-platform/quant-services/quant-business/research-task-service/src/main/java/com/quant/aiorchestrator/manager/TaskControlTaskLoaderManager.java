package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskControlTaskLoaderManager {

    private final ResearchTaskMapper researchTaskMapper;

    public ResearchTaskDO selectRequiredTask(String taskId) {
        ResearchTaskDO task = researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (task == null) {
            throw new BizException("TASK_NOT_FOUND", "Task does not exist");
        }
        return task;
    }
}
