package com.quant.aiorchestrator.service;

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

import java.util.List;

public interface TaskQueryService {
    TaskDetailVO getTaskDetail(String taskId);

    TaskStateVO getTaskState(String taskId);

    List<TaskStepVO> listTaskSteps(String taskId);

    WorkflowInstanceVO getWorkflowInstance(String taskId);

    List<AgentExecutionVO> listAgentExecutions(String taskId);

    List<AuditRecordVO> listAuditRecords(String taskId);

    TaskPageVO pageTasks(TaskPageQueryDTO queryDTO);

    List<TaskRetryLogVO> listRetryLogs(String taskId);

    TaskFullDetailVO getTaskFullDetail(String taskId);

    TaskStatsVO getTaskStats();
}
