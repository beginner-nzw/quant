package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.common.model.TaskDomainConstants;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.task.api.AiTaskStateSnapshot;
import com.quant.task.port.AiTaskResultStatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AiTaskResultStateManager implements AiTaskResultStatePort {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchTaskRetryLogMapper retryLogMapper;

    @Override
    public AiTaskStateSnapshot selectTask(String taskId) {
        return toSnapshot(researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .last("limit 1")
        ));
    }

    @Override
    public int updateFinalState(AiTaskResultMessage message, AiTaskStateSnapshot currentTask, String finalStage) {
        ResearchTaskDO update = new ResearchTaskDO();
        String finalStatus = message.getPayload().getFinalStatus();
        update.setStatus(finalStatus);
        update.setCurrentStage(finalStage);
        update.setResultRef(message.getPayload().getResultRef());
        update.setErrorMessage(null);
        if (TaskStatusEnum.FAILED.name().equals(finalStatus) || TaskStatusEnum.CANCELLED.name().equals(finalStatus)) {
            update.setErrorMessage(message.getPayload().getSummary());
            update.setResultRef(null);
        }
        if (TaskStatusEnum.CANCELLED.name().equals(finalStatus)) {
            update.setCurrentStage(TaskStageEnum.CANCELLED.name());
        }
        update.setFinishTime(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        return researchTaskMapper.update(
                update,
                finalStateGuard(
                        message.getTaskId(),
                        currentTask.getStatus(),
                        message.getRetryCount() == null ? 0 : message.getRetryCount()
                )
        );
    }

    @Override
    public void updateRetryLogStatus(AiTaskResultMessage message, String retryStatus) {
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

    @Override
    public String retryStatusForFinalStatus(String finalStatus) {
        if (TaskStatusEnum.FAILED.name().equals(finalStatus)) {
            return TaskDomainConstants.RetryStatus.FAILED.name();
        }
        if (TaskStatusEnum.CANCELLED.name().equals(finalStatus)) {
            return TaskDomainConstants.RetryStatus.CANCELLED.name();
        }
        return TaskDomainConstants.RetryStatus.SUCCESS.name();
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
