package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.task.port.TaskControlAuditAppender;
import com.quant.task.port.TaskWorkflowTraceFinisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskControlCommandManager {

    private final ResearchTaskMapper researchTaskMapper;
    private final TaskStateManager taskStateManager;
    private final TaskWorkflowTraceFinisher taskWorkflowTraceFinisher;
    private final TaskControlRuntimeManager runtimeManager;
    private final TaskControlDispatchManager dispatchManager;
    private final TaskControlAuditAppender auditAppender;

    public String cancelTask(ResearchTaskDO task, TaskCancelDTO dto) {
        if (!taskStateManager.canTransfer(task.getStatus(), TaskStatusEnum.CANCELLED.name())) {
            throw new BizException("TASK_STATUS_INVALID", "当前状态不允许取消");
        }

        String cancelReason = dto == null || dto.getCancelReason() == null || dto.getCancelReason().isBlank()
                ? "手工取消任务"
                : dto.getCancelReason();

        researchTaskMapper.updateTaskCancelled(task.getTaskId(), cancelReason);
        runtimeManager.writeCancelRuntime(task.getTaskId(), cancelReason);
        runtimeManager.evictTaskCaches(task.getTaskId());
        auditAppender.recordCancelAudit(task.getTaskId(), dto, cancelReason);
        taskWorkflowTraceFinisher.finishWorkflow(
                "wf-" + task.getTaskId(),
                TaskStageEnum.CANCELLED.name(),
                TaskStatusEnum.CANCELLED.name()
        );
        return task.getTaskId();
    }

    public String resumeTask(ResearchTaskDO task, TaskWorkflowControlDTO dto) {
        String checkpoint = runtimeManager.workflowCheckpoint(task.getTaskId());
        if (checkpoint == null || checkpoint.isBlank()) {
            throw new BizException("WORKFLOW_CHECKPOINT_NOT_FOUND", "未找到可恢复的工作流 checkpoint");
        }

        writeWorkflowControlSignal(task.getTaskId(), "RESUME", dto);
        dispatchManager.dispatchWorkflowControl(task, dto);
        runtimeManager.evictTaskCaches(task.getTaskId());
        return task.getTaskId();
    }

    public String rerunNode(ResearchTaskDO task, TaskWorkflowControlDTO dto) {
        if (dto == null || dto.getNodeName() == null || dto.getNodeName().isBlank()) {
            throw new BizException("WORKFLOW_NODE_REQUIRED", "节点重跑必须指定 nodeName");
        }
        String nodeState = runtimeManager.workflowNodeState(task.getTaskId(), dto.getNodeName());
        if (nodeState == null || nodeState.isBlank()) {
            throw new BizException("WORKFLOW_NODE_STATE_NOT_FOUND", "未找到可重跑的节点状态");
        }

        writeWorkflowControlSignal(task.getTaskId(), "RERUN_NODE", dto);
        dispatchManager.dispatchWorkflowControl(task, dto);
        runtimeManager.evictTaskCaches(task.getTaskId());
        return task.getTaskId();
    }

    private void writeWorkflowControlSignal(String taskId, String action, TaskWorkflowControlDTO dto) {
        runtimeManager.writeWorkflowControlSignal(
                taskId,
                action,
                dto == null ? null : dto.getReason(),
                dto == null ? null : dto.getOperatorId(),
                dto == null ? null : dto.getNodeName()
        );
    }
}
