package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.*;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.mapper.*;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.TaskQueryService;
import com.quant.aiorchestrator.service.WorkflowConfigService;
import com.quant.aiorchestrator.util.CacheKeyUtil;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.MarketIntelligenceTypeEnum;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyConstants;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskQueryServiceImpl implements TaskQueryService {

    private record MarketIntelligenceFollowUpSummary(
            String followUpStatus,
            Integer followUpTaskCount,
            String latestFollowUpTaskId,
            String latestFollowUpTaskTitle,
            String latestFollowUpTaskStatus,
            LocalDateTime latestFollowUpCreatedAt
    ) {}

    private record RiskWarningFollowUpSummary(
            String followUpStatus,
            Integer followUpTaskCount,
            String latestFollowUpTaskId,
            String latestFollowUpTaskTitle,
            String latestFollowUpTaskStatus,
            LocalDateTime latestFollowUpCreatedAt
    ) {}

    private record StrategySignalFollowUpSummary(
            String followUpStatus,
            Integer followUpTaskCount,
            String latestFollowUpTaskId,
            String latestFollowUpTaskTitle,
            String latestFollowUpTaskStatus,
            LocalDateTime latestFollowUpCreatedAt
    ) {}

    private record RiskProjection(
            boolean needHumanReview,
            int warningCount,
            int riskPointCount,
            int totalRiskCount,
            RiskLevelEnum riskLevel
    ) {}

    private static final List<String> POSITIVE_SIGNAL_HINTS = List.of("增长", "改善", "提升", "利好", "看好", "强劲", "稳健", "修复", "机会", "受益", "positive", "upside", "beat");
    private static final List<String> NEGATIVE_SIGNAL_HINTS = List.of("风险", "下滑", "承压", "利空", "谨慎", "波动", "回落", "下行", "不确定", "亏损", "negative", "downside", "miss");
    private static final String BACKTEST_STATUS_NOT_READY = "NOT_READY";
    private static final String BACKTEST_SUMMARY_NOT_READY = "历史回测待接入";

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
    private final AgentConfigService agentConfigService;
    private final ConfigChangeAuditService configChangeAuditService;
    private final EventAutoTriggerConfigService eventAutoTriggerConfigService;
    private final MarketEventIngestHistoryService marketEventIngestHistoryService;
    private final EventSourceConfigService eventSourceConfigService;
    private final ModelStrategyConfigService modelStrategyConfigService;
    private final PromptTemplateConfigService promptTemplateConfigService;
    private final WorkflowConfigService workflowConfigService;
    private final RoleAccessConfigService roleAccessConfigService;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;
    private final ReportEvidenceRefMapper reportEvidenceRefMapper;
    private final HumanReviewRecordMapper humanReviewRecordMapper;
    private final ResearchReportSectionMapper researchReportSectionMapper;
    private final TaskStateManager taskStateManager;

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
        vo.setReport(getTaskReportOnly(taskId));
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

    @Override
    public RiskWarningPageVO pageRiskWarnings(RiskWarningPageQueryDTO queryDTO) {
        RiskWarningPageQueryDTO safeQuery = queryDTO == null ? new RiskWarningPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<RiskWarningListItemVO> matchedRecords = listRiskWarningRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        RiskWarningPageVO vo = new RiskWarningPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public RiskWarningStatsVO getRiskWarningStats() {
        List<RiskWarningListItemVO> records = listRiskWarningRecords(new RiskWarningPageQueryDTO());
        RiskWarningStatsVO vo = new RiskWarningStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setHighCount(records.stream().filter(item -> RiskLevelEnum.HIGH.name().equals(item.getRiskLevel())).count());
        vo.setMediumCount(records.stream().filter(item -> RiskLevelEnum.MEDIUM.name().equals(item.getRiskLevel())).count());
        vo.setLowCount(records.stream().filter(item -> RiskLevelEnum.LOW.name().equals(item.getRiskLevel())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReportReviewStatus())).count());
        vo.setHumanReviewCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedHumanReview())).count());
        return vo;
    }

    private List<RiskWarningListItemVO> listRiskWarningRecords(RiskWarningPageQueryDTO queryDTO) {
        List<RiskWarningDO> domainWarnings = loadActiveRiskWarnings();
        if (domainWarnings.isEmpty()) {
            return listRiskWarningRecordsFromReports(queryDTO, Collections.emptySet());
        }

        Set<String> coveredTaskIds = domainWarnings.stream()
                .map(RiskWarningDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        List<RiskWarningListItemVO> records = new ArrayList<>(listRiskWarningRecordsFromDomain(domainWarnings, queryDTO));
        records.addAll(listRiskWarningRecordsFromReports(queryDTO, coveredTaskIds));
        return sortRiskWarningRecords(records);
    }

    private List<RiskWarningDO> loadActiveRiskWarnings() {
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getDeleted, 0)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
        );
    }

    private List<RiskWarningListItemVO> listRiskWarningRecordsFromDomain(List<RiskWarningDO> warnings,
                                                                         RiskWarningPageQueryDTO queryDTO) {
        if (warnings.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = warnings.stream()
                .map(RiskWarningDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        Map<String, ResearchTaskDO> taskMap = loadTaskMap(taskIds);
        Map<String, ResearchReportDO> reportMap = loadReportMapByTaskIds(taskIds);

        List<ResearchTaskDO> followUpTasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "RISK_WARNING")
        );
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        Set<String> warningIds = warnings.stream()
                .map(RiskWarningDO::getWarningId)
                .filter(warningId -> warningId != null && !warningId.isBlank())
                .collect(Collectors.toSet());
        Map<String, List<RiskWarningDetailDO>> detailMap = warningIds.isEmpty()
                ? Collections.emptyMap()
                : riskWarningDetailMapper.selectList(
                        new LambdaQueryWrapper<RiskWarningDetailDO>()
                                .eq(RiskWarningDetailDO::getDeleted, 0)
                                .in(RiskWarningDetailDO::getWarningId, warningIds)
                                .orderByAsc(RiskWarningDetailDO::getId)
                ).stream().collect(Collectors.groupingBy(RiskWarningDetailDO::getWarningId));

        return warnings.stream()
                .map(warning -> {
                    ResearchTaskDO task = taskMap.get(warning.getTaskId());
                    ResearchReportDO report = reportMap.get(warning.getTaskId());
                    return toRiskWarningItem(
                            warning,
                            task,
                            report,
                            resolveRiskWarningFollowUpSummary(
                                    task,
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            detailMap.getOrDefault(warning.getWarningId(), List.of())
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> matchesRiskWarningQuery(item, queryDTO))
                .toList();
    }

    private RiskWarningListItemVO toRiskWarningItem(RiskWarningDO warning,
                                                    ResearchTaskDO task,
                                                    ResearchReportDO report,
                                                    RiskWarningFollowUpSummary followUpSummary,
                                                    List<RiskWarningDetailDO> details) {
        if (warning == null) {
            return null;
        }

        List<String> riskReasons = buildDomainRiskReasons(warning, details);
        boolean needHumanReview = isDomainRiskHumanReview(warning);
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(
                warning.getReviewStatus() == null && report != null ? report.getReviewStatus() : warning.getReviewStatus()
        );

        RiskWarningListItemVO vo = new RiskWarningListItemVO();
        vo.setTaskId(warning.getTaskId());
        vo.setTaskTitle(task == null ? warning.getWarningSummary() : task.getTaskTitle());
        vo.setTaskType(task == null ? null : task.getTaskType());
        vo.setTargetCode(warning.getEntityCode());
        vo.setTargetName(warning.getEntityName());
        vo.setPriority(task == null ? null : task.getPriority());
        vo.setTaskStatus(task == null ? null : task.getStatus());
        vo.setCurrentStage(task == null ? null : task.getCurrentStage());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportType(report == null ? null : report.getReportType());
        vo.setFinalStatus(report == null ? null : report.getFinalStatus());
        vo.setRiskLevel(resolveDomainRiskLevel(warning).name());
        vo.setWarningCount(1);
        vo.setRiskPointCount(details == null ? 0 : details.size());
        vo.setTotalRiskCount(1 + (details == null ? 0 : details.size()));
        vo.setNeedHumanReview(needHumanReview);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(warning.getReviewerId() == null && report != null ? report.getReviewedBy() : warning.getReviewerId());
        vo.setReportReviewedAt(warning.getReviewTime() == null && report != null ? report.getReviewedAt() : warning.getReviewTime());
        vo.setRevised(report != null && isReportRevised(report));
        vo.setSummaryRevised(report != null && isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && isRiskPointsRevised(report));
        if (followUpSummary != null) {
            vo.setFollowUpStatus(followUpSummary.followUpStatus());
            vo.setFollowUpTaskCount(followUpSummary.followUpTaskCount());
            vo.setLatestFollowUpTaskId(followUpSummary.latestFollowUpTaskId());
            vo.setLatestFollowUpTaskTitle(followUpSummary.latestFollowUpTaskTitle());
            vo.setLatestFollowUpTaskStatus(followUpSummary.latestFollowUpTaskStatus());
            vo.setLatestFollowUpCreatedAt(followUpSummary.latestFollowUpCreatedAt());
        }
        vo.setReviewComment(report == null ? null : report.getReviewComment());
        vo.setSummary(warning.getWarningSummary());
        vo.setRiskReasons(riskReasons);
        vo.setRiskSourceTags(buildDomainRiskSourceTags(warning, needHumanReview, reviewStatus));
        vo.setCreatedAt(warning.getCreatedAt());
        return vo;
    }

    private List<String> buildDomainRiskReasons(RiskWarningDO warning, List<RiskWarningDetailDO> details) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if (warning.getWarningReason() != null && !warning.getWarningReason().isBlank()) {
            for (String item : warning.getWarningReason().split("\\R")) {
                if (item != null && !item.isBlank()) {
                    reasons.add(item.trim());
                }
            }
        }
        if (warning.getWarningSummary() != null && !warning.getWarningSummary().isBlank()) {
            reasons.add(warning.getWarningSummary().trim());
        }
        if (details != null) {
            for (RiskWarningDetailDO detail : details) {
                if (detail.getDetailDesc() != null && !detail.getDetailDesc().isBlank()) {
                    reasons.add(detail.getDetailDesc().trim());
                } else if (detail.getIndicatorValue() != null && !detail.getIndicatorValue().isBlank()) {
                    reasons.add(detail.getIndicatorValue().trim());
                }
            }
        }
        return new ArrayList<>(reasons);
    }

    private boolean isDomainRiskHumanReview(RiskWarningDO warning) {
        if (warning == null) {
            return false;
        }
        if ("NEED_HUMAN_REVIEW".equalsIgnoreCase(warning.getSuggestAction())) {
            return true;
        }
        RiskLevelEnum riskLevel = RiskLevelEnum.from(warning.getWarningLevel());
        return riskLevel == RiskLevelEnum.HIGH
                && ReportReviewStatusEnum.PENDING.name().equalsIgnoreCase(warning.getReviewStatus());
    }

    private RiskLevelEnum resolveDomainRiskLevel(RiskWarningDO warning) {
        RiskLevelEnum resolved = warning == null ? null : RiskLevelEnum.from(warning.getWarningLevel());
        return resolved == null ? RiskLevelEnum.LOW : resolved;
    }

    private List<String> buildDomainRiskSourceTags(RiskWarningDO warning,
                                                   boolean needHumanReview,
                                                   ReportReviewStatusEnum reviewStatus) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("RISK_WARNING");
        if (warning.getWarningType() != null && !warning.getWarningType().isBlank()) {
            tags.add(warning.getWarningType().trim());
        }
        if (warning.getTriggerSource() != null && !warning.getTriggerSource().isBlank()) {
            tags.add(warning.getTriggerSource().trim());
        }
        if (warning.getTriggerEventId() != null && !warning.getTriggerEventId().isBlank()) {
            tags.add("EVENT_TRIGGERED");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private List<RiskWarningListItemVO> listRiskWarningRecordsFromReports(RiskWarningPageQueryDTO queryDTO,
                                                                          Set<String> excludedTaskIds) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );

        if (reports.isEmpty()) {
            return List.of();
        }

        if (excludedTaskIds != null && !excludedTaskIds.isEmpty()) {
            reports = reports.stream()
                    .filter(report -> report.getTaskId() == null || !excludedTaskIds.contains(report.getTaskId()))
                    .toList();
            if (reports.isEmpty()) {
                return List.of();
            }
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ResearchTaskDO> taskMap = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );

        List<ResearchTaskDO> followUpTasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "RISK_WARNING")
        );

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        return reports.stream()
                .map(report -> toRiskWarningItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        resolveRiskWarningFollowUpSummary(
                                taskMap.get(report.getTaskId()),
                                report,
                                followUpTaskMapBySourceTaskId,
                                followUpTaskMapBySourceReportId
                        )
                ))
                .filter(Objects::nonNull)
                .filter(item -> matchesRiskWarningQuery(item, queryDTO))
                .toList();
    }

    private List<RiskWarningListItemVO> sortRiskWarningRecords(List<RiskWarningListItemVO> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .sorted(Comparator
                        .comparingInt((RiskWarningListItemVO item) -> riskLevelRank(item.getRiskLevel()))
                        .reversed()
                        .thenComparing(
                                RiskWarningListItemVO::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                RiskWarningListItemVO::getTaskId,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .toList();
    }

    private RiskWarningListItemVO toRiskWarningItem(ResearchReportDO report,
                                                    ResearchTaskDO task,
                                                    RiskWarningFollowUpSummary followUpSummary) {
        if (report == null || task == null) {
            return null;
        }

        List<String> warningList = readTextList(report.getRiskWarnings());
        List<String> riskPointList = readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints());
        boolean needHumanReview = report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;

        if (!needHumanReview && warningList.isEmpty() && riskPointList.isEmpty()) {
            return null;
        }

        int totalRiskCount = warningList.size() + riskPointList.size();
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());
        RiskLevelEnum riskLevel = resolveRiskLevel(totalRiskCount, needHumanReview);

        RiskWarningListItemVO vo = new RiskWarningListItemVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setTaskStatus(task.getStatus());
        vo.setCurrentStage(task.getCurrentStage());
        vo.setReportId(report.getReportId());
        vo.setReportType(report.getReportType());
        vo.setFinalStatus(report.getFinalStatus());
        vo.setRiskLevel(riskLevel.name());
        vo.setWarningCount(warningList.size());
        vo.setRiskPointCount(riskPointList.size());
        vo.setTotalRiskCount(totalRiskCount);
        vo.setNeedHumanReview(needHumanReview);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(report.getReviewedBy());
        vo.setReportReviewedAt(report.getReviewedAt());
        vo.setRevised(isReportRevised(report));
        vo.setSummaryRevised(isSummaryRevised(report));
        vo.setHighlightsRevised(isHighlightsRevised(report));
        vo.setRiskPointsRevised(isRiskPointsRevised(report));
        if (followUpSummary != null) {
            vo.setFollowUpStatus(followUpSummary.followUpStatus());
            vo.setFollowUpTaskCount(followUpSummary.followUpTaskCount());
            vo.setLatestFollowUpTaskId(followUpSummary.latestFollowUpTaskId());
            vo.setLatestFollowUpTaskTitle(followUpSummary.latestFollowUpTaskTitle());
            vo.setLatestFollowUpTaskStatus(followUpSummary.latestFollowUpTaskStatus());
            vo.setLatestFollowUpCreatedAt(followUpSummary.latestFollowUpCreatedAt());
        }
        vo.setReviewComment(report.getReviewComment());
        vo.setSummary(resolveReportCenterSummary(report));
        vo.setRiskReasons(mergeRiskReasons(warningList, riskPointList));
        vo.setRiskSourceTags(buildRiskSourceTags(warningList, riskPointList, needHumanReview, reviewStatus));
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private boolean matchesRiskWarningQuery(RiskWarningListItemVO item, RiskWarningPageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !queryDTO.getTargetCode().equalsIgnoreCase(item.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        RiskLevelEnum riskLevel = RiskLevelEnum.from(queryDTO.getRiskLevel());
        if (riskLevel != null && !riskLevel.name().equals(item.getRiskLevel())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReportReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReportReviewStatus())) {
            return false;
        }
        if (queryDTO.getNeedHumanReview() != null && !queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview())) {
            return false;
        }
        return true;
    }

    private List<String> readTextList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {})
                    .stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> mergeRiskReasons(List<String> warningList, List<String> riskPointList) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        reasons.addAll(warningList);
        reasons.addAll(riskPointList);
        return new ArrayList<>(reasons);
    }

    private RiskWarningFollowUpSummary resolveRiskWarningFollowUpSummary(ResearchTaskDO sourceTask,
                                                                        ResearchReportDO sourceReport,
                                                                        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId,
                                                                        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId) {
        if (sourceTask == null && sourceReport == null) {
            return defaultRiskWarningFollowUpSummary();
        }

        LinkedHashMap<String, ResearchTaskDO> followUpTaskMap = new LinkedHashMap<>();
        if (sourceTask != null && sourceTask.getTaskId() != null) {
            followUpTaskMapBySourceTaskId.getOrDefault(sourceTask.getTaskId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }
        if (sourceReport != null && sourceReport.getReportId() != null) {
            followUpTaskMapBySourceReportId.getOrDefault(sourceReport.getReportId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }

        List<ResearchTaskDO> followUpTasks = new ArrayList<>(followUpTaskMap.values());
        if (followUpTasks.isEmpty()) {
            return defaultRiskWarningFollowUpSummary();
        }

        followUpTasks.sort(Comparator
                .comparing(ResearchTaskDO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ResearchTaskDO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        ResearchTaskDO latestTask = followUpTasks.get(0);
        String followUpStatus = resolveRiskWarningFollowUpStatus(followUpTasks);
        return new RiskWarningFollowUpSummary(
                followUpStatus,
                followUpTasks.size(),
                latestTask.getTaskId(),
                latestTask.getTaskTitle(),
                latestTask.getStatus(),
                latestTask.getCreatedAt()
        );
    }

    private RiskWarningFollowUpSummary defaultRiskWarningFollowUpSummary() {
        return new RiskWarningFollowUpSummary("NOT_TRACKED", 0, null, null, null, null);
    }

    private String resolveRiskWarningFollowUpStatus(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return "NOT_TRACKED";
        }

        boolean hasActiveTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.INIT
                        || status == TaskStatusEnum.DISPATCHED
                        || status == TaskStatusEnum.RUNNING);
        if (hasActiveTask) {
            return "TRACKING";
        }

        boolean hasSuccessTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.SUCCESS);
        if (hasSuccessTask) {
            return "COMPLETED";
        }

        boolean hasFailedTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.FAILED || status == TaskStatusEnum.CANCELLED);
        if (hasFailedTask) {
            return "FAILED";
        }

        return "TRACKING";
    }

    private ReportReviewStatusEnum resolveReviewStatus(String reviewStatus) {
        ReportReviewStatusEnum resolved = ReportReviewStatusEnum.from(reviewStatus);
        return resolved == null ? ReportReviewStatusEnum.PENDING : resolved;
    }

    private RiskLevelEnum resolveRiskLevel(int totalRiskCount, boolean needHumanReview) {
        if (needHumanReview || totalRiskCount >= 4) {
            return RiskLevelEnum.HIGH;
        }
        if (totalRiskCount >= 2) {
            return RiskLevelEnum.MEDIUM;
        }
        return RiskLevelEnum.LOW;
    }

    private int riskLevelRank(String riskLevel) {
        RiskLevelEnum resolved = RiskLevelEnum.from(riskLevel);
        if (resolved == null) {
            return 0;
        }
        return switch (resolved) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }

    private LocalDateTime firstNonNull(LocalDateTime left, LocalDateTime right) {
        return left != null ? left : right;
    }

    private Map<String, ResearchTaskDO> loadTaskMap(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
    }

    private Map<String, ResearchReportDO> loadReportMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .in(ResearchReportDO::getTaskId, taskIds)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        ).stream().collect(Collectors.toMap(
                ResearchReportDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
    }

    private Map<String, RiskWarningDO> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getDeleted, 0)
                        .in(RiskWarningDO::getTaskId, taskIds)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
        ).stream().collect(Collectors.toMap(
                RiskWarningDO::getTaskId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private Map<String, StrategySignalDO> loadLatestStrategySignalMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StrategySignalDO> signals = strategySignalMapper.selectList(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getDeleted, 0)
                        .in(StrategySignalDO::getTaskId, taskIds)
                        .orderByDesc(StrategySignalDO::getSignalDate, StrategySignalDO::getCreatedAt, StrategySignalDO::getId)
        );
        if (signals == null || signals.isEmpty()) {
            return Collections.emptyMap();
        }
        return signals.stream()
                .filter(item -> item.getTaskId() != null && !item.getTaskId().isBlank())
                .collect(Collectors.toMap(
                        StrategySignalDO::getTaskId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<String, List<RiskWarningDetailDO>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds) {
        if (warningIds == null || warningIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return riskWarningDetailMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDetailDO>()
                        .eq(RiskWarningDetailDO::getDeleted, 0)
                        .in(RiskWarningDetailDO::getWarningId, warningIds)
                        .orderByAsc(RiskWarningDetailDO::getId)
        ).stream().collect(Collectors.groupingBy(RiskWarningDetailDO::getWarningId, LinkedHashMap::new, Collectors.toList()));
    }

    private RiskProjection resolveRiskProjection(ResearchReportDO report,
                                                 RiskWarningDO warning,
                                                 List<RiskWarningDetailDO> details) {
        if (warning != null) {
            int warningCount = 1;
            int riskPointCount = details == null ? 0 : details.size();
            return new RiskProjection(
                    isDomainRiskHumanReview(warning),
                    warningCount,
                    riskPointCount,
                    warningCount + riskPointCount,
                    resolveDomainRiskLevel(warning)
            );
        }
        int warningCount = report == null ? 0 : readTextList(report.getRiskWarnings()).size();
        int riskPointCount = report == null ? 0 : readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints()).size();
        boolean needHumanReview = report != null && report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;
        int totalRiskCount = warningCount + riskPointCount;
        return new RiskProjection(
                needHumanReview,
                warningCount,
                riskPointCount,
                totalRiskCount,
                totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null
        );
    }

    private RiskProjection resolveRiskProjection(ResearchReportDO report, RiskWarningDO warning) {
        return resolveRiskProjection(report, warning, List.of());
    }

    @Override
    public StrategySignalPageVO pageStrategySignals(StrategySignalPageQueryDTO queryDTO) {
        StrategySignalPageQueryDTO safeQuery = queryDTO == null ? new StrategySignalPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<StrategySignalListItemVO> matchedRecords = listStrategySignalRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        StrategySignalPageVO vo = new StrategySignalPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public StrategySignalStatsVO getStrategySignalStats() {
        List<StrategySignalListItemVO> records = listStrategySignalRecords(new StrategySignalPageQueryDTO());
        StrategySignalStatsVO vo = new StrategySignalStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setPositiveCount(records.stream().filter(item -> SignalDirectionEnum.POSITIVE.name().equals(item.getSignalDirection())).count());
        vo.setNeutralCount(records.stream().filter(item -> SignalDirectionEnum.NEUTRAL.name().equals(item.getSignalDirection())).count());
        vo.setNegativeCount(records.stream().filter(item -> SignalDirectionEnum.NEGATIVE.name().equals(item.getSignalDirection())).count());
        vo.setHighConfidenceCount(records.stream().filter(item -> isHighConfidence(item.getConfidenceScore())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReportReviewStatus())).count());
        return vo;
    }

    private List<StrategySignalListItemVO> listStrategySignalRecords(StrategySignalPageQueryDTO queryDTO) {
        List<StrategySignalDO> domainSignals = loadActiveStrategySignals();
        if (domainSignals.isEmpty()) {
            return listStrategySignalRecordsFromReports(queryDTO, Collections.emptySet());
        }

        Set<String> coveredTaskIds = domainSignals.stream()
                .map(StrategySignalDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        List<StrategySignalListItemVO> records = new ArrayList<>(listStrategySignalRecordsFromDomain(domainSignals, queryDTO));
        records.addAll(listStrategySignalRecordsFromReports(queryDTO, coveredTaskIds));
        return sortStrategySignalRecords(records);
    }

    private List<StrategySignalDO> loadActiveStrategySignals() {
        return strategySignalMapper.selectList(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getDeleted, 0)
                        .orderByDesc(StrategySignalDO::getSignalDate, StrategySignalDO::getCreatedAt, StrategySignalDO::getId)
        );
    }

    private List<StrategySignalListItemVO> listStrategySignalRecordsFromDomain(List<StrategySignalDO> signals,
                                                                               StrategySignalPageQueryDTO queryDTO) {
        if (signals.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = signals.stream()
                .map(StrategySignalDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        Map<String, ResearchTaskDO> taskMap = loadTaskMap(taskIds);
        Map<String, ResearchReportDO> reportMap = loadReportMapByTaskIds(taskIds);
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );

        List<ResearchTaskDO> followUpTasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "STRATEGY_SIGNAL")
        );
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));
        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        Set<String> signalIds = signals.stream()
                .map(StrategySignalDO::getSignalId)
                .filter(signalId -> signalId != null && !signalId.isBlank())
                .collect(Collectors.toSet());
        Map<String, List<StrategySignalFactorDO>> factorMap = signalIds.isEmpty()
                ? Collections.emptyMap()
                : strategySignalFactorMapper.selectList(
                        new LambdaQueryWrapper<StrategySignalFactorDO>()
                                .eq(StrategySignalFactorDO::getDeleted, 0)
                                .in(StrategySignalFactorDO::getSignalId, signalIds)
                                .orderByAsc(StrategySignalFactorDO::getId)
                ).stream().collect(Collectors.groupingBy(StrategySignalFactorDO::getSignalId));

        return signals.stream()
                .map(signal -> {
                    ResearchTaskDO task = taskMap.get(signal.getTaskId());
                    ResearchReportDO report = reportMap.get(signal.getTaskId());
                    RiskWarningDO warning = riskWarningMap.get(signal.getTaskId());
                    return toStrategySignalItem(
                            signal,
                            task,
                            report,
                            resolveStrategySignalFollowUpSummary(
                                    task,
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            factorMap.getOrDefault(signal.getSignalId(), List.of()),
                            warning,
                            warning == null ? List.of() : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of())
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> matchesStrategySignalQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(StrategySignalListItemVO::getSignalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private StrategySignalListItemVO toStrategySignalItem(StrategySignalDO signal,
                                                          ResearchTaskDO task,
                                                          ResearchReportDO report,
                                                          StrategySignalFollowUpSummary followUpSummary,
                                                          List<StrategySignalFactorDO> factors,
                                                          RiskWarningDO warning,
                                                          List<RiskWarningDetailDO> warningDetails) {
        if (signal == null) {
            return null;
        }

        Double confidenceScore = signal.getConfidenceScore() == null ? null : signal.getConfidenceScore().doubleValue();
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report == null ? null : report.getReviewStatus());
        boolean needHumanReview = resolveRiskProjection(report, warning, warningDetails).needHumanReview();

        StrategySignalListItemVO vo = new StrategySignalListItemVO();
        vo.setSignalId(signal.getSignalId());
        vo.setTaskId(signal.getTaskId());
        vo.setTaskTitle(task == null ? signal.getReasonSummary() : task.getTaskTitle());
        vo.setTaskType(task == null ? null : task.getTaskType());
        vo.setTargetCode(signal.getEntityCode());
        vo.setTargetName(signal.getEntityName());
        vo.setPriority(task == null ? null : task.getPriority());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportType(report == null ? signal.getSignalType() : report.getReportType());
        vo.setFinalStatus(report == null ? null : report.getFinalStatus());
        vo.setSignalDirection(resolveDomainSignalDirection(signal).name());
        vo.setSignalStrength(resolveDomainSignalStrength(signal).name());
        vo.setSignalScore(signal.getSignalScore());
        vo.setConfidenceScore(confidenceScore);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(report == null ? null : report.getReviewedBy());
        vo.setReportReviewedAt(report == null ? null : report.getReviewedAt());
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewComment(report == null ? null : report.getReviewComment());
        vo.setRevised(report != null && isReportRevised(report));
        vo.setSummaryRevised(report != null && isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && isRiskPointsRevised(report));
        if (followUpSummary != null) {
            vo.setFollowUpStatus(followUpSummary.followUpStatus());
            vo.setFollowUpTaskCount(followUpSummary.followUpTaskCount());
            vo.setLatestFollowUpTaskId(followUpSummary.latestFollowUpTaskId());
            vo.setLatestFollowUpTaskTitle(followUpSummary.latestFollowUpTaskTitle());
            vo.setLatestFollowUpTaskStatus(followUpSummary.latestFollowUpTaskStatus());
            vo.setLatestFollowUpCreatedAt(followUpSummary.latestFollowUpCreatedAt());
        }
        vo.setStrategySummary(signal.getReasonSummary());
        vo.setSignalSources(buildDomainSignalSources(signal, factors));
        vo.setSignalSourceTags(buildDomainSignalSourceTags(signal, factors, needHumanReview, reviewStatus, confidenceScore));
        vo.setBacktestStatus(BACKTEST_STATUS_NOT_READY);
        vo.setBacktestSummary(BACKTEST_SUMMARY_NOT_READY);
        vo.setCreatedAt(signal.getCreatedAt());
        return vo;
    }

    private SignalDirectionEnum resolveDomainSignalDirection(StrategySignalDO signal) {
        SignalDirectionEnum resolved = signal == null ? null : SignalDirectionEnum.from(signal.getSignalDirection());
        if (resolved != null) {
            return resolved;
        }
        Double confidenceScore = signal == null || signal.getConfidenceScore() == null
                ? null
                : signal.getConfidenceScore().doubleValue();
        return resolveSignalDirection(signal == null ? null : signal.getReasonSummary(), 0, false, confidenceScore);
    }

    private SignalStrengthEnum resolveDomainSignalStrength(StrategySignalDO signal) {
        SignalStrengthEnum resolved = signal == null ? null : SignalStrengthEnum.from(signal.getSignalLevel());
        if (resolved != null) {
            return resolved;
        }
        if (signal != null && signal.getSignalScore() != null) {
            return resolveSignalStrength(signal.getSignalScore());
        }
        Double confidenceScore = signal == null || signal.getConfidenceScore() == null
                ? null
                : signal.getConfidenceScore().doubleValue();
        int fallbackScore = confidenceScore == null ? 60 : (int) Math.round(Math.max(0D, Math.min(1D, confidenceScore)) * 100D);
        return resolveSignalStrength(fallbackScore);
    }

    private List<String> buildDomainSignalSources(StrategySignalDO signal, List<StrategySignalFactorDO> factors) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (signal != null && signal.getReasonSummary() != null && !signal.getReasonSummary().isBlank()) {
            sources.add(signal.getReasonSummary().trim());
        }
        if (factors != null) {
            for (StrategySignalFactorDO factor : factors) {
                String conclusion = normalizeText(factor.getFactorConclusion());
                if (conclusion != null) {
                    sources.add(conclusion);
                    continue;
                }
                String factorName = normalizeText(factor.getFactorName());
                String factorValue = normalizeText(factor.getFactorValue());
                if (factorName != null && factorValue != null) {
                    sources.add(factorName + ": " + factorValue);
                } else if (factorName != null) {
                    sources.add(factorName);
                } else if (factorValue != null) {
                    sources.add(factorValue);
                }
            }
        }
        return new ArrayList<>(sources);
    }

    private List<String> buildDomainSignalSourceTags(StrategySignalDO signal,
                                                     List<StrategySignalFactorDO> factors,
                                                     boolean needHumanReview,
                                                     ReportReviewStatusEnum reviewStatus,
                                                     Double confidenceScore) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("STRATEGY_SIGNAL");
        if (signal != null && signal.getSignalType() != null && !signal.getSignalType().isBlank()) {
            tags.add(signal.getSignalType().trim());
        }
        if (signal != null && signal.getSourceEventId() != null && !signal.getSourceEventId().isBlank()) {
            tags.add("EVENT_TRIGGERED");
        }
        if (factors != null && !factors.isEmpty()) {
            tags.add("FACTOR_EXPLAINED");
            boolean hasRiskFactor = factors.stream()
                    .map(StrategySignalFactorDO::getFactorCode)
                    .filter(Objects::nonNull)
                    .anyMatch(code -> "RISK_COUNT".equalsIgnoreCase(code) || "HUMAN_REVIEW".equalsIgnoreCase(code));
            if (hasRiskFactor) {
                tags.add("RISK_ADJUSTED");
            }
        }
        if (isHighConfidence(confidenceScore)) {
            tags.add("HIGH_CONFIDENCE");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private List<StrategySignalListItemVO> listStrategySignalRecordsFromReports(StrategySignalPageQueryDTO queryDTO,
                                                                               Set<String> excludedTaskIds) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );

        if (reports.isEmpty()) {
            return List.of();
        }

        if (excludedTaskIds != null && !excludedTaskIds.isEmpty()) {
            reports = reports.stream()
                    .filter(report -> report.getTaskId() == null || !excludedTaskIds.contains(report.getTaskId()))
                    .toList();
            if (reports.isEmpty()) {
                return List.of();
            }
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ResearchTaskDO> taskMap = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );

        List<ResearchTaskDO> followUpTasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "STRATEGY_SIGNAL")
        );

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        return reports.stream()
                .map(report -> {
                    RiskWarningDO warning = riskWarningMap.get(report.getTaskId());
                    return toStrategySignalItem(
                            report,
                            taskMap.get(report.getTaskId()),
                            resolveStrategySignalFollowUpSummary(
                                    taskMap.get(report.getTaskId()),
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            warning,
                            warning == null ? List.of() : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of())
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> matchesStrategySignalQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(StrategySignalListItemVO::getSignalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private List<StrategySignalListItemVO> sortStrategySignalRecords(List<StrategySignalListItemVO> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .sorted(Comparator
                        .comparing(StrategySignalListItemVO::getSignalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StrategySignalListItemVO::getTaskId, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private StrategySignalListItemVO toStrategySignalItem(ResearchReportDO report,
                                                          ResearchTaskDO task,
                                                          StrategySignalFollowUpSummary followUpSummary,
                                                          RiskWarningDO warning,
                                                          List<RiskWarningDetailDO> warningDetails) {
        if (report == null || task == null) {
            return null;
        }

        String strategySummary = resolveStrategySummary(report);
        List<String> signalSources = resolveSignalSources(report, strategySummary);
        Double confidenceScore = report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();

        if ((strategySummary == null || strategySummary.isBlank()) && signalSources.isEmpty() && confidenceScore == null) {
            return null;
        }

        RiskProjection riskProjection = resolveRiskProjection(report, warning, warningDetails);
        int totalRiskCount = riskProjection.totalRiskCount();
        boolean needHumanReview = riskProjection.needHumanReview();
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());
        SignalDirectionEnum signalDirection = resolveSignalDirection(strategySummary, totalRiskCount, needHumanReview, confidenceScore);
        int signalScore = calculateSignalScore(confidenceScore, totalRiskCount, needHumanReview, reviewStatus, signalDirection);
        SignalStrengthEnum signalStrength = resolveSignalStrength(signalScore);

        StrategySignalListItemVO vo = new StrategySignalListItemVO();
        vo.setSignalId(null);
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setReportId(report.getReportId());
        vo.setReportType(report.getReportType());
        vo.setFinalStatus(report.getFinalStatus());
        vo.setSignalDirection(signalDirection.name());
        vo.setSignalStrength(signalStrength.name());
        vo.setSignalScore(signalScore);
        vo.setConfidenceScore(confidenceScore);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(report.getReviewedBy());
        vo.setReportReviewedAt(report.getReviewedAt());
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewComment(report.getReviewComment());
        vo.setRevised(isReportRevised(report));
        vo.setSummaryRevised(isSummaryRevised(report));
        vo.setHighlightsRevised(isHighlightsRevised(report));
        vo.setRiskPointsRevised(isRiskPointsRevised(report));
        if (followUpSummary != null) {
            vo.setFollowUpStatus(followUpSummary.followUpStatus());
            vo.setFollowUpTaskCount(followUpSummary.followUpTaskCount());
            vo.setLatestFollowUpTaskId(followUpSummary.latestFollowUpTaskId());
            vo.setLatestFollowUpTaskTitle(followUpSummary.latestFollowUpTaskTitle());
            vo.setLatestFollowUpTaskStatus(followUpSummary.latestFollowUpTaskStatus());
            vo.setLatestFollowUpCreatedAt(followUpSummary.latestFollowUpCreatedAt());
        }
        vo.setStrategySummary(strategySummary);
        vo.setSignalSources(signalSources);
        vo.setSignalSourceTags(buildSignalSourceTags(report, signalSources, totalRiskCount, needHumanReview, reviewStatus, confidenceScore));
        vo.setBacktestStatus(BACKTEST_STATUS_NOT_READY);
        vo.setBacktestSummary(BACKTEST_SUMMARY_NOT_READY);
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private boolean matchesStrategySignalQuery(StrategySignalListItemVO item, StrategySignalPageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !queryDTO.getTargetCode().equalsIgnoreCase(item.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        SignalDirectionEnum signalDirection = SignalDirectionEnum.from(queryDTO.getSignalDirection());
        if (signalDirection != null && !signalDirection.name().equals(item.getSignalDirection())) {
            return false;
        }
        SignalStrengthEnum signalStrength = SignalStrengthEnum.from(queryDTO.getSignalStrength());
        if (signalStrength != null && !signalStrength.name().equals(item.getSignalStrength())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReportReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReportReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyHighConfidence()) && !isHighConfidence(item.getConfidenceScore())) {
            return false;
        }
        return true;
    }

    private String resolveStrategySummary(ResearchReportDO report) {
        if (report.getRevisedSummary() != null && !report.getRevisedSummary().isBlank()) {
            return report.getRevisedSummary().trim();
        }
        if (report.getSummary() != null && !report.getSummary().isBlank()) {
            return report.getSummary().trim();
        }
        return null;
    }

    private List<String> resolveSignalSources(ResearchReportDO report, String strategySummary) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        sources.addAll(readTextList(report.getRevisedHighlights()));
        sources.addAll(readTextList(report.getHighlights()));
        if (sources.isEmpty() && strategySummary != null && !strategySummary.isBlank()) {
            sources.add(strategySummary);
        }
        return new ArrayList<>(sources);
    }

    private List<String> buildSignalSourceTags(ResearchReportDO report,
                                               List<String> signalSources,
                                               int totalRiskCount,
                                               boolean needHumanReview,
                                               ReportReviewStatusEnum reviewStatus,
                                               Double confidenceScore) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (!readTextList(report.getRevisedHighlights()).isEmpty() || !readTextList(report.getHighlights()).isEmpty()) {
            tags.add("REPORT_HIGHLIGHT");
        } else if (signalSources != null && !signalSources.isEmpty()) {
            tags.add("SUMMARY_INFERENCE");
        }
        if (isHighConfidence(confidenceScore)) {
            tags.add("HIGH_CONFIDENCE");
        }
        if (totalRiskCount > 0) {
            tags.add("RISK_ADJUSTED");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private StrategySignalFollowUpSummary resolveStrategySignalFollowUpSummary(ResearchTaskDO sourceTask,
                                                                              ResearchReportDO sourceReport,
                                                                              Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId,
                                                                              Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId) {
        if (sourceTask == null && sourceReport == null) {
            return defaultStrategySignalFollowUpSummary();
        }

        LinkedHashMap<String, ResearchTaskDO> followUpTaskMap = new LinkedHashMap<>();
        if (sourceTask != null && sourceTask.getTaskId() != null) {
            followUpTaskMapBySourceTaskId.getOrDefault(sourceTask.getTaskId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }
        if (sourceReport != null && sourceReport.getReportId() != null) {
            followUpTaskMapBySourceReportId.getOrDefault(sourceReport.getReportId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }

        List<ResearchTaskDO> followUpTasks = new ArrayList<>(followUpTaskMap.values());
        if (followUpTasks.isEmpty()) {
            return defaultStrategySignalFollowUpSummary();
        }

        followUpTasks.sort(Comparator
                .comparing(ResearchTaskDO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ResearchTaskDO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        ResearchTaskDO latestTask = followUpTasks.get(0);
        String followUpStatus = resolveStrategySignalFollowUpStatus(followUpTasks);
        return new StrategySignalFollowUpSummary(
                followUpStatus,
                followUpTasks.size(),
                latestTask.getTaskId(),
                latestTask.getTaskTitle(),
                latestTask.getStatus(),
                latestTask.getCreatedAt()
        );
    }

    private StrategySignalFollowUpSummary defaultStrategySignalFollowUpSummary() {
        return new StrategySignalFollowUpSummary("NOT_TRACKED", 0, null, null, null, null);
    }

    private String resolveStrategySignalFollowUpStatus(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return "NOT_TRACKED";
        }

        boolean hasActiveTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.INIT
                        || status == TaskStatusEnum.DISPATCHED
                        || status == TaskStatusEnum.RUNNING);
        if (hasActiveTask) {
            return "TRACKING";
        }

        boolean hasSuccessTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.SUCCESS);
        if (hasSuccessTask) {
            return "COMPLETED";
        }

        boolean hasFailedTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.FAILED || status == TaskStatusEnum.CANCELLED);
        if (hasFailedTask) {
            return "FAILED";
        }

        return "TRACKING";
    }

    private SignalDirectionEnum resolveSignalDirection(String strategySummary,
                                                       int totalRiskCount,
                                                       boolean needHumanReview,
                                                       Double confidenceScore) {
        String normalizedSummary = strategySummary == null ? "" : strategySummary.toLowerCase();
        int positiveHit = countKeywords(normalizedSummary, POSITIVE_SIGNAL_HINTS);
        int negativeHit = countKeywords(normalizedSummary, NEGATIVE_SIGNAL_HINTS) + totalRiskCount + (needHumanReview ? 1 : 0);

        if (negativeHit >= positiveHit + 2) {
            return SignalDirectionEnum.NEGATIVE;
        }
        if (positiveHit >= negativeHit + 2 && !needHumanReview && totalRiskCount <= 1 && isHighConfidence(confidenceScore)) {
            return SignalDirectionEnum.POSITIVE;
        }
        if (needHumanReview || totalRiskCount >= 3) {
            return SignalDirectionEnum.NEGATIVE;
        }
        if (isHighConfidence(confidenceScore) && totalRiskCount == 0) {
            return SignalDirectionEnum.POSITIVE;
        }
        return SignalDirectionEnum.NEUTRAL;
    }

    private SignalStrengthEnum resolveSignalStrength(int signalScore) {
        if (signalScore >= 80) {
            return SignalStrengthEnum.STRONG;
        }
        if (signalScore >= 60) {
            return SignalStrengthEnum.MEDIUM;
        }
        return SignalStrengthEnum.WEAK;
    }

    private int calculateSignalScore(Double confidenceScore,
                                     int totalRiskCount,
                                     boolean needHumanReview,
                                     ReportReviewStatusEnum reviewStatus,
                                     SignalDirectionEnum signalDirection) {
        int score = confidenceScore == null ? 60 : (int) Math.round(Math.max(0D, Math.min(1D, confidenceScore)) * 100D);
        score -= totalRiskCount * 8;
        if (needHumanReview) {
            score -= 12;
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            score -= 10;
        }
        if (signalDirection == SignalDirectionEnum.POSITIVE) {
            score += 5;
        }
        if (signalDirection == SignalDirectionEnum.NEGATIVE) {
            score -= 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private boolean isHighConfidence(Double confidenceScore) {
        return confidenceScore != null && confidenceScore >= 0.8D;
    }

    private int countKeywords(String content, List<String> keywords) {
        if (content == null || content.isBlank() || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && content.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public ReportCenterPageVO pageReportCenter(ReportCenterPageQueryDTO queryDTO) {
        ReportCenterPageQueryDTO safeQuery = queryDTO == null ? new ReportCenterPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<ReportCenterListItemVO> matchedRecords = listReportCenterRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        ReportCenterPageVO vo = new ReportCenterPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public ReportCenterStatsVO getReportCenterStats() {
        List<ReportCenterListItemVO> records = listReportCenterRecords(new ReportCenterPageQueryDTO());
        ReportCenterStatsVO vo = new ReportCenterStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setHighConfidenceCount(records.stream().filter(item -> isHighConfidence(item.getConfidenceScore())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        vo.setApprovedCount(records.stream().filter(item -> ReportReviewStatusEnum.APPROVED.name().equals(item.getReviewStatus())).count());
        vo.setHumanReviewCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedHumanReview())).count());
        return vo;
    }

    private List<ReportCenterListItemVO> listReportCenterRecords(ReportCenterPageQueryDTO queryDTO) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );

        if (reports.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ResearchTaskDO> taskMap = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);

        return reports.stream()
                .map(report -> toReportCenterItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        riskWarningMap.get(report.getTaskId())
                ))
                .filter(Objects::nonNull)
                .filter(item -> matchesReportCenterQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(ReportCenterListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ReportCenterListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private ReportCenterListItemVO toReportCenterItem(ResearchReportDO report,
                                                      ResearchTaskDO task,
                                                      RiskWarningDO warning) {
        if (report == null || task == null) {
            return null;
        }

        String summary = resolveReportCenterSummary(report);
        String reportType = resolveReportType(report, task);

        if ((summary == null || summary.isBlank())
                && (report.getResultRef() == null || report.getResultRef().isBlank())
                && (reportType == null || reportType.isBlank())) {
            return null;
        }

        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());
        boolean needHumanReview = resolveRiskProjection(report, warning).needHumanReview();

        ReportCenterListItemVO vo = new ReportCenterListItemVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setReportId(report.getReportId());
        vo.setReportType(reportType);
        vo.setFinalStatus(report.getFinalStatus());
        vo.setConfidenceScore(report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue());
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setRevised(isReportRevised(report));
        vo.setSummaryRevised(isSummaryRevised(report));
        vo.setHighlightsRevised(isHighlightsRevised(report));
        vo.setRiskPointsRevised(isRiskPointsRevised(report));
        vo.setSummary(summary);
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private boolean matchesReportCenterQuery(ReportCenterListItemVO item, ReportCenterPageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !queryDTO.getTargetCode().equalsIgnoreCase(item.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        if (queryDTO.getReportType() != null && !queryDTO.getReportType().isBlank()
                && !queryDTO.getReportType().equalsIgnoreCase(item.getReportType())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyHighConfidence()) && !isHighConfidence(item.getConfidenceScore())) {
            return false;
        }
        if (queryDTO.getNeedHumanReview() != null
                && !queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview())) {
            return false;
        }
        return true;
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
        return !Objects.equals(
                normalizeText(report.getSummary()),
                resolveDisplaySummary(report.getRevisedSummary(), report.getSummary())
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

    private List<String> buildRiskSourceTags(List<String> warningList,
                                             List<String> riskPointList,
                                             boolean needHumanReview,
                                             ReportReviewStatusEnum reviewStatus) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (warningList != null && !warningList.isEmpty()) {
            tags.add("WARNING_SIGNAL");
        }
        if (riskPointList != null && !riskPointList.isEmpty()) {
            tags.add("REPORT_RISK_POINT");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private String resolveReportType(ResearchReportDO report, ResearchTaskDO task) {
        if (report.getReportType() != null && !report.getReportType().isBlank()) {
            return report.getReportType().trim();
        }
        return task == null ? null : task.getTaskType();
    }

    @Override
    public MarketIntelligencePageVO pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO) {
        MarketIntelligencePageQueryDTO safeQuery = queryDTO == null ? new MarketIntelligencePageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<MarketIntelligenceListItemVO> matchedRecords = listMarketIntelligenceRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        MarketIntelligencePageVO vo = new MarketIntelligencePageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public MarketIntelligenceStatsVO getMarketIntelligenceStats() {
        List<MarketIntelligenceListItemVO> records = listMarketIntelligenceRecords(new MarketIntelligencePageQueryDTO());
        MarketIntelligenceStatsVO vo = new MarketIntelligenceStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setRiskAlertCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.RISK_ALERT.name().equals(item.getIntelligenceType())).count());
        vo.setStrategySignalCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.STRATEGY_SIGNAL.name().equals(item.getIntelligenceType())).count());
        vo.setReportInsightCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.REPORT_INSIGHT.name().equals(item.getIntelligenceType())).count());
        vo.setHighPriorityCount(records.stream().filter(item -> "HIGH".equalsIgnoreCase(item.getPriority())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        return vo;
    }

    private List<MarketIntelligenceListItemVO> listMarketIntelligenceRecords(MarketIntelligencePageQueryDTO queryDTO) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );

        if (reports.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ResearchTaskDO> taskMap = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );
        Map<String, StrategySignalDO> strategySignalMap = loadLatestStrategySignalMapByTaskIds(taskIds);

        List<ResearchTaskDO> followUpTasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "MARKET_INTELLIGENCE")
        );

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        return reports.stream()
                .map(report -> {
                    RiskWarningDO warning = riskWarningMap.get(report.getTaskId());
                    StrategySignalDO signal = strategySignalMap.get(report.getTaskId());
                    return toMarketIntelligenceItem(
                            report,
                            taskMap.get(report.getTaskId()),
                            resolveMarketIntelligenceFollowUpSummary(
                                    taskMap.get(report.getTaskId()),
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            warning,
                            warning == null ? List.of() : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of()),
                            signal
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> matchesMarketIntelligenceQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(MarketIntelligenceListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketIntelligenceListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private MarketIntelligenceListItemVO toMarketIntelligenceItem(ResearchReportDO report,
                                                                  ResearchTaskDO task,
                                                                  MarketIntelligenceFollowUpSummary followUpSummary,
                                                                  RiskWarningDO warning,
                                                                  List<RiskWarningDetailDO> warningDetails,
                                                                  StrategySignalDO strategySignal) {
        if (report == null || task == null) {
            return null;
        }

        String summary = resolveReportCenterSummary(report);
        String reportType = resolveReportType(report, task);
        RiskProjection riskProjection = resolveRiskProjection(report, warning, warningDetails);
        int totalRiskCount = riskProjection.totalRiskCount();
        boolean needHumanReview = riskProjection.needHumanReview();
        Double reportConfidenceScore = report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();
        Double signalConfidenceScore = strategySignal == null || strategySignal.getConfidenceScore() == null
                ? null
                : strategySignal.getConfidenceScore().doubleValue();
        Double confidenceScore = signalConfidenceScore == null ? reportConfidenceScore : signalConfidenceScore;
        SignalDirectionEnum signalDirection = strategySignal == null
                ? resolveSignalDirection(summary, totalRiskCount, needHumanReview, confidenceScore)
                : resolveDomainSignalDirection(strategySignal);
        RiskLevelEnum riskLevel = riskProjection.riskLevel();
        MarketIntelligenceTypeEnum intelligenceType = resolveMarketIntelligenceType(
                totalRiskCount,
                needHumanReview,
                confidenceScore,
                signalDirection,
                strategySignal != null
        );

        if ((summary == null || summary.isBlank())
                && (report.getResultRef() == null || report.getResultRef().isBlank())
                && (reportType == null || reportType.isBlank())) {
            return null;
        }

        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());

        MarketIntelligenceListItemVO vo = new MarketIntelligenceListItemVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setSourceChannel(task.getSourceChannel());
        vo.setIntelligenceType(intelligenceType.name());
        vo.setReportId(report.getReportId());
        vo.setReportType(reportType);
        vo.setFinalStatus(report.getFinalStatus());
        vo.setConfidenceScore(confidenceScore);
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setReviewComment(report.getReviewComment());
        vo.setRevised(isReportRevised(report));
        vo.setSummaryRevised(isSummaryRevised(report));
        vo.setHighlightsRevised(isHighlightsRevised(report));
        vo.setRiskPointsRevised(isRiskPointsRevised(report));
        if (followUpSummary != null) {
            vo.setFollowUpStatus(followUpSummary.followUpStatus());
            vo.setFollowUpTaskCount(followUpSummary.followUpTaskCount());
            vo.setLatestFollowUpTaskId(followUpSummary.latestFollowUpTaskId());
            vo.setLatestFollowUpTaskTitle(followUpSummary.latestFollowUpTaskTitle());
            vo.setLatestFollowUpTaskStatus(followUpSummary.latestFollowUpTaskStatus());
            vo.setLatestFollowUpCreatedAt(followUpSummary.latestFollowUpCreatedAt());
        }
        vo.setSignalDirection(signalDirection.name());
        vo.setRiskLevel(riskLevel == null ? null : riskLevel.name());
        vo.setIntelligenceSourceTags(buildMarketIntelligenceSourceTags(
                task,
                intelligenceType,
                summary,
                totalRiskCount,
                needHumanReview,
                reviewStatus,
                confidenceScore,
                signalDirection,
                strategySignal != null
        ));
        vo.setSummary(summary);
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private boolean matchesMarketIntelligenceQuery(MarketIntelligenceListItemVO item, MarketIntelligencePageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !containsIgnoreCase(item.getTargetCode(), queryDTO.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        MarketIntelligenceTypeEnum intelligenceType = MarketIntelligenceTypeEnum.from(queryDTO.getIntelligenceType());
        if (intelligenceType != null && !intelligenceType.name().equals(item.getIntelligenceType())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyHighPriority()) && !"HIGH".equalsIgnoreCase(item.getPriority())) {
            return false;
        }
        if (queryDTO.getNeedHumanReview() != null && !queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview())) {
            return false;
        }
        return true;
    }

    private MarketIntelligenceTypeEnum resolveMarketIntelligenceType(int totalRiskCount,
                                                                     boolean needHumanReview,
                                                                     Double confidenceScore,
                                                                     SignalDirectionEnum signalDirection,
                                                                     boolean hasDomainStrategySignal) {
        if (needHumanReview || totalRiskCount > 0) {
            return MarketIntelligenceTypeEnum.RISK_ALERT;
        }
        if (hasDomainStrategySignal) {
            return MarketIntelligenceTypeEnum.STRATEGY_SIGNAL;
        }
        if (signalDirection == SignalDirectionEnum.POSITIVE
                || signalDirection == SignalDirectionEnum.NEGATIVE
                || isHighConfidence(confidenceScore)) {
            return MarketIntelligenceTypeEnum.STRATEGY_SIGNAL;
        }
        return MarketIntelligenceTypeEnum.REPORT_INSIGHT;
    }

    private MarketIntelligenceFollowUpSummary resolveMarketIntelligenceFollowUpSummary(ResearchTaskDO sourceTask,
                                                                                       ResearchReportDO sourceReport,
                                                                                       Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId,
                                                                                       Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId) {
        if (sourceTask == null && sourceReport == null) {
            return defaultMarketIntelligenceFollowUpSummary();
        }

        LinkedHashMap<String, ResearchTaskDO> followUpTaskMap = new LinkedHashMap<>();
        if (sourceTask != null && sourceTask.getTaskId() != null) {
            followUpTaskMapBySourceTaskId.getOrDefault(sourceTask.getTaskId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }
        if (sourceReport != null && sourceReport.getReportId() != null) {
            followUpTaskMapBySourceReportId.getOrDefault(sourceReport.getReportId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }

        List<ResearchTaskDO> followUpTasks = new ArrayList<>(followUpTaskMap.values());
        if (followUpTasks.isEmpty()) {
            return defaultMarketIntelligenceFollowUpSummary();
        }

        followUpTasks.sort(Comparator
                .comparing(ResearchTaskDO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ResearchTaskDO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        ResearchTaskDO latestTask = followUpTasks.get(0);
        String followUpStatus = resolveMarketIntelligenceFollowUpStatus(followUpTasks);
        return new MarketIntelligenceFollowUpSummary(
                followUpStatus,
                followUpTasks.size(),
                latestTask.getTaskId(),
                latestTask.getTaskTitle(),
                latestTask.getStatus(),
                latestTask.getCreatedAt()
        );
    }

    private MarketIntelligenceFollowUpSummary defaultMarketIntelligenceFollowUpSummary() {
        return new MarketIntelligenceFollowUpSummary("NOT_TRACKED", 0, null, null, null, null);
    }

    private String resolveMarketIntelligenceFollowUpStatus(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return "NOT_TRACKED";
        }

        boolean hasActiveTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.INIT
                        || status == TaskStatusEnum.DISPATCHED
                        || status == TaskStatusEnum.RUNNING);
        if (hasActiveTask) {
            return "TRACKING";
        }

        boolean hasSuccessTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.SUCCESS);
        if (hasSuccessTask) {
            return "COMPLETED";
        }

        boolean hasFailedTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.FAILED || status == TaskStatusEnum.CANCELLED);
        if (hasFailedTask) {
            return "FAILED";
        }

        return "TRACKING";
    }

    private List<String> buildMarketIntelligenceSourceTags(ResearchTaskDO task,
                                                           MarketIntelligenceTypeEnum intelligenceType,
                                                           String summary,
                                                           int totalRiskCount,
                                                           boolean needHumanReview,
                                                           ReportReviewStatusEnum reviewStatus,
                                                           Double confidenceScore,
                                                           SignalDirectionEnum signalDirection,
                                                           boolean hasDomainStrategySignal) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (task != null && task.getSourceChannel() != null && !task.getSourceChannel().isBlank()) {
            tags.add("SOURCE_CHANNEL");
        }
        if (summary != null && !summary.isBlank()) {
            tags.add("REPORT_SUMMARY");
        }
        if (intelligenceType == MarketIntelligenceTypeEnum.RISK_ALERT || totalRiskCount > 0) {
            tags.add("RISK_ALERT");
        }
        if (hasDomainStrategySignal
                || intelligenceType == MarketIntelligenceTypeEnum.STRATEGY_SIGNAL
                || signalDirection == SignalDirectionEnum.POSITIVE
                || signalDirection == SignalDirectionEnum.NEGATIVE
                || isHighConfidence(confidenceScore)) {
            tags.add("STRATEGY_SIGNAL");
        }
        if (intelligenceType == MarketIntelligenceTypeEnum.REPORT_INSIGHT) {
            tags.add("REPORT_INSIGHT");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    @Override
    public AuditCompliancePageVO pageAuditCompliance(AuditCompliancePageQueryDTO queryDTO) {
        AuditCompliancePageQueryDTO safeQuery = queryDTO == null ? new AuditCompliancePageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<AuditComplianceListItemVO> matchedRecords = listAuditComplianceRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        AuditCompliancePageVO vo = new AuditCompliancePageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public AuditComplianceStatsVO getAuditComplianceStats() {
        List<AuditComplianceListItemVO> records = listAuditComplianceRecords(new AuditCompliancePageQueryDTO());
        AuditComplianceStatsVO vo = new AuditComplianceStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        vo.setInterceptedCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getIntercepted())).count());
        vo.setRevisedReportCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getRevised())).count());
        vo.setHumanReviewCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedHumanReview())).count());
        vo.setDecisionTraceCount(records.stream().filter(this::hasDecisionTrace).count());
        vo.setPromptAuditCount(records.stream().filter(this::hasPromptAuditTrail).count());
        return vo;
    }

    private List<AuditComplianceListItemVO> listAuditComplianceRecords(AuditCompliancePageQueryDTO queryDTO) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );

        if (reports.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ResearchTaskDO> taskMap = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        Map<String, List<AuditRecordDO>> auditMap = auditRecordMapper.selectList(
                new LambdaQueryWrapper<AuditRecordDO>()
                        .eq(AuditRecordDO::getDeleted, 0)
                        .in(AuditRecordDO::getTaskId, taskIds)
                        .orderByDesc(AuditRecordDO::getCreatedAt, AuditRecordDO::getId)
        ).stream().collect(Collectors.groupingBy(AuditRecordDO::getTaskId));

        Map<String, AiWorkflowInstanceDO> workflowMap = aiWorkflowInstanceMapper.selectList(
                new LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getDeleted, 0)
                        .in(AiWorkflowInstanceDO::getTaskId, taskIds)
                        .orderByDesc(AiWorkflowInstanceDO::getCreatedAt, AiWorkflowInstanceDO::getId)
        ).stream().collect(Collectors.toMap(
                AiWorkflowInstanceDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        Map<String, List<AiAgentExecutionDO>> agentExecutionMap = aiAgentExecutionMapper.selectList(
                new LambdaQueryWrapper<AiAgentExecutionDO>()
                        .eq(AiAgentExecutionDO::getDeleted, 0)
                        .in(AiAgentExecutionDO::getTaskId, taskIds)
                        .orderByDesc(AiAgentExecutionDO::getCreatedAt, AiAgentExecutionDO::getId)
        ).stream().collect(Collectors.groupingBy(AiAgentExecutionDO::getTaskId));
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);

        return reports.stream()
                .map(report -> toAuditComplianceItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        auditMap.getOrDefault(report.getTaskId(), List.of()),
                        workflowMap.get(report.getTaskId()),
                        agentExecutionMap.getOrDefault(report.getTaskId(), List.of()),
                        riskWarningMap.get(report.getTaskId())
                ))
                .filter(Objects::nonNull)
                .filter(item -> matchesAuditComplianceQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing((AuditComplianceListItemVO item) -> Boolean.TRUE.equals(item.getIntercepted()))
                        .reversed()
                        .thenComparing(
                                AuditComplianceListItemVO::getLatestAuditAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                AuditComplianceListItemVO::getReviewedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                AuditComplianceListItemVO::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .toList();
    }

    private AuditComplianceListItemVO toAuditComplianceItem(ResearchReportDO report,
                                                            ResearchTaskDO task,
                                                            List<AuditRecordDO> audits,
                                                            AiWorkflowInstanceDO workflow,
                                                            List<AiAgentExecutionDO> agentExecutions,
                                                            RiskWarningDO warning) {
        if (report == null || task == null) {
            return null;
        }

        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(report.getReviewStatus());
        AuditRecordDO latestAudit = audits.isEmpty() ? null : audits.get(0);
        boolean needHumanReview = resolveRiskProjection(report, warning).needHumanReview();
        boolean revised = isRevisedReport(report);
        long failedAuditCount = audits.stream()
                .filter(item -> "FAILED".equalsIgnoreCase(item.getResultStatus()))
                .count();
        boolean intercepted = reviewStatus == ReportReviewStatusEnum.REJECTED
                || failedAuditCount > 0
                || (needHumanReview && reviewStatus != ReportReviewStatusEnum.APPROVED);

        AuditComplianceListItemVO vo = new AuditComplianceListItemVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
        vo.setTraceId(task.getTraceId());
        vo.setReportId(report.getReportId());
        vo.setReportType(resolveReportType(report, task));
        vo.setFinalStatus(report.getFinalStatus());
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setReviewComment(report.getReviewComment());
        vo.setNeedHumanReview(needHumanReview);
        vo.setRevised(revised);
        vo.setIntercepted(intercepted);

        vo.setAuditCount((long) audits.size());
        vo.setFailedAuditCount(failedAuditCount);
        vo.setAgentAuditCount(audits.stream().filter(item -> "AGENT".equalsIgnoreCase(item.getOperatorType())).count());
        vo.setHumanAuditCount(audits.stream().filter(item -> "HUMAN".equalsIgnoreCase(item.getOperatorType())).count());

        vo.setAgentExecutionCount((long) agentExecutions.size());
        vo.setHumanReviewAgentCount(agentExecutions.stream()
                .filter(item -> item.getNeedHumanReview() != null && item.getNeedHumanReview() == 1)
                .count());

        vo.setWorkflowInstanceId(workflow == null ? null : workflow.getWorkflowInstanceId());
        vo.setWorkflowCode(workflow == null ? null : workflow.getWorkflowCode());
        vo.setWorkflowVersion(workflow == null ? null : workflow.getWorkflowVersion());
        vo.setWorkflowStatus(workflow == null ? null : workflow.getStatus());
        vo.setCurrentNode(workflow == null ? null : workflow.getCurrentNode());

        vo.setHasInputLog(agentExecutions.stream().anyMatch(item -> hasText(item.getInputRef())));
        vo.setHasOutputLog(agentExecutions.stream().anyMatch(item -> hasText(item.getOutputRef())));
        vo.setHasDecisionLog(agentExecutions.stream().anyMatch(item -> hasText(item.getDecisionRef())));

        vo.setLatestAuditType(latestAudit == null ? null : latestAudit.getAuditType());
        vo.setLatestAuditStage(latestAudit == null ? null : latestAudit.getAuditStage());
        vo.setLatestAuditActionCode(latestAudit == null ? null : latestAudit.getActionCode());
        vo.setLatestAuditResultStatus(latestAudit == null ? null : normalizeAuditResultStatus(latestAudit.getResultStatus()));
        vo.setLatestAuditRemark(latestAudit == null ? null : latestAudit.getRemark());
        vo.setLatestAuditAt(latestAudit == null ? null : latestAudit.getCreatedAt());

        vo.setOriginalSummary(resolveOriginalSummary(report));
        vo.setRevisedSummary(hasText(report.getRevisedSummary()) ? report.getRevisedSummary().trim() : null);
        vo.setOriginalHighlights(readTextList(report.getHighlights()));
        vo.setRevisedHighlights(readTextList(report.getRevisedHighlights()));
        vo.setOriginalRiskPoints(readTextList(report.getRiskPoints()));
        vo.setRevisedRiskPoints(readTextList(report.getRevisedRiskPoints()));
        vo.setCreatedAt(firstNonNullOf(latestAudit == null ? null : latestAudit.getCreatedAt(), report.getCreatedAt(), task.getCreatedAt()));
        return vo;
    }

    private boolean matchesAuditComplianceQuery(AuditComplianceListItemVO item, AuditCompliancePageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTaskId() != null && !queryDTO.getTaskId().isBlank()
                && !containsIgnoreCase(item.getTaskId(), queryDTO.getTaskId())) {
            return false;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !containsIgnoreCase(item.getTargetCode(), queryDTO.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReviewStatus())) {
            return false;
        }
        String auditResultStatus = normalizeAuditResultStatus(queryDTO.getAuditResultStatus());
        if (auditResultStatus != null && !auditResultStatus.equalsIgnoreCase(item.getLatestAuditResultStatus())) {
            return false;
        }
        if (queryDTO.getNeedHumanReview() != null
                && !queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview())) {
            return false;
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyIntercepted()) && !Boolean.TRUE.equals(item.getIntercepted())) {
            return false;
        }
        return true;
    }

    private boolean hasDecisionTrace(AuditComplianceListItemVO item) {
        return item != null
                && (hasText(item.getWorkflowInstanceId())
                || (item.getAgentExecutionCount() != null && item.getAgentExecutionCount() > 0));
    }

    private boolean hasPromptAuditTrail(AuditComplianceListItemVO item) {
        return item != null
                && (Boolean.TRUE.equals(item.getHasInputLog())
                || Boolean.TRUE.equals(item.getHasOutputLog())
                || Boolean.TRUE.equals(item.getHasDecisionLog()));
    }

    private boolean isRevisedReport(ResearchReportDO report) {
        return hasText(report.getRevisedSummary())
                || !readTextList(report.getRevisedHighlights()).isEmpty()
                || !readTextList(report.getRevisedRiskPoints()).isEmpty();
    }

    private String resolveOriginalSummary(ResearchReportDO report) {
        return hasText(report.getSummary()) ? report.getSummary().trim() : null;
    }

    private String normalizeAuditResultStatus(String resultStatus) {
        if (!hasText(resultStatus)) {
            return null;
        }
        if ("SUCCESS".equalsIgnoreCase(resultStatus)) {
            return "SUCCESS";
        }
        if ("FAILED".equalsIgnoreCase(resultStatus)) {
            return "FAILED";
        }
        return resultStatus.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime firstNonNullOf(LocalDateTime... candidates) {
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        for (LocalDateTime candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public ModelAgentConfigCenterVO getModelAgentConfigCenter() {
        ModelAgentConfigCenterVO vo = new ModelAgentConfigCenterVO();
        vo.setCurrentAccessRole(SecurityUtils.currentUserRole());
        vo.setEditable(roleAccessConfigService.hasPermissionForCurrentRole(RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT));

        List<ToolWhitelistItemVO> toolWhitelists = List.of(
                buildToolWhitelist("task_control_service.check_cancelled", "任务取消检查", "RUNTIME_GUARD", "ALL_AGENTS", "执行前统一检查任务是否已取消"),
                buildToolWhitelist("market_data_service.load_financial_data", "财务数据加载", "DATA_SERVICE", "financial_analysis_agent", "当前接最小财务数据占位实现"),
                buildToolWhitelist("timeout_executor.run_with_timeout", "节点超时控制", "RUNTIME_GUARD", "WORKFLOW_NODE", "对每个 LangGraph 节点做超时保护")
        );

        List<PromptTemplateItemVO> promptTemplates = List.of(
                buildPromptTemplate("planner_agent_template", "任务规划模板", "planner_agent", List.of("task_type", "target_code"), "当前为内联规则模板，未拆独立 Prompt 文件"),
                buildPromptTemplate("intent_agent_template", "意图识别模板", "intent_agent", List.of("target_name", "task_type"), "当前为内联规则模板，输出分析模式和关注维度"),
                buildPromptTemplate("financial_analysis_agent_template", "财务分析模板", "financial_analysis_agent", List.of("target_code", "financial_data"), "当前为规则占位模板，结合最小财务数据结构"),
                buildPromptTemplate("risk_review_agent_template", "风险审查模板", "risk_review_agent", List.of("financial_result", "target_name"), "当前为规则占位模板，输出固定风险审查结构"),
                buildPromptTemplate("report_generation_agent_template", "报告生成模板", "report_generation_agent", List.of("financial_result", "risk_result", "target_name"), "当前为规则占位模板，生成结构化报告结果")
        );

        List<AgentConfigItemVO> agents = List.of(
                buildAgentConfig("planner_agent", "Planner Agent", "PLANNING", 1, 5, false,
                        List.of("task_control_service.check_cancelled"),
                        List.of("task_id", "task_type", "target_code"),
                        List.of("current_stage", "current_node", "agent_audits"),
                        "负责接单和初始任务规划"),
                buildAgentConfig("intent_agent", "Intent Agent", "INTENT_UNDERSTANDING", 2, 5, false,
                        List.of("task_control_service.check_cancelled"),
                        List.of("task_id", "task_type", "target_name"),
                        List.of("intent_result", "current_stage", "agent_audits"),
                        "负责识别分析模式和关注维度"),
                buildAgentConfig("evidence_collection_agent", "Evidence Collection Agent", "EVIDENCE_COLLECTION", 3, 5, false,
                        List.of("task_control_service.check_cancelled", "market_data_service.load_financial_data"),
                        List.of("task_id", "target_code", "source_context"),
                        List.of("evidence_items", "evidence_refs", "market_context", "current_stage", "agent_audits"),
                        "负责汇总来源事件、来源报告和同标的市场快照，生成结构化证据条目"),
                buildAgentConfig("financial_analysis_agent", "Financial Analysis Agent", "FINANCIAL_ANALYSIS", 4, 10, false,
                        List.of("task_control_service.check_cancelled", "market_data_service.load_financial_data"),
                        List.of("task_id", "target_code", "target_name"),
                        List.of("financial_result", "current_stage", "agent_audits"),
                        "当前接最小财务数据占位实现，支持 FAIL001 / TIMEOUT001 测试分支"),
                buildAgentConfig("risk_review_agent", "Risk Review Agent", "RISK_REVIEW", 5, 10, false,
                        List.of("task_control_service.check_cancelled"),
                        List.of("financial_result", "target_name"),
                        List.of("risk_result", "current_stage", "agent_audits"),
                        "负责风险等级和风险点审查"),
                buildAgentConfig("report_generation_agent", "Report Generation Agent", "REPORT_GENERATION", 6, 10, false,
                        List.of("task_control_service.check_cancelled"),
                        List.of("financial_result", "risk_result", "target_name"),
                        List.of("report_result", "evidence_refs", "status", "agent_audits"),
                        "负责汇总生成结构化研究报告")
        );

        List<WorkflowConfigItemVO> ignoredWorkflows = List.of(
                buildWorkflowConfig(
                        "stock_research_workflow",
                        "1.0.0",
                        "planner_agent",
                        List.of("planner_agent", "intent_agent", "evidence_collection_agent", "financial_analysis_agent", "risk_review_agent", "report_generation_agent"),
                        "planner=5s, intent=5s, evidence=5s, financial=10s, risk=10s, report=10s",
                        "当前唯一启用的 LangGraph 串行研究工作流"
                )
        );

        List<WorkflowConfigItemVO> workflows = workflowConfigService.loadWorkflows();
        if (workflows.isEmpty()) {
            workflows = ignoredWorkflows;
        }

        List<AgentConfigItemVO> configuredAgents = agentConfigService.loadAgents();
        if (!configuredAgents.isEmpty()) {
            agents = configuredAgents;
        }
        workflows = applyAgentConfigsToWorkflows(workflows, agents);

        List<ModelStrategyItemVO> modelStrategies = List.of(
                buildModelStrategy(
                        "analysis_rule_engine",
                        "STOCK_RESEARCH_ANALYSIS",
                        "BUILTIN",
                        "RULE_PLACEHOLDER",
                        "LOCAL_INLINE",
                        true,
                        "financial_analysis_agent_template",
                        List.of("planner_agent", "intent_agent", "financial_analysis_agent"),
                        "当前未接外部大模型 SDK，使用内置规则/占位逻辑"
                ),
                buildModelStrategy(
                        "review_rule_engine",
                        "RISK_REVIEW_AND_REPORT",
                        "BUILTIN",
                        "RULE_PLACEHOLDER",
                        "LOCAL_INLINE",
                        true,
                        "risk_review_agent_template",
                        List.of("risk_review_agent", "report_generation_agent"),
                        "当前未接外部大模型 SDK，使用内置规则/占位逻辑"
                )
        );

        modelStrategies = modelStrategyConfigService.loadStrategies();
        EventAutoTriggerConfigVO eventAutoTriggerConfig = eventAutoTriggerConfigService.loadConfigView();
        EventSourceConfigVO eventSourceConfig = eventSourceConfigService.loadConfigView();
        enrichEventSourceConfigStats(eventSourceConfig);
        List<ConfigChangeAuditItemVO> configChangeAudits = configChangeAuditService.loadRecentAudits();
        List<RoleAccessConfigItemVO> roleAccessConfigs = roleAccessConfigService.loadRoles();

        EngineRuntimeConfigVO engineRuntime = new EngineRuntimeConfigVO();
        engineRuntime.setEngineCode("python-ai-engine");
        engineRuntime.setEnv("local");
        engineRuntime.setHost("0.0.0.0");
        engineRuntime.setPort(8090);
        engineRuntime.setWorkflowTimeoutSeconds(resolveWorkflowTimeoutSeconds(workflows, agents, 60));
        engineRuntime.setConsumerGroup("python-ai-engine-group");
        engineRuntime.setKafkaBootstrapServers("127.0.0.1:19092");
        engineRuntime.setDispatchTopic("ai.task.dispatch");
        engineRuntime.setStatusTopic("ai.task.status");
        engineRuntime.setResultTopic("ai.task.result");
        engineRuntime.setAuditTopic("ai.task.audit");
        engineRuntime.setRedisEndpoint("127.0.0.1:6379/0");
        engineRuntime.setRuntimeMode(resolveRuntimeMode(modelStrategies));

        ModelAgentConfigStatsVO stats = new ModelAgentConfigStatsVO();
        stats.setWorkflowCount(workflows.size());
        stats.setActiveAgentCount((int) agents.stream().filter(item -> Boolean.TRUE.equals(item.getEnabled())).count());
        stats.setModelStrategyCount(modelStrategies.size());
        stats.setPromptTemplateCount(promptTemplates.size());
        stats.setToolWhitelistCount(toolWhitelists.size());
        stats.setPlaceholderStrategyCount((int) modelStrategies.stream().filter(item -> Boolean.TRUE.equals(item.getPlaceholder())).count());
        stats.setEventAutoTriggerRuleCount(eventAutoTriggerConfig.getRules() == null ? 0 : eventAutoTriggerConfig.getRules().size());
        stats.setEventSourceConfigCount(eventSourceConfig.getSources() == null ? 0 : eventSourceConfig.getSources().size());
        stats.setConfigAuditCount(configChangeAudits.size());
        stats.setRoleAccessConfigCount(roleAccessConfigs.size());

        vo.setStats(stats);
        vo.setEngineRuntime(engineRuntime);
        vo.setWorkflows(workflows);
        vo.setAgents(agents);
        vo.setModelStrategies(modelStrategies);
        vo.setEventAutoTriggerConfig(eventAutoTriggerConfig);
        vo.setEventSourceConfig(eventSourceConfig);
        vo.setPromptTemplates(promptTemplates);
        vo.setToolWhitelists(toolWhitelists);
        vo.setRoleAccessConfigs(roleAccessConfigs);
        vo.setConfigChangeAudits(configChangeAudits);
        return vo;
    }

    private WorkflowConfigItemVO buildWorkflowConfig(String workflowCode,
                                                     String workflowVersion,
                                                     String entryAgent,
                                                     List<String> nodeSequence,
                                                     String nodeTimeoutSummary,
                                                     String remark) {
        WorkflowConfigItemVO vo = new WorkflowConfigItemVO();
        vo.setWorkflowCode(workflowCode);
        vo.setWorkflowVersion(workflowVersion);
        vo.setWorkflowType("LANGGRAPH_STATE_GRAPH");
        vo.setEntryAgent(entryAgent);
        vo.setNodeCount(nodeSequence.size());
        vo.setEnabled(true);
        vo.setDefaultSelected(true);
        vo.setNodeSequence(nodeSequence);
        vo.setNodeTimeoutSummary(nodeTimeoutSummary);
        vo.setRemark(remark);
        return vo;
    }

    private AgentConfigItemVO buildAgentConfig(String agentCode,
                                               String agentName,
                                               String stageCode,
                                               Integer executionOrder,
                                               Integer timeoutSeconds,
                                               boolean needHumanReview,
                                               List<String> toolWhitelist,
                                               List<String> inputKeys,
                                               List<String> outputKeys,
                                               String remark) {
        AgentConfigItemVO vo = new AgentConfigItemVO();
        vo.setAgentCode(agentCode);
        vo.setAgentName(agentName);
        vo.setStageCode(stageCode);
        vo.setExecutionOrder(executionOrder);
        vo.setEnabled(true);
        vo.setTimeoutSeconds(timeoutSeconds);
        vo.setNeedHumanReview(needHumanReview);
        vo.setImplementationMode("PYTHON_RULE_PLACEHOLDER");
        vo.setVersion("1.0.0");
        vo.setToolWhitelist(toolWhitelist);
        vo.setInputKeys(inputKeys);
        vo.setOutputKeys(outputKeys);
        vo.setRemark(remark);
        return vo;
    }

    private List<WorkflowConfigItemVO> applyAgentConfigsToWorkflows(List<WorkflowConfigItemVO> workflows,
                                                                    List<AgentConfigItemVO> agents) {
        if (workflows == null || workflows.isEmpty()) {
            return List.of();
        }
        Map<String, AgentConfigItemVO> agentMap = agents == null
                ? Collections.emptyMap()
                : agents.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getAgentCode() != null && !item.getAgentCode().isBlank())
                .collect(Collectors.toMap(AgentConfigItemVO::getAgentCode, item -> item, (left, right) -> right, LinkedHashMap::new));

        List<WorkflowConfigItemVO> result = new ArrayList<>();
        for (WorkflowConfigItemVO item : workflows) {
            WorkflowConfigItemVO workflow = new WorkflowConfigItemVO();
            BeanUtils.copyProperties(item, workflow);

            List<String> currentSequence = item.getNodeSequence() == null ? List.of() : item.getNodeSequence();
            List<String> effectiveSequence = currentSequence.stream()
                    .filter(agentCode -> {
                        AgentConfigItemVO config = agentMap.get(agentCode);
                        if (config == null) {
                            return true;
                        }
                        if ("report_generation_agent".equals(agentCode)) {
                            return true;
                        }
                        return !Boolean.FALSE.equals(config.getEnabled());
                    })
                    .toList();

            if (effectiveSequence.isEmpty()) {
                effectiveSequence = currentSequence;
            }

            workflow.setNodeSequence(currentSequence);
            workflow.setNodeCount(currentSequence.size());
            workflow.setEntryAgent(effectiveSequence.isEmpty() ? item.getEntryAgent() : effectiveSequence.get(0));
            workflow.setEnabled(!Boolean.FALSE.equals(item.getEnabled()));
            workflow.setNodeTimeoutSummary(buildWorkflowTimeoutSummary(effectiveSequence, agentMap));
            result.add(workflow);
        }
        return result;
    }

    private String buildWorkflowTimeoutSummary(List<String> nodeSequence, Map<String, AgentConfigItemVO> agentMap) {
        if (nodeSequence == null || nodeSequence.isEmpty()) {
            return "";
        }
        return nodeSequence.stream()
                .map(agentCode -> {
                    AgentConfigItemVO config = agentMap.get(agentCode);
                    Integer timeoutSeconds = config == null ? null : config.getTimeoutSeconds();
                    if (timeoutSeconds == null) {
                        return agentCode;
                    }
                    return agentCode + "=" + timeoutSeconds + "s";
                })
                .collect(Collectors.joining(", "));
    }

    private Integer resolveWorkflowTimeoutSeconds(List<WorkflowConfigItemVO> workflows,
                                                  List<AgentConfigItemVO> agents,
                                                  int fallbackSeconds) {
        if (workflows == null || workflows.isEmpty()) {
            return fallbackSeconds;
        }
        Map<String, AgentConfigItemVO> agentMap = agents == null
                ? Collections.emptyMap()
                : agents.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getAgentCode() != null && !item.getAgentCode().isBlank())
                .collect(Collectors.toMap(AgentConfigItemVO::getAgentCode, item -> item, (left, right) -> right, LinkedHashMap::new));

        WorkflowConfigItemVO workflow = workflows.stream()
                .filter(item -> !Boolean.FALSE.equals(item.getEnabled()))
                .filter(item -> Boolean.TRUE.equals(item.getDefaultSelected()))
                .findFirst()
                .orElseGet(() -> workflows.stream()
                        .filter(item -> !Boolean.FALSE.equals(item.getEnabled()))
                        .findFirst()
                        .orElse(workflows.get(0)));

        List<String> nodeSequence = (workflow.getNodeSequence() == null ? List.<String>of() : workflow.getNodeSequence()).stream()
                .filter(agentCode -> {
                    if ("report_generation_agent".equals(agentCode)) {
                        return true;
                    }
                    AgentConfigItemVO config = agentMap.get(agentCode);
                    return config == null || !Boolean.FALSE.equals(config.getEnabled());
                })
                .toList();
        int total = 0;
        for (String agentCode : nodeSequence) {
            AgentConfigItemVO config = agentMap.get(agentCode);
            if (config != null && config.getTimeoutSeconds() != null) {
                total += config.getTimeoutSeconds();
            }
        }
        return total > 0 ? total : fallbackSeconds;
    }

    private ModelStrategyItemVO buildModelStrategy(String strategyCode,
                                                   String scenarioCode,
                                                   String provider,
                                                   String modelName,
                                                   String accessMode,
                                                   boolean placeholder,
                                                   String promptTemplateCode,
                                                   List<String> boundAgents,
                                                   String remark) {
        ModelStrategyItemVO vo = new ModelStrategyItemVO();
        vo.setStrategyCode(strategyCode);
        vo.setScenarioCode(scenarioCode);
        vo.setProvider(provider);
        vo.setModelName(modelName);
        vo.setAccessMode(accessMode);
        vo.setEnabled(true);
        vo.setPlaceholder(placeholder);
        vo.setPromptTemplateCode(promptTemplateCode);
        vo.setBoundAgents(boundAgents);
        vo.setRemark(remark);
        return vo;
    }

    private String resolveRuntimeMode(List<ModelStrategyItemVO> modelStrategies) {
        if (modelStrategies == null || modelStrategies.isEmpty()) {
            return "RULE_PLACEHOLDER";
        }
        boolean hasModelStrategy = modelStrategies.stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getEnabled()) && !Boolean.TRUE.equals(item.getPlaceholder()));
        if (hasModelStrategy) {
            return "LANGCHAIN_WITH_FALLBACK";
        }
        return "RULE_PLACEHOLDER";
    }

    private PromptTemplateItemVO buildPromptTemplate(String templateCode,
                                                     String templateName,
                                                     String boundAgentCode,
                                                     List<String> variables,
                                                     String remark) {
        PromptTemplateItemVO vo = new PromptTemplateItemVO();
        vo.setTemplateCode(templateCode);
        vo.setTemplateName(templateName);
        vo.setVersion("1.0.0");
        vo.setSourceType("FILE_SYSTEM_PROMPT");
        vo.setEditable(true);
        vo.setEnabled(true);
        vo.setBoundAgentCode(boundAgentCode);
        vo.setVariables(variables);
        vo.setTemplatePath(promptTemplateConfigService.resolveTemplatePathForDisplay(templateCode));
        vo.setTemplateContent(promptTemplateConfigService.loadTemplateContent(templateCode));
        vo.setRemark(remark);
        return vo;
    }

    private ToolWhitelistItemVO buildToolWhitelist(String toolCode,
                                                   String toolName,
                                                   String toolType,
                                                   String scope,
                                                   String remark) {
        ToolWhitelistItemVO vo = new ToolWhitelistItemVO();
        vo.setToolCode(toolCode);
        vo.setToolName(toolName);
        vo.setToolType(toolType);
        vo.setEnabled(true);
        vo.setScope(scope);
        vo.setRemark(remark);
        return vo;
    }

    @Override
    public ResearchWorkbenchVO getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO) {
        ResearchWorkbenchQueryDTO safeQuery = queryDTO == null ? new ResearchWorkbenchQueryDTO() : queryDTO;
        ResearchWorkbenchVO vo = new ResearchWorkbenchVO();
        vo.setTargetCode(safeQuery.getTargetCode());
        vo.setTargetName(safeQuery.getTargetName());
        vo.setTaskCount(0L);
        vo.setReportCount(0L);
        vo.setActiveTaskCount(0L);
        vo.setSuccessTaskCount(0L);
        vo.setFailedTaskCount(0L);
        vo.setHighConfidenceReportCount(0L);
        vo.setPendingReviewCount(0L);
        vo.setRiskDispositionSummary(emptyResearchWorkbenchDispositionSummary("RISK_WARNING"));
        vo.setStrategySignalDispositionSummary(emptyResearchWorkbenchDispositionSummary("STRATEGY_SIGNAL"));
        vo.setMarketIntelligenceDispositionSummary(emptyResearchWorkbenchDispositionSummary("MARKET_INTELLIGENCE"));
        vo.setRecentTasks(List.of());

        if ((safeQuery.getTargetCode() == null || safeQuery.getTargetCode().isBlank())
                && (safeQuery.getTargetName() == null || safeQuery.getTargetName().isBlank())) {
            return vo;
        }

        LambdaQueryWrapper<ResearchTaskDO> wrapper = new LambdaQueryWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getDeleted, 0)
                .orderByDesc(ResearchTaskDO::getCreatedAt, ResearchTaskDO::getId);
        if (safeQuery.getTargetCode() != null && !safeQuery.getTargetCode().isBlank()) {
            wrapper.eq(ResearchTaskDO::getTargetCode, safeQuery.getTargetCode().trim());
        }
        if (safeQuery.getTargetName() != null && !safeQuery.getTargetName().isBlank()) {
            wrapper.like(ResearchTaskDO::getTargetName, safeQuery.getTargetName().trim());
        }

        List<ResearchTaskDO> tasks = researchTaskMapper.selectList(wrapper);
        if (tasks.isEmpty()) {
            return vo;
        }

        ResearchTaskDO latestTask = tasks.get(0);
        Map<String, ResearchTaskDO> taskMap = tasks.stream()
                .filter(item -> item.getTaskId() != null && !item.getTaskId().isBlank())
                .collect(Collectors.toMap(
                        ResearchTaskDO::getTaskId,
                        item -> item,
                        (left, right) -> left
                ));
        vo.setTargetCode(latestTask.getTargetCode());
        vo.setTargetName(latestTask.getTargetName());
        vo.setTargetType(latestTask.getTargetType());
        vo.setTaskCount((long) tasks.size());
        vo.setActiveTaskCount(tasks.stream().filter(item -> item.getStatus() != null && (TaskStatusEnum.DISPATCHED.name().equals(item.getStatus()) || TaskStatusEnum.RUNNING.name().equals(item.getStatus()))).count());
        vo.setSuccessTaskCount(tasks.stream().filter(item -> TaskStatusEnum.SUCCESS.name().equals(item.getStatus())).count());
        vo.setFailedTaskCount(tasks.stream().filter(item -> TaskStatusEnum.FAILED.name().equals(item.getStatus())).count());

        Set<String> taskIds = tasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            vo.setRecentTasks(tasks.stream()
                    .limit(resolveRecentTaskLimit(safeQuery.getRecentTaskLimit()))
                    .map(item -> toResearchWorkbenchRecentTask(item, null))
                    .toList());
            return vo;
        }

        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .in(ResearchReportDO::getTaskId, taskIds)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );
        Map<String, RiskWarningDO> riskWarningMap = loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );
        Map<String, StrategySignalDO> strategySignalMap = loadLatestStrategySignalMapByTaskIds(taskIds);

        vo.setReportCount((long) reports.size());
        vo.setHighConfidenceReportCount(reports.stream().filter(item -> isHighConfidence(item.getConfidenceScore() == null ? null : item.getConfidenceScore().doubleValue())).count());
        vo.setPendingReviewCount(reports.stream().filter(item -> ReportReviewStatusEnum.PENDING == resolveReviewStatus(item.getReviewStatus())).count());
        populateResearchWorkbenchDispositionSummaries(vo, tasks, reports, taskMap, riskWarningMap, riskWarningDetailMap, strategySignalMap);

        Map<String, ResearchReportDO> latestReportMap = reports.stream().collect(Collectors.toMap(
                ResearchReportDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        if (!reports.isEmpty()) {
            ResearchReportDO latestReport = reports.get(0);
            RiskWarningDO latestWarning = riskWarningMap.get(latestReport.getTaskId());
            vo.setLatestInsight(toResearchWorkbenchInsight(
                    latestReport,
                    taskMap.get(latestReport.getTaskId()),
                    latestWarning,
                    latestWarning == null
                            ? List.of()
                            : riskWarningDetailMap.getOrDefault(latestWarning.getWarningId(), List.of()),
                    strategySignalMap.get(latestReport.getTaskId())
            ));
        }

        vo.setRecentTasks(tasks.stream()
                .limit(resolveRecentTaskLimit(safeQuery.getRecentTaskLimit()))
                .map(item -> toResearchWorkbenchRecentTask(item, latestReportMap.get(item.getTaskId())))
                .toList());
        return vo;
    }

    private int resolveRecentTaskLimit(Integer recentTaskLimit) {
        if (recentTaskLimit == null || recentTaskLimit < 1) {
            return 6;
        }
        return Math.min(recentTaskLimit, 10);
    }

    private ResearchWorkbenchInsightVO toResearchWorkbenchInsight(ResearchReportDO report,
                                                                  ResearchTaskDO task,
                                                                  RiskWarningDO warning,
                                                                  List<RiskWarningDetailDO> details,
                                                                  StrategySignalDO strategySignal) {
        if (report == null && warning == null) {
            return null;
        }
        String summary = report == null ? normalizeText(warning == null ? null : warning.getWarningSummary()) : resolveReportCenterSummary(report);
        if ((summary == null || summary.isBlank()) && strategySignal != null) {
            summary = normalizeText(strategySignal.getReasonSummary());
        }
        List<String> highlights = report == null ? List.of() : readPreferredTextList(report.getRevisedHighlights(), report.getHighlights());
        List<String> fallbackRiskPoints = report == null ? List.of() : readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints());
        List<String> fallbackRiskWarnings = report == null ? List.of() : readTextList(report.getRiskWarnings());
        List<String> domainRiskPoints = warning == null ? List.of() : buildDomainRiskInsightPoints(warning, details);
        int totalRiskCount = warning != null
                ? 1 + (details == null ? 0 : details.size())
                : fallbackRiskPoints.size() + fallbackRiskWarnings.size();
        Double reportConfidenceScore = report == null || report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();
        Double signalConfidenceScore = strategySignal == null || strategySignal.getConfidenceScore() == null
                ? null
                : strategySignal.getConfidenceScore().doubleValue();
        Double confidenceScore = signalConfidenceScore == null ? reportConfidenceScore : signalConfidenceScore;
        boolean needHumanReview = warning != null
                ? isDomainRiskHumanReview(warning)
                : report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;
        ReportReviewStatusEnum reviewStatus = resolveReviewStatus(
                warning != null && warning.getReviewStatus() != null ? warning.getReviewStatus() : (report == null ? null : report.getReviewStatus())
        );
        SignalDirectionEnum signalDirection = strategySignal == null
                ? resolveSignalDirection(summary, totalRiskCount, needHumanReview, confidenceScore)
                : resolveDomainSignalDirection(strategySignal);
        SignalStrengthEnum signalStrength = strategySignal == null
                ? resolveSignalStrength(calculateSignalScore(confidenceScore, totalRiskCount, needHumanReview, reviewStatus, signalDirection))
                : resolveDomainSignalStrength(strategySignal);
        RiskLevelEnum riskLevel = warning != null
                ? resolveDomainRiskLevel(warning)
                : (totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null);

        ResearchWorkbenchInsightVO vo = new ResearchWorkbenchInsightVO();
        vo.setTaskId(report == null ? (warning == null ? null : warning.getTaskId()) : report.getTaskId());
        vo.setTaskTitle(task == null ? null : task.getTaskTitle());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportType(report == null ? (task == null ? null : task.getTaskType()) : resolveReportType(report, task));
        vo.setFinalStatus(report == null ? (task == null ? null : task.getStatus()) : report.getFinalStatus());
        vo.setConfidenceScore(confidenceScore);
        vo.setNeedHumanReview(needHumanReview);
        vo.setReviewStatus(reviewStatus.name());
        vo.setReviewedBy(warning != null && warning.getReviewerId() != null ? warning.getReviewerId() : (report == null ? null : report.getReviewedBy()));
        vo.setReviewedAt(warning != null && warning.getReviewTime() != null ? warning.getReviewTime() : (report == null ? null : report.getReviewedAt()));
        vo.setRevised(report != null && isReportRevised(report));
        vo.setSummaryRevised(report != null && isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && isRiskPointsRevised(report));
        vo.setSignalDirection(signalDirection.name());
        vo.setSignalStrength(signalStrength.name());
        vo.setRiskLevel(riskLevel == null ? null : riskLevel.name());
        vo.setSummary(summary);
        vo.setHighlights(highlights);
        vo.setRiskPoints(domainRiskPoints.isEmpty() ? (fallbackRiskPoints.isEmpty() ? fallbackRiskWarnings : fallbackRiskPoints) : domainRiskPoints);
        vo.setCreatedAt(firstNonNull(
                report == null ? (warning == null ? null : warning.getCreatedAt()) : report.getCreatedAt(),
                task == null ? null : task.getCreatedAt()
        ));
        return vo;
    }

    private ResearchWorkbenchRecentTaskVO toResearchWorkbenchRecentTask(ResearchTaskDO task, ResearchReportDO report) {
        ResearchWorkbenchRecentTaskVO vo = new ResearchWorkbenchRecentTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setCurrentStage(task.getCurrentStage());
        vo.setRetryCount(task.getRetryCount());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportReviewStatus(report == null ? null : resolveReviewStatus(report.getReviewStatus()).name());
        vo.setRevised(report != null && isReportRevised(report));
        vo.setSummaryRevised(report != null && isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && isRiskPointsRevised(report));
        vo.setConfidenceScore(report == null || report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue());
        vo.setFinishTime(task.getFinishTime());
        vo.setCreatedAt(task.getCreatedAt());
        return vo;
    }

    private void populateResearchWorkbenchDispositionSummaries(ResearchWorkbenchVO vo,
                                                               List<ResearchTaskDO> tasks,
                                                               List<ResearchReportDO> reports,
                                                               Map<String, ResearchTaskDO> taskMap,
                                                               Map<String, RiskWarningDO> riskWarningMap,
                                                               Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap,
                                                               Map<String, StrategySignalDO> strategySignalMap) {
        if (vo == null || tasks == null || tasks.isEmpty()) {
            return;
        }

        Set<String> taskIds = tasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());
        List<ResearchReportDO> safeReports = reports == null ? List.of() : reports;
        Set<String> reportIds = safeReports.stream()
                .map(ResearchReportDO::getReportId)
                .filter(reportId -> reportId != null && !reportId.isBlank())
                .collect(Collectors.toSet());
        Map<String, ResearchReportDO> latestReportMap = safeReports.stream().collect(Collectors.toMap(
                ResearchReportDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));

        List<ResearchTaskDO> riskFollowUpTasks = loadResearchWorkbenchFollowUpTasks("RISK_WARNING", taskIds, reportIds);
        Map<String, List<ResearchTaskDO>> riskFollowUpTaskMapBySourceTaskId = groupFollowUpTasksBySourceTaskId(riskFollowUpTasks);
        Map<String, List<ResearchTaskDO>> riskFollowUpTaskMapBySourceReportId = groupFollowUpTasksBySourceReportId(riskFollowUpTasks);
        Set<String> coveredRiskTaskIds = riskWarningMap == null
                ? Collections.emptySet()
                : riskWarningMap.keySet().stream()
                        .filter(taskId -> taskId != null && !taskId.isBlank())
                        .collect(Collectors.toSet());
        List<String> riskStatuses = new ArrayList<>();
        if (riskWarningMap != null && !riskWarningMap.isEmpty()) {
            riskStatuses.addAll(riskWarningMap.values().stream()
                    .map(warning -> toRiskWarningItem(
                            warning,
                            taskMap.get(warning.getTaskId()),
                            latestReportMap.get(warning.getTaskId()),
                            resolveRiskWarningFollowUpSummary(
                                    taskMap.get(warning.getTaskId()),
                                    latestReportMap.get(warning.getTaskId()),
                                    riskFollowUpTaskMapBySourceTaskId,
                                    riskFollowUpTaskMapBySourceReportId
                            ),
                            riskWarningDetailMap == null ? List.of() : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of())
                    ))
                    .filter(Objects::nonNull)
                    .map(RiskWarningListItemVO::getFollowUpStatus)
                    .filter(status -> status != null && !status.isBlank())
                    .toList());
        }
        riskStatuses.addAll(safeReports.stream()
                .filter(report -> report.getTaskId() == null || !coveredRiskTaskIds.contains(report.getTaskId()))
                .map(report -> toRiskWarningItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        resolveRiskWarningFollowUpSummary(
                                taskMap.get(report.getTaskId()),
                                report,
                                riskFollowUpTaskMapBySourceTaskId,
                                riskFollowUpTaskMapBySourceReportId
                        )
                ))
                .filter(Objects::nonNull)
                .map(RiskWarningListItemVO::getFollowUpStatus)
                .filter(status -> status != null && !status.isBlank())
                .toList());
        vo.setRiskDispositionSummary(buildResearchWorkbenchDispositionSummary("RISK_WARNING", riskStatuses));

        List<ResearchTaskDO> strategyFollowUpTasks = loadResearchWorkbenchFollowUpTasks("STRATEGY_SIGNAL", taskIds, reportIds);
        Map<String, List<ResearchTaskDO>> strategyFollowUpTaskMapBySourceTaskId = groupFollowUpTasksBySourceTaskId(strategyFollowUpTasks);
        Map<String, List<ResearchTaskDO>> strategyFollowUpTaskMapBySourceReportId = groupFollowUpTasksBySourceReportId(strategyFollowUpTasks);
        Set<String> coveredStrategyTaskIds = strategySignalMap == null
                ? Collections.emptySet()
                : strategySignalMap.keySet().stream()
                        .filter(taskId -> taskId != null && !taskId.isBlank())
                        .collect(Collectors.toSet());
        List<String> strategyStatuses = new ArrayList<>();
        if (strategySignalMap != null && !strategySignalMap.isEmpty()) {
            strategyStatuses.addAll(strategySignalMap.values().stream()
                    .map(signal -> {
                        RiskWarningDO warning = riskWarningMap == null ? null : riskWarningMap.get(signal.getTaskId());
                        return toStrategySignalItem(
                                signal,
                                taskMap.get(signal.getTaskId()),
                                latestReportMap.get(signal.getTaskId()),
                                resolveStrategySignalFollowUpSummary(
                                        taskMap.get(signal.getTaskId()),
                                        latestReportMap.get(signal.getTaskId()),
                                        strategyFollowUpTaskMapBySourceTaskId,
                                        strategyFollowUpTaskMapBySourceReportId
                                ),
                                List.of(),
                                warning,
                                warning == null || riskWarningDetailMap == null
                                        ? List.of()
                                        : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of())
                        );
                    })
                    .filter(Objects::nonNull)
                    .map(StrategySignalListItemVO::getFollowUpStatus)
                    .filter(status -> status != null && !status.isBlank())
                    .toList());
        }
        strategyStatuses.addAll(safeReports.stream()
                .filter(report -> report.getTaskId() == null || !coveredStrategyTaskIds.contains(report.getTaskId()))
                .map(report -> {
                    RiskWarningDO warning = riskWarningMap == null ? null : riskWarningMap.get(report.getTaskId());
                    return toStrategySignalItem(
                            report,
                            taskMap.get(report.getTaskId()),
                            resolveStrategySignalFollowUpSummary(
                                    taskMap.get(report.getTaskId()),
                                    report,
                                    strategyFollowUpTaskMapBySourceTaskId,
                                    strategyFollowUpTaskMapBySourceReportId
                            ),
                            warning,
                            warning == null || riskWarningDetailMap == null
                                    ? List.of()
                                    : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of())
                    );
                })
                .filter(Objects::nonNull)
                .map(StrategySignalListItemVO::getFollowUpStatus)
                .filter(status -> status != null && !status.isBlank())
                .toList());
        vo.setStrategySignalDispositionSummary(buildResearchWorkbenchDispositionSummary("STRATEGY_SIGNAL", strategyStatuses));

        List<ResearchTaskDO> intelligenceFollowUpTasks = loadResearchWorkbenchFollowUpTasks("MARKET_INTELLIGENCE", taskIds, reportIds);
        Map<String, List<ResearchTaskDO>> intelligenceFollowUpTaskMapBySourceTaskId = groupFollowUpTasksBySourceTaskId(intelligenceFollowUpTasks);
        Map<String, List<ResearchTaskDO>> intelligenceFollowUpTaskMapBySourceReportId = groupFollowUpTasksBySourceReportId(intelligenceFollowUpTasks);
        List<String> intelligenceStatuses = safeReports.stream()
                .map(report -> {
                    RiskWarningDO warning = riskWarningMap == null ? null : riskWarningMap.get(report.getTaskId());
                    StrategySignalDO signal = strategySignalMap == null ? null : strategySignalMap.get(report.getTaskId());
                    return toMarketIntelligenceItem(
                            report,
                            taskMap.get(report.getTaskId()),
                            resolveMarketIntelligenceFollowUpSummary(
                                    taskMap.get(report.getTaskId()),
                                    report,
                                    intelligenceFollowUpTaskMapBySourceTaskId,
                                    intelligenceFollowUpTaskMapBySourceReportId
                            ),
                            warning,
                            warning == null || riskWarningDetailMap == null
                                    ? List.of()
                                    : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of()),
                            signal
                    );
                })
                .filter(Objects::nonNull)
                .map(MarketIntelligenceListItemVO::getFollowUpStatus)
                .filter(status -> status != null && !status.isBlank())
                .toList();
        vo.setMarketIntelligenceDispositionSummary(buildResearchWorkbenchDispositionSummary("MARKET_INTELLIGENCE", intelligenceStatuses));
    }

    private List<String> buildDomainRiskInsightPoints(RiskWarningDO warning, List<RiskWarningDetailDO> details) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        if (details != null) {
            for (RiskWarningDetailDO detail : details) {
                String description = normalizeText(detail.getDetailDesc());
                if (description != null) {
                    points.add(description);
                    continue;
                }
                String indicatorName = normalizeText(detail.getIndicatorName());
                String indicatorValue = normalizeText(detail.getIndicatorValue());
                if (indicatorName != null && indicatorValue != null) {
                    points.add(indicatorName + ": " + indicatorValue);
                } else if (indicatorValue != null) {
                    points.add(indicatorValue);
                }
            }
        }
        if (warning != null && warning.getWarningReason() != null && !warning.getWarningReason().isBlank()) {
            for (String item : warning.getWarningReason().split("\\R")) {
                if (item != null && !item.isBlank()) {
                    points.add(item.trim());
                }
            }
        }
        if (points.isEmpty() && warning != null && warning.getWarningSummary() != null && !warning.getWarningSummary().isBlank()) {
            points.add(warning.getWarningSummary().trim());
        }
        return new ArrayList<>(points);
    }

    private ResearchWorkbenchDispositionSummaryVO emptyResearchWorkbenchDispositionSummary(String domainCode) {
        return buildResearchWorkbenchDispositionSummary(domainCode, List.of());
    }

    private ResearchWorkbenchDispositionSummaryVO buildResearchWorkbenchDispositionSummary(String domainCode,
                                                                                           List<String> followUpStatuses) {
        List<String> safeStatuses = followUpStatuses == null ? List.of() : followUpStatuses;
        ResearchWorkbenchDispositionSummaryVO vo = new ResearchWorkbenchDispositionSummaryVO();
        vo.setDomainCode(domainCode);
        vo.setTotalCount((long) safeStatuses.size());
        vo.setNotTrackedCount(safeStatuses.stream().filter("NOT_TRACKED"::equals).count());
        vo.setTrackingCount(safeStatuses.stream().filter("TRACKING"::equals).count());
        vo.setCompletedCount(safeStatuses.stream().filter("COMPLETED"::equals).count());
        vo.setFailedCount(safeStatuses.stream().filter("FAILED"::equals).count());
        return vo;
    }

    private List<ResearchTaskDO> loadResearchWorkbenchFollowUpTasks(String sourceDomain,
                                                                    Set<String> sourceTaskIds,
                                                                    Set<String> sourceReportIds) {
        boolean hasSourceTaskIds = sourceTaskIds != null && !sourceTaskIds.isEmpty();
        boolean hasSourceReportIds = sourceReportIds != null && !sourceReportIds.isEmpty();
        if (!hasSourceTaskIds && !hasSourceReportIds) {
            return List.of();
        }

        LambdaQueryWrapper<ResearchTaskDO> wrapper = new LambdaQueryWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getDeleted, 0)
                .eq(ResearchTaskDO::getSourceDomain, sourceDomain);
        if (hasSourceTaskIds && hasSourceReportIds) {
            wrapper.and(nested -> nested
                    .in(ResearchTaskDO::getSourceTaskId, sourceTaskIds)
                    .or()
                    .in(ResearchTaskDO::getSourceReportId, sourceReportIds));
        } else if (hasSourceTaskIds) {
            wrapper.in(ResearchTaskDO::getSourceTaskId, sourceTaskIds);
        } else {
            wrapper.in(ResearchTaskDO::getSourceReportId, sourceReportIds);
        }
        return researchTaskMapper.selectList(wrapper);
    }

    private Map<String, List<ResearchTaskDO>> groupFollowUpTasksBySourceTaskId(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));
    }

    private Map<String, List<ResearchTaskDO>> groupFollowUpTasksBySourceReportId(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));
    }

    private List<String> readPreferredTextList(String preferredRawJson, String fallbackRawJson) {
        List<String> preferred = readTextList(preferredRawJson);
        return preferred.isEmpty() ? readTextList(fallbackRawJson) : preferred;
    }

    @Override
    public TaskReportVO getTaskReportOnly(String taskId) {
        String cacheKey = RedisKeyBuilder.taskResult(taskId);
        String cache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null && !cache.isBlank()) {
            try {
                TaskReportVO cached = objectMapper.readValue(cache, TaskReportVO.class);
                boolean upgraded = hydrateTaskReportContextFields(cached);
                upgraded = hydrateTaskReportDomainFields(cached) || upgraded;
                if (isCurrentTaskReportCache(cached)) {
                    if (upgraded) {
                        try {
                            stringRedisTemplate.opsForValue().set(
                                    cacheKey,
                                    objectMapper.writeValueAsString(cached),
                                    java.time.Duration.ofHours(12)
                            );
                        } catch (Exception ignored) {
                        }
                    }
                    return cached;
                }
            } catch (Exception ignored) {
            }
        }

        ResearchReportDO report = researchReportMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getTaskId, taskId)
                        .eq(ResearchReportDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (report == null) {
            return null;
        }

        RiskWarningDO warning = loadLatestRiskWarningMapByTaskIds(Set.of(taskId)).get(taskId);
        List<RiskWarningDetailDO> warningDetails = warning == null
                ? List.of()
                : loadRiskWarningDetailMapByWarningIds(Set.of(warning.getWarningId()))
                .getOrDefault(warning.getWarningId(), List.of());

        TaskReportVO vo = toTaskReportVO(report, warning, warningDetails);
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(vo),
                    java.time.Duration.ofHours(12)
            );
        } catch (Exception ignored) {
        }
        return vo;
    }

    private TaskReportVO toTaskReportVO(ResearchReportDO report,
                                        RiskWarningDO warning,
                                        List<RiskWarningDetailDO> warningDetails) {
        String originalSummary = normalizeText(report.getSummary());
        String revisedSummary = normalizeText(report.getRevisedSummary());
        String displaySummary = resolveDisplaySummary(report.getRevisedSummary(), report.getSummary());

        List<String> originalHighlights = readTextList(report.getHighlights());
        List<String> revisedHighlights = readTextList(report.getRevisedHighlights());
        List<String> displayHighlights = resolveDisplayList(revisedHighlights, originalHighlights);

        List<String> fallbackRiskWarnings = readTextList(report.getRiskWarnings());
        List<String> domainRiskWarnings = warning == null ? List.of() : buildDomainRiskWarningMessages(warning);
        List<String> originalRiskPoints = readTextList(report.getRiskPoints());
        List<String> revisedRiskPoints = readTextList(report.getRevisedRiskPoints());
        List<String> displayRiskPoints = resolveDisplayList(revisedRiskPoints, originalRiskPoints);

        TaskReportVO vo = new TaskReportVO();
        vo.setTaskType(report.getTaskType());
        vo.setFinalStatus(report.getFinalStatus());
        vo.setReportId(report.getReportId());
        vo.setVersionNo(defaultVersionNo(report.getVersionNo()));
        vo.setReportType(resolveTaskReportType(report));
        vo.setSummary(displaySummary);
        vo.setOriginalSummary(originalSummary);
        vo.setDisplaySummary(displaySummary);
        vo.setConfidenceScore(report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue());
        vo.setNeedHumanReview(warning == null
                ? report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1
                : isDomainRiskHumanReview(warning));
        vo.setRiskWarnings(domainRiskWarnings.isEmpty() ? fallbackRiskWarnings : domainRiskWarnings);
        vo.setOriginalHighlights(originalHighlights);
        vo.setDisplayHighlights(displayHighlights);
        vo.setOriginalRiskPoints(originalRiskPoints);
        vo.setDisplayRiskPoints(displayRiskPoints);
        vo.setResultRef(report.getResultRef());
        vo.setRawPayload(report.getRawPayload());
        vo.setReviewStatus(report.getReviewStatus());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt() == null ? null : report.getReviewedAt().toString());
        vo.setRevisedSummary(revisedSummary);
        vo.setRevisedHighlights(revisedHighlights);
        vo.setRevisedRiskPoints(revisedRiskPoints);
        vo.setReviewComment(report.getReviewComment());

        TaskReportVO.ReportMetaVO meta = new TaskReportVO.ReportMetaVO();
        meta.setReportId(report.getReportId());
        meta.setReportType(resolveTaskReportType(report));
        meta.setHighlights(originalHighlights);
        meta.setRiskPoints(originalRiskPoints);
        meta.setSummary(originalSummary);
        vo.setReportMeta(meta);
        hydrateTaskReportContextFields(vo);
        hydrateTaskReportDomainFields(vo);

        return vo;
    }

    private boolean hydrateTaskReportDomainFields(TaskReportVO report) {
        if (report == null || !hasText(report.getReportId())) {
            return false;
        }

        boolean changed = false;
        List<ResearchReportSectionDO> sections = researchReportSectionMapper.selectList(
                new LambdaQueryWrapper<ResearchReportSectionDO>()
                        .eq(ResearchReportSectionDO::getReportId, report.getReportId())
                        .eq(ResearchReportSectionDO::getDeleted, 0)
                        .orderByAsc(ResearchReportSectionDO::getSectionOrder)
                        .orderByAsc(ResearchReportSectionDO::getId)
        );
        sections = sections == null ? List.of() : sections;
        if (!sections.isEmpty()) {
            List<TaskReportVO.ReportSectionVO> sectionItems = sections.stream()
                    .map(this::toReportSection)
                    .toList();
            if (!Objects.equals(report.getSections(), sectionItems)) {
                report.setSections(sectionItems);
                changed = true;
            }
        }

        List<ReportEvidenceRefDO> evidenceRefs = reportEvidenceRefMapper.selectList(
                new LambdaQueryWrapper<ReportEvidenceRefDO>()
                        .eq(ReportEvidenceRefDO::getReportId, report.getReportId())
                        .eq(ReportEvidenceRefDO::getDeleted, 0)
                        .orderByAsc(ReportEvidenceRefDO::getId)
        );
        evidenceRefs = evidenceRefs == null ? List.of() : evidenceRefs;
        if (!evidenceRefs.isEmpty()) {
            List<TaskReportVO.ReportEvidenceItemVO> domainEvidenceItems = evidenceRefs.stream()
                    .map(this::toReportEvidenceItem)
                    .toList();
            List<TaskReportVO.ReportEvidenceItemVO> mergedEvidenceItems = mergeEvidenceItems(
                    domainEvidenceItems,
                    report.getEvidenceItems()
            );
            if (!Objects.equals(report.getEvidenceItems(), mergedEvidenceItems)) {
                report.setEvidenceItems(mergedEvidenceItems);
                changed = true;
            }

            List<String> domainRefs = evidenceRefs.stream()
                    .map(this::toEvidenceRefText)
                    .filter(Objects::nonNull)
                    .toList();
            List<String> mergedRefs = mergeTextRefs(domainRefs, report.getEvidenceRefs());
            if (!Objects.equals(report.getEvidenceRefs(), mergedRefs)) {
                report.setEvidenceRefs(mergedRefs);
                changed = true;
            }
        }

        List<HumanReviewRecordDO> reviewRecords = humanReviewRecordMapper.selectList(
                new LambdaQueryWrapper<HumanReviewRecordDO>()
                        .eq(HumanReviewRecordDO::getRelatedObjectType, "REPORT")
                        .eq(HumanReviewRecordDO::getRelatedObjectId, report.getReportId())
                        .eq(HumanReviewRecordDO::getDeleted, 0)
                        .orderByDesc(HumanReviewRecordDO::getId)
        );
        reviewRecords = reviewRecords == null ? List.of() : reviewRecords;
        List<TaskReportVO.HumanReviewRecordVO> humanReviews = reviewRecords.stream()
                .map(this::toHumanReviewRecord)
                .toList();
        if (!Objects.equals(report.getHumanReviewRecords(), humanReviews)) {
            report.setHumanReviewRecords(humanReviews);
            changed = true;
        }
        return changed;
    }

    private TaskReportVO.ReportSectionVO toReportSection(ResearchReportSectionDO section) {
        TaskReportVO.ReportSectionVO vo = new TaskReportVO.ReportSectionVO();
        vo.setSectionId(section.getSectionId());
        vo.setVersionNo(defaultVersionNo(section.getVersionNo()));
        vo.setSectionCode(section.getSectionCode());
        vo.setSectionTitle(section.getSectionTitle());
        vo.setSectionOrder(section.getSectionOrder());
        vo.setSectionContent(section.getSectionContent());
        vo.setSectionItems(readTextList(section.getSectionItems()));
        vo.setRevisedContent(section.getRevisedContent());
        vo.setRevisedItems(readTextList(section.getRevisedItems()));
        vo.setDisplayContent(resolveDisplaySummary(section.getRevisedContent(), section.getSectionContent()));
        vo.setDisplayItems(resolveDisplayList(vo.getRevisedItems(), vo.getSectionItems()));
        vo.setReviewStatus(section.getReviewStatus());
        vo.setReviewedBy(section.getReviewedBy());
        vo.setReviewedAt(section.getReviewedAt() == null ? null : section.getReviewedAt().toString());
        vo.setReviewComment(section.getReviewComment());
        vo.setConfidenceScore(section.getConfidenceScore() == null ? null : section.getConfidenceScore().doubleValue());
        return vo;
    }

    private TaskReportVO.ReportEvidenceItemVO toReportEvidenceItem(ReportEvidenceRefDO ref) {
        TaskReportVO.ReportEvidenceItemVO item = new TaskReportVO.ReportEvidenceItemVO();
        item.setEvidenceId(ref.getEvidenceId());
        item.setEvidenceType(ref.getSourceType());
        item.setSource(ref.getSourceType());
        item.setTitle(hasText(ref.getConclusionCode()) ? ref.getConclusionCode() : ref.getSourceRefId());
        item.setSummary(ref.getEvidenceSummary());
        item.setUrl(ref.getEvidenceUrl());
        item.setReferenceId(ref.getSourceRefId());
        item.setRelevance(ref.getConfidenceScore() == null ? null : ref.getConfidenceScore().toPlainString());
        return item;
    }

    private String toEvidenceRefText(ReportEvidenceRefDO ref) {
        String sourceType = normalizeText(ref.getSourceType());
        String sourceRefId = normalizeText(ref.getSourceRefId());
        if (sourceType == null && sourceRefId == null) {
            return null;
        }
        if (sourceType == null) {
            return sourceRefId;
        }
        return sourceType + ":" + (sourceRefId == null ? "" : sourceRefId);
    }

    private List<TaskReportVO.ReportEvidenceItemVO> mergeEvidenceItems(List<TaskReportVO.ReportEvidenceItemVO> preferred,
                                                                       List<TaskReportVO.ReportEvidenceItemVO> fallback) {
        Map<String, TaskReportVO.ReportEvidenceItemVO> merged = new LinkedHashMap<>();
        for (TaskReportVO.ReportEvidenceItemVO item : preferred == null ? List.<TaskReportVO.ReportEvidenceItemVO>of() : preferred) {
            merged.put(evidenceItemKey(item), item);
        }
        for (TaskReportVO.ReportEvidenceItemVO item : fallback == null ? List.<TaskReportVO.ReportEvidenceItemVO>of() : fallback) {
            merged.putIfAbsent(evidenceItemKey(item), item);
        }
        return new ArrayList<>(merged.values());
    }

    private String evidenceItemKey(TaskReportVO.ReportEvidenceItemVO item) {
        String evidenceId = normalizeText(item.getEvidenceId());
        if (evidenceId != null) {
            return evidenceId;
        }
        return normalizeText(item.getTitle()) + "::" + normalizeText(item.getSummary()) + "::" + normalizeText(item.getReferenceId());
    }

    private List<String> mergeTextRefs(List<String> preferred, List<String> fallback) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (String item : preferred == null ? List.<String>of() : preferred) {
            String normalized = normalizeText(item);
            if (normalized != null) {
                merged.add(normalized);
            }
        }
        for (String item : fallback == null ? List.<String>of() : fallback) {
            String normalized = normalizeText(item);
            if (normalized != null) {
                merged.add(normalized);
            }
        }
        return new ArrayList<>(merged);
    }

    private TaskReportVO.HumanReviewRecordVO toHumanReviewRecord(HumanReviewRecordDO record) {
        TaskReportVO.HumanReviewRecordVO vo = new TaskReportVO.HumanReviewRecordVO();
        vo.setReviewId(record.getReviewId());
        vo.setReviewerId(record.getReviewerId());
        vo.setReviewerRole(record.getReviewerRole());
        vo.setReviewResult(record.getReviewResult());
        vo.setReviewComment(record.getReviewComment());
        vo.setBeforeSnapshotRef(record.getBeforeSnapshotRef());
        vo.setAfterSnapshotRef(record.getAfterSnapshotRef());
        vo.setBeforeSnapshot(record.getBeforeSnapshot());
        vo.setAfterSnapshot(record.getAfterSnapshot());
        vo.setTraceId(record.getTraceId());
        vo.setCreatedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().toString());
        return vo;
    }

    private List<String> buildDomainRiskWarningMessages(RiskWarningDO warning) {
        LinkedHashSet<String> messages = new LinkedHashSet<>();
        if (warning == null) {
            return List.of();
        }
        String summary = normalizeText(warning.getWarningSummary());
        if (summary != null) {
            messages.add(summary);
        }
        String reason = normalizeText(warning.getWarningReason());
        if (reason != null) {
            for (String item : reason.split("\\R")) {
                if (item != null && !item.isBlank()) {
                    messages.add(item.trim());
                }
            }
        }
        return new ArrayList<>(messages);
    }

    private boolean isCurrentTaskReportCache(TaskReportVO cached) {
        return cached != null && cached.getReportId() != null && !cached.getReportId().isBlank();
    }

    private String resolveTaskReportType(ResearchReportDO report) {
        if (report.getReportType() != null && !report.getReportType().isBlank()) {
            return report.getReportType().trim();
        }
        if (report.getTaskType() != null && !report.getTaskType().isBlank()) {
            return report.getTaskType().trim();
        }
        return null;
    }

    private String resolveDisplaySummary(String preferredSummary, String fallbackSummary) {
        String normalizedPreferred = normalizeText(preferredSummary);
        return normalizedPreferred != null ? normalizedPreferred : normalizeText(fallbackSummary);
    }

    private List<String> resolveDisplayList(List<String> preferredItems, List<String> fallbackItems) {
        return preferredItems.isEmpty() ? fallbackItems : preferredItems;
    }

    private boolean hydrateTaskReportContextFields(TaskReportVO report) {
        if (report == null || !hasText(report.getRawPayload())) {
            return false;
        }

        JsonNode reportMetaNode = extractReportMetaNode(report.getRawPayload());
        if (reportMetaNode == null) {
            return false;
        }

        boolean changed = false;

        Map<String, Object> contextSnapshot = readObjectMap(reportMetaNode.get("contextSnapshot"));
        if (!contextSnapshot.isEmpty()) {
            Map<String, Object> mergedContextSnapshot = mergeObjectMap(report.getContextSnapshot(), contextSnapshot);
            if (!Objects.equals(normalizeObjectMap(report.getContextSnapshot()), mergedContextSnapshot)) {
                report.setContextSnapshot(mergedContextSnapshot);
                changed = true;
            }
        }

        if (report.getEvidenceRefs() == null || report.getEvidenceRefs().isEmpty()) {
            List<String> evidenceRefs = readTextList(reportMetaNode.get("evidenceRefs"));
            if (!evidenceRefs.isEmpty()) {
                report.setEvidenceRefs(evidenceRefs);
                changed = true;
            }
        }

        if (report.getEvidenceItems() == null || report.getEvidenceItems().isEmpty()) {
            List<TaskReportVO.ReportEvidenceItemVO> evidenceItems = readEvidenceItems(reportMetaNode.get("evidenceItems"));
            if (!evidenceItems.isEmpty()) {
                report.setEvidenceItems(evidenceItems);
                changed = true;
            }
        }

        if (!hasText(report.getReviewSuggestion())) {
            String reviewSuggestion = normalizeText(reportMetaNode.path("reviewSuggestion").asText(null));
            if (reviewSuggestion != null) {
                report.setReviewSuggestion(reviewSuggestion);
                changed = true;
            }
        }

        return changed;
    }

    private Map<String, Object> mergeObjectMap(Map<String, Object> current, Map<String, Object> latest) {
        Map<String, Object> merged = new java.util.LinkedHashMap<>(normalizeObjectMap(current));
        merged.putAll(normalizeObjectMap(latest));
        return merged;
    }

    private Map<String, Object> normalizeObjectMap(Map<String, Object> value) {
        return value == null ? Collections.emptyMap() : value;
    }

    private JsonNode extractReportMetaNode(String rawPayload) {
        if (!hasText(rawPayload)) {
            return null;
        }
        try {
            JsonNode payloadNode = objectMapper.readTree(rawPayload);
            JsonNode reportMetaNode = payloadNode.path("reportMeta");
            if (reportMetaNode.isMissingNode() || reportMetaNode.isNull() || !reportMetaNode.isObject()) {
                return null;
            }
            return reportMetaNode;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> readObjectMap(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException ignored) {
            return Collections.emptyMap();
        }
    }

    private List<String> readTextList(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String text = normalizeText(item.asText(null));
            if (text != null) {
                values.add(text);
            }
        });
        return values;
    }

    private List<TaskReportVO.ReportEvidenceItemVO> readEvidenceItems(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<TaskReportVO.ReportEvidenceItemVO> items = new ArrayList<>();
        node.forEach(item -> {
            if (item == null || item.isNull() || !item.isObject()) {
                return;
            }
            TaskReportVO.ReportEvidenceItemVO evidence = new TaskReportVO.ReportEvidenceItemVO();
            evidence.setEvidenceId(normalizeText(item.path("evidenceId").asText(null)));
            evidence.setEvidenceType(normalizeText(item.path("evidenceType").asText(null)));
            evidence.setSource(normalizeText(item.path("source").asText(null)));
            evidence.setTitle(normalizeText(item.path("title").asText(null)));
            evidence.setSummary(normalizeText(item.path("summary").asText(null)));
            evidence.setUrl(normalizeText(item.path("url").asText(null)));
            evidence.setOccurredAt(normalizeText(item.path("occurredAt").asText(null)));
            evidence.setReferenceId(normalizeText(item.path("referenceId").asText(null)));
            evidence.setRelevance(normalizeText(item.path("relevance").asText(null)));
            if (evidence.getEvidenceId() != null || evidence.getTitle() != null || evidence.getSummary() != null) {
                items.add(evidence);
            }
        });
        return items;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void enrichEventSourceConfigStats(EventSourceConfigVO eventSourceConfig) {
        if (eventSourceConfig == null || eventSourceConfig.getSources() == null || eventSourceConfig.getSources().isEmpty()) {
            return;
        }
        List<MarketEventIngestHistoryItemVO> histories = marketEventIngestHistoryService.loadRecentHistory();
        if (histories.isEmpty()) {
            return;
        }

        Map<String, List<MarketEventIngestHistoryItemVO>> grouped = histories.stream()
                .filter(item -> item.getSourceCode() != null && !item.getSourceCode().isBlank())
                .collect(Collectors.groupingBy(MarketEventIngestHistoryItemVO::getSourceCode, LinkedHashMap::new, Collectors.toList()));

        for (EventSourceConfigItemVO source : eventSourceConfig.getSources()) {
            if (source == null || source.getSourceCode() == null || source.getSourceCode().isBlank()) {
                continue;
            }
            List<MarketEventIngestHistoryItemVO> sourceHistories = grouped.get(source.getSourceCode());
            if (sourceHistories == null || sourceHistories.isEmpty()) {
                source.setIngestRecordCount(0);
                source.setTotalCount(0);
                source.setSuccessCount(0);
                source.setFailedCount(0);
                source.setDuplicateCount(0);
                source.setAutoTriggeredCount(0);
                source.setLastIngestAt(null);
                source.setLastResultStatus(null);
                source.setLastErrorMessage(null);
                continue;
            }
            MarketEventIngestHistoryItemVO latestHistory = sourceHistories.get(0);
            source.setIngestRecordCount(sourceHistories.size());
            source.setTotalCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getTotalCount())).sum());
            source.setSuccessCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getSuccessCount())).sum());
            source.setFailedCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getFailedCount())).sum());
            source.setDuplicateCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getDuplicateCount())).sum());
            source.setAutoTriggeredCount(sourceHistories.stream().mapToInt(item -> defaultInt(item.getAutoTriggeredCount())).sum());
            source.setLastIngestAt(sourceHistories.stream()
                    .map(MarketEventIngestHistoryItemVO::getCreatedAt)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null));
            source.setLastResultStatus(latestHistory == null ? null : latestHistory.getResultStatus());
            source.setLastErrorMessage(latestHistory == null ? null : latestHistory.getErrorMessage());
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }

    private boolean shouldDisplayTaskErrorMessage(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return TaskStatusEnum.FAILED.name().equals(status) || TaskStatusEnum.CANCELLED.name().equals(status);
    }

    @Override
    public ReportReviewStatsVO getReportReviewStats() {
        ReportReviewStatsVO vo = new ReportReviewStatsVO();

        vo.setPendingCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .and(wrapper -> wrapper
                                .isNull(ResearchReportDO::getReviewStatus)
                                .or()
                                .eq(ResearchReportDO::getReviewStatus, ReportReviewStatusEnum.PENDING.name()))
        ));

        vo.setApprovedCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .eq(ResearchReportDO::getReviewStatus, ReportReviewStatusEnum.APPROVED.name())
        ));

        vo.setRejectedCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .eq(ResearchReportDO::getReviewStatus, ReportReviewStatusEnum.REJECTED.name())
        ));

        vo.setTotalReportCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
        ));

        return vo;
    }
}
