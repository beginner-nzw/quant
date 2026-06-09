package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskRetryStateManager {

    private final ResearchTaskRetryLogMapper retryLogMapper;

    public int currentRetryCount(ResearchTaskDO task) {
        return task.getRetryCount() == null ? 0 : task.getRetryCount();
    }

    public ResearchTaskRetryLogDO createSubmittedLog(String taskId, int retryNo, TaskRetryDTO dto) {
        ResearchTaskRetryLogDO retryLog = new ResearchTaskRetryLogDO();
        retryLog.setTaskId(taskId);
        retryLog.setRetryNo(retryNo);
        retryLog.setRetryReason(dto == null ? null : dto.getRetryReason());
        retryLog.setRetrySource(TaskDomainConstants.RetrySource.MANUAL.name());
        retryLog.setRetryStatus(TaskDomainConstants.RetryStatus.SUBMITTED.name());
        retryLog.setOperatorId(dto == null ? null : dto.getOperatorId());
        retryLog.setDeleted(0);
        retryLogMapper.insert(retryLog);
        return retryLog;
    }

    public void markDispatched(ResearchTaskRetryLogDO retryLog) {
        retryLog.setRetryStatus(TaskDomainConstants.RetryStatus.DISPATCHED.name());
        retryLogMapper.updateById(retryLog);
    }

    public String expectedFailedStatus() {
        return TaskStatusEnum.FAILED.name();
    }

    public String dispatchedStatus() {
        return TaskStatusEnum.DISPATCHED.name();
    }

    public String retryDispatchedStage() {
        return TaskStageEnum.RETRY_DISPATCHED.name();
    }
}
