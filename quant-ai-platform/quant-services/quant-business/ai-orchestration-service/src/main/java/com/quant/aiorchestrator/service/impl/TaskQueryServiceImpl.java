package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.AgentExecutionVO;
import com.quant.aiorchestrator.domain.vo.AuditRecordVO;
import com.quant.aiorchestrator.domain.vo.TaskDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskFullDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskPageVO;
import com.quant.aiorchestrator.domain.vo.TaskRetryLogVO;
import com.quant.aiorchestrator.domain.vo.TaskStateVO;
import com.quant.aiorchestrator.domain.vo.TaskStatsVO;
import com.quant.aiorchestrator.domain.vo.TaskStepVO;
import com.quant.aiorchestrator.domain.vo.WorkflowInstanceVO;
import com.quant.aiorchestrator.manager.TaskQueryProjectionManager;
import com.quant.aiorchestrator.service.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskQueryServiceImpl implements TaskQueryService {

    private final TaskQueryProjectionManager taskQueryProjectionManager;

    @Override
    public TaskDetailVO getTaskDetail(String taskId) {
        return taskQueryProjectionManager.getTaskDetail(taskId);
    }

    @Override
    public TaskStateVO getTaskState(String taskId) {
        return taskQueryProjectionManager.getTaskState(taskId);
    }

    @Override
    public List<TaskStepVO> listTaskSteps(String taskId) {
        return taskQueryProjectionManager.listTaskSteps(taskId);
    }

    @Override
    public WorkflowInstanceVO getWorkflowInstance(String taskId) {
        return taskQueryProjectionManager.getWorkflowInstance(taskId);
    }

    @Override
    public List<AgentExecutionVO> listAgentExecutions(String taskId) {
        return taskQueryProjectionManager.listAgentExecutions(taskId);
    }

    @Override
    public List<AuditRecordVO> listAuditRecords(String taskId) {
        return taskQueryProjectionManager.listAuditRecords(taskId);
    }

    @Override
    public TaskPageVO pageTasks(TaskPageQueryDTO queryDTO) {
        return taskQueryProjectionManager.pageTasks(queryDTO);
    }

    @Override
    public List<TaskRetryLogVO> listRetryLogs(String taskId) {
        return taskQueryProjectionManager.listRetryLogs(taskId);
    }

    @Override
    public TaskFullDetailVO getTaskFullDetail(String taskId) {
        return taskQueryProjectionManager.getTaskFullDetail(taskId);
    }

    @Override
    public TaskStatsVO getTaskStats() {
        return taskQueryProjectionManager.getTaskStats();
    }
}