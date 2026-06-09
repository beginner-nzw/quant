package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.vo.AgentExecutionVO;
import com.quant.aiorchestrator.domain.vo.AuditRecordVO;
import com.quant.aiorchestrator.domain.vo.TaskDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskFullDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskRetryLogVO;
import com.quant.aiorchestrator.domain.vo.TaskStepVO;
import com.quant.aiorchestrator.domain.vo.TaskSummaryVO;
import com.quant.aiorchestrator.domain.vo.WorkflowCheckpointVO;
import com.quant.aiorchestrator.service.ReportQueryService;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TaskFullDetailProjectionManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ReportQueryService reportQueryService;

    public TaskFullDetailVO getTaskFullDetail(String taskId, TaskQueryProjectionManager taskQueryProjectionManager) {
        String cacheKey = RedisKeyBuilder.taskFull(taskId);
        String cache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null && !cache.isBlank()) {
            try {
                return objectMapper.readValue(cache, TaskFullDetailVO.class);
            } catch (Exception ignored) {
            }
        }

        TaskDetailVO detail = taskQueryProjectionManager.getTaskDetail(taskId);
        if (detail == null) {
            throw new BizException("TASK_NOT_FOUND", "任务不存在");
        }

        List<TaskStepVO> steps = taskQueryProjectionManager.listTaskSteps(taskId);
        List<AgentExecutionVO> agents = taskQueryProjectionManager.listAgentExecutions(taskId);
        List<AuditRecordVO> audits = taskQueryProjectionManager.listAuditRecords(taskId);
        List<TaskRetryLogVO> retries = taskQueryProjectionManager.listRetryLogs(taskId);

        TaskFullDetailVO vo = new TaskFullDetailVO();
        vo.setTaskDetail(detail);
        vo.setTaskState(taskQueryProjectionManager.getTaskState(taskId));
        vo.setSummary(buildSummary(detail, steps, agents, audits, retries));
        vo.setReport(reportQueryService.getTaskReportOnly(taskId));
        vo.setSteps(steps);
        vo.setWorkflow(taskQueryProjectionManager.getWorkflowInstance(taskId));
        vo.setCheckpoint(getWorkflowCheckpoint(taskId));
        vo.setAgents(agents);
        vo.setAudits(audits);
        vo.setRetries(retries);

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(vo),
                    Duration.ofSeconds(30)
            );
        } catch (Exception ignored) {
        }

        return vo;
    }

    private TaskSummaryVO buildSummary(TaskDetailVO detail,
                                       List<TaskStepVO> steps,
                                       List<AgentExecutionVO> agents,
                                       List<AuditRecordVO> audits,
                                       List<TaskRetryLogVO> retries) {
        TaskSummaryVO summary = new TaskSummaryVO();
        summary.setStepCount(steps.size());
        summary.setSuccessStepCount((int) steps.stream().filter(s -> TaskStatusEnum.SUCCESS.name().equals(s.getStatus())).count());
        summary.setFailedStepCount((int) steps.stream().filter(s -> TaskStatusEnum.FAILED.name().equals(s.getStatus())).count());
        summary.setAgentCount(agents.size());
        summary.setRetryCount(retries.size());
        summary.setHasAudit(!audits.isEmpty());
        summary.setHasFailure(TaskStatusEnum.FAILED.name().equals(detail.getStatus()) || summary.getFailedStepCount() > 0);
        return summary;
    }

    private WorkflowCheckpointVO getWorkflowCheckpoint(String taskId) {
        String raw = stringRedisTemplate.opsForValue().get(RedisKeyBuilder.taskWorkflowCheckpoint(taskId));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode json = objectMapper.readTree(raw);
            WorkflowCheckpointVO vo = new WorkflowCheckpointVO();
            vo.setTaskId(json.path("taskId").asText(null));
            vo.setWorkflowInstanceId(json.path("workflowInstanceId").asText(null));
            vo.setCurrentNode(json.path("currentNode").asText(null));
            vo.setStatus(json.path("status").asText(null));
            vo.setProgress(json.path("progress").isMissingNode() ? null : json.path("progress").asInt());
            vo.setCurrentStage(json.path("currentStage").asText(null));
            vo.setRetryCount(json.path("retryCount").isMissingNode() ? null : json.path("retryCount").asInt());
            vo.setUpdatedAt(json.path("updatedAt").isMissingNode() ? null : json.path("updatedAt").asLong());
            vo.setFailureMessage(json.path("state").path("checkpoint_error").asText(null));
            vo.setResumable(!TaskStatusEnum.SUCCESS.name().equals(vo.getStatus()));
            if (json.path("branchDecisions").isArray()) {
                vo.setBranchDecisions(objectMapper.convertValue(
                        json.path("branchDecisions"),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
                ));
            } else {
                vo.setBranchDecisions(List.of());
            }
            return vo;
        } catch (Exception ignored) {
            return null;
        }
    }
}
