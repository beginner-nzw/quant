package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.*;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.mapper.*;
import com.quant.aiorchestrator.service.ReportQueryService;
import com.quant.aiorchestrator.service.TaskQueryService;
import com.quant.aiorchestrator.util.CacheKeyUtil;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.redis.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskQueryServiceImpl implements TaskQueryService {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchTaskStepMapper researchTaskStepMapper;
    private final AiWorkflowInstanceMapper aiWorkflowInstanceMapper;
    private final AiAgentExecutionMapper aiAgentExecutionMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ResearchTaskRetryLogMapper researchTaskRetryLogMapper;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final ResearchReportMapper researchReportMapper;
    private final TaskStateManager taskStateManager;
    private final ReportQueryService reportQueryService;


    @Override
    public TaskDetailVO getTaskDetail(String taskId) {
        ResearchTaskDO task = selectTaskById(taskId);
        if (task == null) {
            return null;
        }
        TaskDetailVO vo = new TaskDetailVO();
        BeanUtils.copyProperties(task, vo);
        if (!shouldDisplayTaskErrorMessage(task.getStatus())) {
            vo.setErrorMessage(null);
        }
        return vo;
    }

    @Override
    public TaskStateVO getTaskState(String taskId) {
        TaskStateVO vo = new TaskStateVO();
        vo.setTaskId(taskId);

        String cache = stringRedisTemplate.opsForValue().get(RedisKeyBuilder.taskState(taskId));
        ResearchTaskDO task = null;
        if (cache != null && !cache.isBlank()) {
            try {
                JsonNode json = objectMapper.readTree(cache);
                String cachedStatus = json.path("status").asText();
                if (!taskStateManager.isFinalState(cachedStatus)) {
                    task = selectTaskById(taskId);
                    if (task != null && taskStateManager.isFinalState(task.getStatus())) {
                        vo.setStatus(task.getStatus());
                        vo.setCurrentStage(task.getCurrentStage());
                        vo.setProgress(100);
                        vo.setSource("mysql");
                        refreshTaskStateCache(taskId, task.getStatus(), task.getCurrentStage(), 100);
                        return vo;
                    }
                }

                vo.setStatus(cachedStatus);
                vo.setCurrentStage(json.path("currentStage").asText());
                vo.setProgress(json.path("progress").asInt());
                vo.setSource("redis");
                return vo;
            } catch (Exception ignored) {
            }
        }

        if (task == null) {
            task = selectTaskById(taskId);
        }
        if (task != null) {
            vo.setStatus(task.getStatus());
            vo.setCurrentStage(task.getCurrentStage());
            vo.setProgress(taskStateManager.isFinalState(task.getStatus()) ? 100 : null);
            vo.setSource("mysql");
        }
        return vo;
    }

    @Override
    public List<TaskStepVO> listTaskSteps(String taskId) {
        return researchTaskStepMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskStepDO>()
                        .eq(ResearchTaskStepDO::getTaskId, taskId)
                        .eq(ResearchTaskStepDO::getDeleted, 0)
                        .orderByAsc(ResearchTaskStepDO::getExecutionOrder, ResearchTaskStepDO::getId)
        ).stream().map(this::toTaskStepVO).toList();
    }

    @Override
    public WorkflowInstanceVO getWorkflowInstance(String taskId) {
        AiWorkflowInstanceDO workflow = aiWorkflowInstanceMapper.selectOne(
                new LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getTaskId, taskId)
                        .eq(AiWorkflowInstanceDO::getDeleted, 0)
                        .orderByDesc(AiWorkflowInstanceDO::getCreatedAt, AiWorkflowInstanceDO::getId)
                        .last("limit 1")
        );
        if (workflow == null) {
            return null;
        }
        WorkflowInstanceVO vo = new WorkflowInstanceVO();
        BeanUtils.copyProperties(workflow, vo);
        ResearchTaskDO task = selectTaskById(taskId);
        if (task != null
                && taskStateManager.isFinalState(task.getStatus())
                && !taskStateManager.isFinalState(workflow.getStatus())) {
            vo.setStatus(task.getStatus());
            vo.setCurrentNode(task.getCurrentStage());
            vo.setFinishTime(task.getFinishTime());
        }
        return vo;
    }

    private ResearchTaskDO selectTaskById(String taskId) {
        return researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .last("limit 1")
        );
    }

    private void refreshTaskStateCache(String taskId, String status, String currentStage, int progress) {
        String stateJson = """
                {"status":"%s","currentStage":"%s","progress":%d}
                """.formatted(status, currentStage, progress);
        stringRedisTemplate.opsForValue().set(
                RedisKeyBuilder.taskState(taskId),
                stateJson,
                Duration.ofHours(24)
        );
    }

    @Override
    public List<AgentExecutionVO> listAgentExecutions(String taskId) {
        return aiAgentExecutionMapper.selectList(
                new LambdaQueryWrapper<AiAgentExecutionDO>()
                        .eq(AiAgentExecutionDO::getTaskId, taskId)
                        .eq(AiAgentExecutionDO::getDeleted, 0)
                        .orderByAsc(AiAgentExecutionDO::getId)
        ).stream().map(this::toAgentExecutionVO).toList();
    }

    @Override
    public List<AuditRecordVO> listAuditRecords(String taskId) {
        return auditRecordMapper.selectList(
                new LambdaQueryWrapper<AuditRecordDO>()
                        .eq(AuditRecordDO::getTaskId, taskId)
                        .eq(AuditRecordDO::getDeleted, 0)
                        .orderByDesc(AuditRecordDO::getId)
        ).stream().map(this::toAuditRecordVO).toList();
    }

    private TaskStepVO toTaskStepVO(ResearchTaskStepDO entity) {
        TaskStepVO vo = new TaskStepVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private AgentExecutionVO toAgentExecutionVO(AiAgentExecutionDO entity) {
        AgentExecutionVO vo = new AgentExecutionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private AuditRecordVO toAuditRecordVO(AuditRecordDO entity) {
        AuditRecordVO vo = new AuditRecordVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }


    @Override
    public TaskPageVO pageTasks(TaskPageQueryDTO queryDTO) {
        int pageNum = queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1 ? 1 : queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : queryDTO.getPageSize();

        String queryFingerprint = String.format(
                "pageNum=%d&pageSize=%d&taskType=%s&status=%s&targetCode=%s&targetName=%s&onlyFailed=%s&hasRetry=%s&onlyPendingReview=%s&reportReviewStatus=%s&reportReviewedBy=%s",
                pageNum,
                pageSize,
                queryDTO.getTaskType(),
                queryDTO.getStatus(),
                queryDTO.getTargetCode(),
                queryDTO.getTargetName(),
                queryDTO.getOnlyFailed(),
                queryDTO.getHasRetry(),
                queryDTO.getOnlyPendingReview(),
                queryDTO.getReportReviewStatus(),
                queryDTO.getReportReviewedBy()
        );

        String cacheKey = RedisKeyConstants.TASK_LIST_CACHE_PREFIX + taskCacheVersionManager.currentVersion() + ":" +
                CacheKeyUtil.md5(queryFingerprint);
        String cache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null && !cache.isBlank()) {
            try {
                return objectMapper.readValue(cache, TaskPageVO.class);
            } catch (Exception ignored) {
            }
        }

        LambdaQueryWrapper<ResearchReportDO> reportWrapper = new LambdaQueryWrapper<ResearchReportDO>()
                .eq(ResearchReportDO::getDeleted, 0);

        boolean needReportFilter = false;

        String targetReviewStatus;
        if (Boolean.TRUE.equals(queryDTO.getOnlyPendingReview())) {
            targetReviewStatus = ReportReviewStatusEnum.PENDING.name();
        } else if (queryDTO.getReportReviewStatus() != null && !queryDTO.getReportReviewStatus().isBlank()) {
            targetReviewStatus = queryDTO.getReportReviewStatus();
        } else {
            targetReviewStatus = null;
        }

        if (targetReviewStatus != null) {
            if (ReportReviewStatusEnum.PENDING.name().equals(targetReviewStatus)) {
                reportWrapper.and(wrapper -> wrapper
                        .isNull(ResearchReportDO::getReviewStatus)
                        .or()
                        .eq(ResearchReportDO::getReviewStatus, targetReviewStatus));
            } else {
                reportWrapper.eq(ResearchReportDO::getReviewStatus, targetReviewStatus);
            }
            needReportFilter = true;
        }

        if (queryDTO.getReportReviewedBy() != null && !queryDTO.getReportReviewedBy().isBlank()) {
            reportWrapper.like(ResearchReportDO::getReviewedBy, queryDTO.getReportReviewedBy());
            needReportFilter = true;
        }

        Set<String> reportFilteredTaskIds = null;
        if (needReportFilter) {
            var reports = researchReportMapper.selectList(reportWrapper);

            reportFilteredTaskIds = reports.stream()
                    .map(ResearchReportDO::getTaskId)
                    .collect(Collectors.toSet());

            if (reportFilteredTaskIds.isEmpty()) {
                TaskPageVO empty = new TaskPageVO();
                empty.setTotal(0L);
                empty.setPageNum((long) pageNum);
                empty.setPageSize((long) pageSize);
                empty.setRecords(List.of());
                return empty;
            }
        }

        LambdaQueryWrapper<ResearchTaskDO> wrapper = new LambdaQueryWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getDeleted, 0)
                .orderByDesc(ResearchTaskDO::getId);
        if (reportFilteredTaskIds != null) {
            wrapper.in(ResearchTaskDO::getTaskId, reportFilteredTaskIds);
        }
        if (queryDTO.getTaskType() != null && !queryDTO.getTaskType().isBlank()) {
            wrapper.eq(ResearchTaskDO::getTaskType, queryDTO.getTaskType());
        }
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isBlank()) {
            wrapper.eq(ResearchTaskDO::getStatus, queryDTO.getStatus());
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyFailed())) {
            wrapper.eq(ResearchTaskDO::getStatus, TaskStatusEnum.FAILED.name());
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()) {
            wrapper.eq(ResearchTaskDO::getTargetCode, queryDTO.getTargetCode());
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()) {
            wrapper.like(ResearchTaskDO::getTargetName, queryDTO.getTargetName());
        }
        if (Boolean.TRUE.equals(queryDTO.getHasRetry())) {
            wrapper.gt(ResearchTaskDO::getRetryCount, 0);
        }

        Page<ResearchTaskDO> page =
                new Page<>(pageNum, pageSize);

        Page<ResearchTaskDO> result =
                researchTaskMapper.selectPage(page, wrapper);

        TaskPageVO vo = new TaskPageVO();
        vo.setTotal(result.getTotal());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        Map<String, ResearchReportDO> reportMap = Collections.emptyMap();
        if (!result.getRecords().isEmpty()) {
            var taskIds = result.getRecords().stream().map(ResearchTaskDO::getTaskId).toList();
            var reports = researchReportMapper.selectList(
                    new LambdaQueryWrapper<ResearchReportDO>()
                            .eq(ResearchReportDO::getDeleted, 0)
                            .in(ResearchReportDO::getTaskId, taskIds)
            );

            reportMap = reports.stream().collect(
                    java.util.stream.Collectors.toMap(
                            ResearchReportDO::getTaskId,
                            item -> item,
                            (a, b) -> a
                    )
            );
        }

        Map<String, ResearchReportDO> finalReportMap = reportMap;
        vo.setRecords(result.getRecords().stream().map(task -> {
            TaskListItemVO item = new TaskListItemVO();
            BeanUtils.copyProperties(task, item);
            if (!shouldDisplayTaskErrorMessage(task.getStatus())) {
                item.setErrorMessage(null);
            }

            ResearchReportDO report = finalReportMap.get(task.getTaskId());
            if (report != null) {
                item.setReportId(report.getReportId());
                item.setReportType(resolveReportType(report, task));
                item.setReportReviewStatus(report.getReviewStatus() == null ? ReportReviewStatusEnum.PENDING.name() : report.getReviewStatus());
                item.setRevised(isReportRevised(report));
                item.setSummaryRevised(isSummaryRevised(report));
                item.setHighlightsRevised(isHighlightsRevised(report));
                item.setRiskPointsRevised(isRiskPointsRevised(report));
                item.setReportReviewedBy(report.getReviewedBy());
                item.setReportReviewedAt(report.getReviewedAt());
                item.setReportReviewComment(report.getReviewComment());
            }
            return item;
        }).toList());

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(vo),
                    Duration.ofSeconds(20)
            );
        } catch (Exception ignored) {
        }

        return vo;
    }

    @Override
    public List<TaskRetryLogVO> listRetryLogs(String taskId) {
        return researchTaskRetryLogMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskRetryLogDO>()
                        .eq(ResearchTaskRetryLogDO::getTaskId, taskId)
                        .eq(ResearchTaskRetryLogDO::getDeleted, 0)
                        .orderByAsc(ResearchTaskRetryLogDO::getRetryNo)
        ).stream().map(item -> {
            TaskRetryLogVO vo = new TaskRetryLogVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList();
    }

    @Override
    public TaskFullDetailVO getTaskFullDetail(String taskId) {
        String cacheKey = RedisKeyBuilder.taskFull(taskId);
        String cache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null && !cache.isBlank()) {
            try {
                return objectMapper.readValue(cache, TaskFullDetailVO.class);
            } catch (Exception ignored) {
            }
        }

        TaskDetailVO detail = getTaskDetail(taskId);
        if (detail == null) {
            throw new BizException("TASK_NOT_FOUND", "任务不存在");
        }

        var steps = listTaskSteps(taskId);
        var agents = listAgentExecutions(taskId);
        var audits = listAuditRecords(taskId);
        var retries = listRetryLogs(taskId);

        TaskSummaryVO summary = new TaskSummaryVO();
        summary.setStepCount(steps.size());
        summary.setSuccessStepCount((int) steps.stream().filter(s -> TaskStatusEnum.SUCCESS.name().equals(s.getStatus())).count());
        summary.setFailedStepCount((int) steps.stream().filter(s -> TaskStatusEnum.FAILED.name().equals(s.getStatus())).count());
        summary.setAgentCount(agents.size());
        summary.setRetryCount(retries.size());
        summary.setHasAudit(!audits.isEmpty());
        summary.setHasFailure(TaskStatusEnum.FAILED.name().equals(detail.getStatus()) || summary.getFailedStepCount() > 0);

        TaskFullDetailVO vo = new TaskFullDetailVO();
        vo.setTaskDetail(detail);
        vo.setTaskState(getTaskState(taskId));
        vo.setSummary(summary);
        vo.setReport(reportQueryService.getTaskReportOnly(taskId));
        vo.setSteps(steps);
        vo.setWorkflow(getWorkflowInstance(taskId));
        vo.setAgents(agents);
        vo.setAudits(audits);
        vo.setRetries(retries);

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(vo),
                    java.time.Duration.ofSeconds(30)
            );
        } catch (Exception ignored) {
        }

        return vo;
    }

    @Override
    public TaskStatsVO getTaskStats() {
        String cacheKey = RedisKeyConstants.TASK_STATS_GLOBAL;
        String cache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null && !cache.isBlank()) {
            try {
                return objectMapper.readValue(cache, TaskStatsVO.class);
            } catch (Exception ignored) {
            }
        }

        TaskStatsVO vo = new TaskStatsVO();

        vo.setTotalCount(researchTaskMapper.selectCount(
                new LambdaQueryWrapper<ResearchTaskDO>().eq(ResearchTaskDO::getDeleted, 0)
        ));

        vo.setRunningCount(researchTaskMapper.selectCount(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getStatus, TaskStatusEnum.RUNNING.name())
        ));

        vo.setSuccessCount(researchTaskMapper.selectCount(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getStatus, TaskStatusEnum.SUCCESS.name())
        ));

        vo.setFailedCount(researchTaskMapper.selectCount(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getStatus, TaskStatusEnum.FAILED.name())
        ));

        vo.setRetriedCount(researchTaskMapper.selectCount(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .gt(ResearchTaskDO::getRetryCount, 0)
        ));

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(vo),
                    java.time.Duration.ofSeconds(15)
            );
        } catch (Exception ignored) {
        }

        return vo;
    }


    private String resolveReportCenterSummary(ResearchReportDO report) {
        return resolveDisplaySummary(report.getRevisedSummary(), report.getSummary());
    }

    private boolean isReportRevised(ResearchReportDO report) {
        return isSummaryRevised(report) || isHighlightsRevised(report) || isRiskPointsRevised(report);
    }

    private boolean isSummaryRevised(ResearchReportDO report) {
        if (report == null) {
            return false;
        }
        String revisedSummary = report.getRevisedSummary();
        return revisedSummary != null && !revisedSummary.isBlank() && !Objects.equals(
                normalizeText(report.getSummary()),
                normalizeText(revisedSummary)
        );
    }

    private boolean isHighlightsRevised(ResearchReportDO report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getHighlights()).equals(
                readPreferredTextList(report.getRevisedHighlights(), report.getHighlights())
        );
    }

    private boolean isRiskPointsRevised(ResearchReportDO report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getRiskPoints()).equals(
                readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints())
        );
    }

    private String resolveReportType(ResearchReportDO report, ResearchTaskDO task) {
        if (report != null && report.getReportType() != null && !report.getReportType().isBlank()) {
            return report.getReportType();
        }
        return task == null ? null : task.getTaskType();
    }

    private String resolveDisplaySummary(String preferredSummary, String fallbackSummary) {
        String normalizedPreferred = normalizeText(preferredSummary);
        return normalizedPreferred != null ? normalizedPreferred : normalizeText(fallbackSummary);
    }

    private List<String> readTextList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !item.isBlank())
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> readPreferredTextList(String preferredRawJson, String fallbackRawJson) {
        List<String> preferred = readTextList(preferredRawJson);
        return preferred.isEmpty() ? readTextList(fallbackRawJson) : preferred;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean shouldDisplayTaskErrorMessage(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return TaskStatusEnum.FAILED.name().equals(status) || TaskStatusEnum.CANCELLED.name().equals(status);
    }
}
