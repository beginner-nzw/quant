package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.service.ReportQueryService;
import com.quant.aiorchestrator.service.TaskAuditProjectionProvider;
import com.quant.aiorchestrator.service.TaskWorkflowProjectionProvider;
import com.quant.task.manager.TaskCacheVersionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskQueryProjectionManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final TaskStateManager taskStateManager;
    private final TaskQueryReadManager taskQueryReadManager;
    private final TaskWorkflowProjectionProvider workflowProjectionProvider;
    private final TaskAuditProjectionProvider auditProjectionProvider;
    private final TaskFullDetailProjectionManager taskFullDetailProjectionManager;
    private final TaskQueryItemAssembler itemAssembler;
    private final TaskStateProjectionManager taskStateProjectionManager;
    private final TaskPageProjectionManager taskPageProjectionManager;
    private final TaskStatsManager taskStatsManager;

    @Autowired
    public TaskQueryProjectionManager(StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      TaskCacheVersionManager taskCacheVersionManager,
                                      TaskStateManager taskStateManager,
                                      ReportQueryService reportQueryService,
                                      TaskQueryReadManager taskQueryReadManager,
                                      TaskWorkflowProjectionProvider workflowProjectionProvider,
                                      TaskAuditProjectionProvider auditProjectionProvider,
                                      TaskFullDetailProjectionManager taskFullDetailProjectionManager,
                                      TaskStateProjectionManager taskStateProjectionManager,
                                      TaskPageProjectionManager taskPageProjectionManager,
                                      TaskStatsManager taskStatsManager) {
        this(
                stringRedisTemplate,
                objectMapper,
                taskCacheVersionManager,
                taskStateManager,
                reportQueryService,
                taskQueryReadManager,
                workflowProjectionProvider,
                auditProjectionProvider,
                taskFullDetailProjectionManager,
                new TaskQueryItemAssembler(objectMapper),
                taskStateProjectionManager,
                taskPageProjectionManager,
                taskStatsManager
        );
    }

    public TaskQueryProjectionManager(StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      TaskCacheVersionManager taskCacheVersionManager,
                                      TaskStateManager taskStateManager,
                                      ReportQueryService reportQueryService,
                                      TaskQueryReadManager taskQueryReadManager,
                                      TaskWorkflowProjectionProvider workflowProjectionProvider,
                                      TaskAuditProjectionProvider auditProjectionProvider,
                                      TaskFullDetailProjectionManager taskFullDetailProjectionManager,
                                      TaskQueryItemAssembler itemAssembler,
                                      TaskStateProjectionManager taskStateProjectionManager,
                                      TaskPageProjectionManager taskPageProjectionManager,
                                      TaskStatsManager taskStatsManager) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.taskCacheVersionManager = taskCacheVersionManager;
        this.taskStateManager = taskStateManager;
        this.taskQueryReadManager = taskQueryReadManager;
        this.workflowProjectionProvider = workflowProjectionProvider;
        this.auditProjectionProvider = auditProjectionProvider;
        this.taskFullDetailProjectionManager = taskFullDetailProjectionManager;
        this.itemAssembler = itemAssembler;
        this.taskStateProjectionManager = taskStateProjectionManager;
        this.taskPageProjectionManager = taskPageProjectionManager;
        this.taskStatsManager = taskStatsManager;
    }

    public TaskDetailVO getTaskDetail(String taskId) {
        ResearchTaskDO task = selectTaskById(taskId);
        return itemAssembler.toTaskDetailVO(task);
    }
    public TaskStateVO getTaskState(String taskId) {
        return taskStateProjectionManager.getTaskState(taskId);
    }
    public List<TaskStepVO> listTaskSteps(String taskId) {
        return taskQueryReadManager.listTaskSteps(taskId).stream().map(itemAssembler::toTaskStepVO).toList();
    }
    public WorkflowInstanceVO getWorkflowInstance(String taskId) {
        WorkflowInstanceVO vo = workflowProjectionProvider.getWorkflowInstance(taskId);
        if (vo == null) {
            return null;
        }
        ResearchTaskDO task = selectTaskById(taskId);
        if (task != null
                && taskStateManager.isFinalState(task.getStatus())
                && !taskStateManager.isFinalState(vo.getStatus())) {
            vo.setStatus(task.getStatus());
            vo.setCurrentNode(task.getCurrentStage());
            vo.setFinishTime(task.getFinishTime());
        }
        return vo;
    }

    private ResearchTaskDO selectTaskById(String taskId) {
        return taskQueryReadManager.selectTaskById(taskId);
    }

    public List<AgentExecutionVO> listAgentExecutions(String taskId) {
        return workflowProjectionProvider.listAgentExecutions(taskId);
    }
    public List<AuditRecordVO> listAuditRecords(String taskId) {
        return auditProjectionProvider.listAuditRecords(taskId);
    }
    public TaskPageVO pageTasks(TaskPageQueryDTO queryDTO) {
        return taskPageProjectionManager.pageTasks(queryDTO);
    }
    public List<TaskRetryLogVO> listRetryLogs(String taskId) {
        return taskQueryReadManager.listRetryLogs(taskId).stream().map(itemAssembler::toRetryLogVO).toList();
    }
    public TaskFullDetailVO getTaskFullDetail(String taskId) {
        return taskFullDetailProjectionManager.getTaskFullDetail(taskId, this);
    }
    public TaskStatsVO getTaskStats() {
        return taskStatsManager.getTaskStats();
    }
}
