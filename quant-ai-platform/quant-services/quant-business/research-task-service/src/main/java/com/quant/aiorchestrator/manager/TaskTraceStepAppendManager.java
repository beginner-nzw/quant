package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.ResearchTaskStepDO;
import com.quant.aiorchestrator.mapper.ResearchTaskStepMapper;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.task.port.TaskTraceStepAppender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TaskTraceStepAppendManager implements TaskTraceStepAppender {

    private final ResearchTaskStepMapper researchTaskStepMapper;

    @Override
    public void appendSucceededStep(String taskId, String stage, String node, int executionOrder) {
        ResearchTaskStepDO step = new ResearchTaskStepDO();
        step.setTaskId(taskId);
        step.setStepCode(stage);
        step.setStepName(stage);
        step.setAgentCode(node);
        step.setExecutionOrder(executionOrder);
        step.setStatus(TaskStatusEnum.SUCCESS.name());
        step.setStartTime(LocalDateTime.now());
        step.setFinishTime(LocalDateTime.now());
        step.setDurationMs(0L);
        step.setDeleted(0);
        researchTaskStepMapper.insert(step);
    }
}
