package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.task.api.AiTaskStateSnapshot;
import com.quant.task.port.AiTaskStatusStatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiTaskStatusStateManager implements AiTaskStatusStatePort {

    private final ResearchTaskMapper researchTaskMapper;

    @Override
    public AiTaskStateSnapshot selectTask(String taskId) {
        return toSnapshot(researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .last("limit 1")
        ));
    }

    @Override
    public int updateTaskStage(String taskId, String status, String currentStage) {
        return researchTaskMapper.updateTaskStage(taskId, status, currentStage);
    }

    private AiTaskStateSnapshot toSnapshot(ResearchTaskDO task) {
        if (task == null) {
            return null;
        }
        AiTaskStateSnapshot snapshot = new AiTaskStateSnapshot();
        snapshot.setTaskId(task.getTaskId());
        snapshot.setTaskType(task.getTaskType());
        snapshot.setStatus(task.getStatus());
        snapshot.setRetryCount(task.getRetryCount());
        return snapshot;
    }
}
