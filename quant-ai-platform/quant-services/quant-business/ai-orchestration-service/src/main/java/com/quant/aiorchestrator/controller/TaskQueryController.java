package com.quant.aiorchestrator.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
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
import com.quant.aiorchestrator.sentinel.TaskQuerySentinelBlockHandler;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.TaskControlService;
import com.quant.aiorchestrator.service.TaskQueryService;
import com.quant.aiorchestrator.service.TaskRetryService;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskQueryController {

    private final TaskQueryService taskQueryService;
    private final TaskRetryService taskRetryService;
    private final TaskControlService taskControlService;
    private final RoleAccessConfigService roleAccessConfigService;

    @GetMapping("/{taskId}")
    public Result<TaskDetailVO> getTaskDetail(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.getTaskDetail(taskId));
    }

    @GetMapping("/{taskId}/state")
    public Result<TaskStateVO> getTaskState(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.getTaskState(taskId));
    }

    @GetMapping("/{taskId}/steps")
    public Result<List<TaskStepVO>> listTaskSteps(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.listTaskSteps(taskId));
    }

    @GetMapping("/{taskId}/workflow")
    public Result<WorkflowInstanceVO> getWorkflowInstance(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.getWorkflowInstance(taskId));
    }

    @GetMapping("/{taskId}/agents")
    public Result<List<AgentExecutionVO>> listAgentExecutions(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.listAgentExecutions(taskId));
    }

    @GetMapping("/{taskId}/audits")
    public Result<List<AuditRecordVO>> listAuditRecords(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.listAuditRecords(taskId));
    }

    @GetMapping
    @SentinelResource(
            value = "pageTasks",
            blockHandlerClass = TaskQuerySentinelBlockHandler.class,
            blockHandler = "handlePageTasksBlock"
    )
    public Result<TaskPageVO> pageTasks(TaskPageQueryDTO queryDTO) {
        return Result.success(taskQueryService.pageTasks(queryDTO));
    }

    @PostMapping("/{taskId}/retry")
    public Result<String> retryTask(@PathVariable("taskId") String taskId,
                                    @RequestBody(required = false) TaskRetryDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_RETRY);
        return Result.success(taskRetryService.retryTask(taskId, dto));
    }

    @GetMapping("/{taskId}/retries")
    public Result<List<TaskRetryLogVO>> listRetryLogs(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.listRetryLogs(taskId));
    }

    @GetMapping("/{taskId}/full")
    @SentinelResource(
            value = "getTaskFullDetail",
            blockHandlerClass = TaskQuerySentinelBlockHandler.class,
            blockHandler = "handleTaskFullDetailBlock"
    )
    public Result<TaskFullDetailVO> getTaskFullDetail(@PathVariable("taskId") String taskId) {
        return Result.success(taskQueryService.getTaskFullDetail(taskId));
    }

    @GetMapping("/stats")
    public Result<TaskStatsVO> getTaskStats() {
        return Result.success(taskQueryService.getTaskStats());
    }

    @GetMapping("/failed")
    public Result<TaskPageVO> pageFailedTasks(TaskPageQueryDTO queryDTO) {
        queryDTO.setOnlyFailed(true);
        return Result.success(taskQueryService.pageTasks(queryDTO));
    }

    @PostMapping("/{taskId}/cancel")
    public Result<String> cancelTask(@PathVariable("taskId") String taskId,
                                     @RequestBody(required = false) TaskCancelDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_CANCEL);
        return Result.success(taskControlService.cancelTask(taskId, dto));
    }

    @PostMapping("/{taskId}/resume")
    public Result<String> resumeTask(@PathVariable("taskId") String taskId,
                                     @RequestBody(required = false) TaskWorkflowControlDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_RETRY);
        return Result.success(taskControlService.resumeTask(taskId, dto));
    }

    @PostMapping("/{taskId}/rerun")
    public Result<String> rerunNode(@PathVariable("taskId") String taskId,
                                    @RequestBody(required = false) TaskWorkflowControlDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_TASK_RETRY);
        return Result.success(taskControlService.rerunNode(taskId, dto));
    }
}
