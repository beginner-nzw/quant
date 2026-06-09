package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.vo.RiskWarningPageVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningStatsVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.manager.ReportCenterProjectionManager;
import com.quant.aiorchestrator.manager.ReportReviewStatsManager;
import com.quant.aiorchestrator.manager.FollowUpTaskSummaryManager;
import com.quant.aiorchestrator.manager.ResearchWorkbenchProjectionManager;
import com.quant.aiorchestrator.manager.RiskWarningRuleManager;
import com.quant.aiorchestrator.manager.StrategySignalRuleManager;
import com.quant.aiorchestrator.manager.RiskWarningProjectionManager;
import com.quant.task.port.TaskCacheVersionPort;
import com.quant.aiorchestrator.manager.TaskReportProjectionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.mapper.AiAgentExecutionMapper;
import com.quant.aiorchestrator.mapper.AiWorkflowInstanceMapper;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskStepMapper;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.WorkflowConfigService;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.aiorchestrator.service.TaskReportService;
import com.quant.aiorchestrator.service.impl.ReportQueryServiceImpl;
import com.quant.aiorchestrator.service.impl.ResearchWorkbenchQueryServiceImpl;
import com.quant.aiorchestrator.service.impl.RiskQueryServiceImpl;
import com.quant.aiorchestrator.report.ReportCenterRiskProjection;
import com.quant.aiorchestrator.report.ReportCenterTaskProjection;
import com.quant.aiorchestrator.report.TaskReportRiskDetailProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjectionProvider;
import com.quant.aiorchestrator.report.TaskReportReadPort;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.task.risk.RiskWarningTaskProjection;
import com.quant.task.risk.RiskWarningTaskReadPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskQueryServiceRiskWarningTests {

    @Test
    void pageRiskWarningsMergesDomainAndLegacyFallbackWithoutDuplicateTasks() {
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);

        ResearchTaskDO domainTask = buildTask(
                "task-domain",
                "Domain risk task",
                "600000",
                "浦发银行",
                "HIGH",
                LocalDateTime.of(2026, 5, 7, 10, 0)
        );
        ResearchTaskDO legacyTask = buildTask(
                "task-legacy",
                "Legacy risk task",
                "000001",
                "平安银行",
                "MEDIUM",
                LocalDateTime.of(2026, 5, 6, 10, 0)
        );
        when(researchTaskMapper.selectList(any())).thenReturn(List.of(domainTask, legacyTask));

        ResearchReportDO domainReport = buildReport(
                "report-domain",
                "task-domain",
                "Domain report summary",
                "[\"旧报表风险\"]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 10, 5)
        );
        ResearchReportDO legacyReport = buildReport(
                "report-legacy",
                "task-legacy",
                "Legacy report summary",
                "[\"流动性承压\"]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 6, 10, 5)
        );
        when(researchReportMapper.selectList(any())).thenReturn(List.of(domainReport, legacyReport));

        RiskWarningDO domainWarning = buildRiskWarning(
                "warn-domain",
                "task-domain",
                RiskLevelEnum.HIGH.name(),
                "600000",
                "浦发银行",
                "高风险预警",
                "杠杆水平高",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 10, 10)
        );
        when(riskWarningMapper.selectList(any())).thenReturn(List.of(domainWarning));

        RiskWarningDetailDO detail = new RiskWarningDetailDO();
        detail.setWarningId("warn-domain");
        detail.setDetailDesc("负债率高于阈值");
        when(riskWarningDetailMapper.selectList(any())).thenReturn(List.of(detail));

        QueryServices service = newService(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper
        );

        RiskWarningPageVO page = service.pageRiskWarnings(new RiskWarningPageQueryDTO());

        assertEquals(2L, page.getTotal());
        assertIterableEquals(
                List.of("task-domain", "task-legacy"),
                page.getRecords().stream().map(item -> item.getTaskId()).toList()
        );
        assertEquals("高风险预警", page.getRecords().get(0).getSummary());
        assertEquals("Legacy report summary", page.getRecords().get(1).getSummary());
    }

    @Test
    void pageRiskWarningsDoesNotLeakLegacyFallbackForTaskAlreadyCoveredByDomainWarning() {
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);

        ResearchTaskDO domainTask = buildTask(
                "task-domain",
                "Domain risk task",
                "600000",
                "浦发银行",
                "HIGH",
                LocalDateTime.of(2026, 5, 7, 10, 0)
        );
        when(researchTaskMapper.selectList(any())).thenReturn(List.of(domainTask));
        when(researchReportMapper.selectList(any())).thenReturn(List.of(buildReport(
                "report-domain",
                "task-domain",
                "Domain report summary",
                "[\"旧报表风险\"]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 10, 5)
        )));
        when(riskWarningMapper.selectList(any())).thenReturn(List.of(buildRiskWarning(
                "warn-domain",
                "task-domain",
                RiskLevelEnum.HIGH.name(),
                "600000",
                "浦发银行",
                "高风险预警",
                "杠杆水平高",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 10, 10)
        )));
        when(riskWarningDetailMapper.selectList(any())).thenReturn(List.of());

        QueryServices service = newService(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper
        );

        RiskWarningPageQueryDTO queryDTO = new RiskWarningPageQueryDTO();
        queryDTO.setRiskLevel(RiskLevelEnum.LOW.name());
        RiskWarningPageVO page = service.pageRiskWarnings(queryDTO);

        assertEquals(0L, page.getTotal());
    }

    @Test
    void getRiskWarningStatsCountsDomainAndLegacyRecordsTogether() {
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);

        when(researchTaskMapper.selectList(any())).thenReturn(List.of(
                buildTask("task-domain", "Domain risk task", "600000", "浦发银行", "HIGH", LocalDateTime.of(2026, 5, 7, 10, 0)),
                buildTask("task-legacy", "Legacy risk task", "000001", "平安银行", "MEDIUM", LocalDateTime.of(2026, 5, 6, 10, 0))
        ));
        when(researchReportMapper.selectList(any())).thenReturn(List.of(
                buildReport(
                        "report-domain",
                        "task-domain",
                        "Domain report summary",
                        "[\"旧报表风险\"]",
                        "[]",
                        0,
                        ReportReviewStatusEnum.APPROVED.name(),
                        LocalDateTime.of(2026, 5, 7, 10, 5)
                ),
                buildReport(
                        "report-legacy",
                        "task-legacy",
                        "Legacy report summary",
                        "[\"流动性承压\"]",
                        "[]",
                        0,
                        ReportReviewStatusEnum.APPROVED.name(),
                        LocalDateTime.of(2026, 5, 6, 10, 5)
                )
        ));
        when(riskWarningMapper.selectList(any())).thenReturn(List.of(buildRiskWarning(
                "warn-domain",
                "task-domain",
                RiskLevelEnum.HIGH.name(),
                "600000",
                "浦发银行",
                "高风险预警",
                "杠杆水平高",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 10, 10)
        )));
        when(riskWarningDetailMapper.selectList(any())).thenReturn(List.of());

        QueryServices service = newService(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper
        );

        RiskWarningStatsVO stats = service.getRiskWarningStats();

        assertEquals(2L, stats.getTotalCount());
        assertEquals(1L, stats.getHighCount());
        assertEquals(0L, stats.getMediumCount());
        assertEquals(1L, stats.getLowCount());
        assertEquals(1L, stats.getPendingReviewCount());
        assertEquals(1L, stats.getHumanReviewCount());
    }

    @Test
    void getResearchWorkbenchPrefersDomainRiskForLatestInsightAndDisposition() {
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);

        ResearchTaskDO task = buildTask(
                "task-domain",
                "Domain insight task",
                "600000",
                "浦发银行",
                "HIGH",
                LocalDateTime.of(2026, 5, 7, 11, 0)
        );
        when(researchTaskMapper.selectList(any())).thenReturn(List.of(task));

        ResearchReportDO report = buildReport(
                "report-domain",
                "task-domain",
                "最新投研摘要",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 11, 5)
        );
        report.setHighlights("[\"经营稳健\"]");
        when(researchReportMapper.selectList(any())).thenReturn(List.of(report));

        RiskWarningDO warning = buildRiskWarning(
                "warn-domain",
                "task-domain",
                RiskLevelEnum.HIGH.name(),
                "600000",
                "浦发银行",
                "高风险预警",
                "债务集中到期",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 11, 6)
        );
        when(riskWarningMapper.selectList(any())).thenReturn(List.of(warning));

        RiskWarningDetailDO detail = new RiskWarningDetailDO();
        detail.setWarningId("warn-domain");
        detail.setDetailDesc("未来三个月存在集中偿付压力");
        when(riskWarningDetailMapper.selectList(any())).thenReturn(List.of(detail));

        QueryServices service = newService(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper
        );

        ResearchWorkbenchQueryDTO queryDTO = new ResearchWorkbenchQueryDTO();
        queryDTO.setTargetCode("600000");
        ResearchWorkbenchVO workbench = service.getResearchWorkbench(queryDTO);

        assertEquals(RiskLevelEnum.HIGH.name(), workbench.getLatestInsight().getRiskLevel());
        assertEquals(Boolean.TRUE, workbench.getLatestInsight().getNeedHumanReview());
        assertIterableEquals(
                List.of("未来三个月存在集中偿付压力", "债务集中到期"),
                workbench.getLatestInsight().getRiskPoints()
        );
        assertEquals(1L, workbench.getRiskDispositionSummary().getTotalCount());
        assertEquals(1L, workbench.getRiskDispositionSummary().getNotTrackedCount());
    }

    @Test
    void getResearchWorkbenchRiskDispositionMergesDomainAndLegacyFallback() {
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);

        ResearchTaskDO domainTask = buildTask(
                "task-domain",
                "Domain task",
                "600000",
                "浦发银行",
                "HIGH",
                LocalDateTime.of(2026, 5, 7, 11, 0)
        );
        ResearchTaskDO legacyTask = buildTask(
                "task-legacy",
                "Legacy task",
                "600000",
                "浦发银行",
                "MEDIUM",
                LocalDateTime.of(2026, 5, 6, 11, 0)
        );
        when(researchTaskMapper.selectList(any())).thenReturn(List.of(domainTask, legacyTask));

        ResearchReportDO domainReport = buildReport(
                "report-domain",
                "task-domain",
                "Domain report",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 11, 5)
        );
        ResearchReportDO legacyReport = buildReport(
                "report-legacy",
                "task-legacy",
                "Legacy report",
                "[\"流动性波动\"]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 6, 11, 5)
        );
        when(researchReportMapper.selectList(any())).thenReturn(List.of(domainReport, legacyReport));

        when(riskWarningMapper.selectList(any())).thenReturn(List.of(buildRiskWarning(
                "warn-domain",
                "task-domain",
                RiskLevelEnum.HIGH.name(),
                "600000",
                "浦发银行",
                "高风险预警",
                "债务集中到期",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 11, 6)
        )));
        when(riskWarningDetailMapper.selectList(any())).thenReturn(List.of());

        QueryServices service = newService(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper
        );

        ResearchWorkbenchQueryDTO queryDTO = new ResearchWorkbenchQueryDTO();
        queryDTO.setTargetCode("600000");
        ResearchWorkbenchVO workbench = service.getResearchWorkbench(queryDTO);

        assertEquals(2L, workbench.getRiskDispositionSummary().getTotalCount());
        assertEquals(2L, workbench.getRiskDispositionSummary().getNotTrackedCount());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getTaskReportOnlyPrefersDomainRiskWarnings() {
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);

        ResearchReportDO report = buildReport(
                "report-domain",
                "task-domain",
                "Domain report",
                "[\"旧报表预警\"]",
                "[\"旧报表风险点\"]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 12, 0)
        );
        when(researchReportMapper.selectOne(any())).thenReturn(report);
        when(riskWarningMapper.selectList(any())).thenReturn(List.of(buildRiskWarning(
                "warn-domain",
                "task-domain",
                RiskLevelEnum.HIGH.name(),
                "600000",
                "浦发银行",
                "高风险预警",
                "债务集中到期\n短期偿付承压",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 12, 1)
        )));
        when(riskWarningDetailMapper.selectList(any())).thenReturn(List.of());

        QueryServices service = newService(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper,
                stringRedisTemplate
        );

        var result = service.getTaskReportOnly("task-domain");

        assertEquals(Boolean.TRUE, result.getNeedHumanReview());
        assertIterableEquals(List.of("高风险预警", "债务集中到期", "短期偿付承压"), result.getRiskWarnings());
        assertTrue(result.getOriginalRiskPoints().contains("旧报表风险点"));
    }

    private QueryServices newService(ResearchTaskMapper researchTaskMapper,
                                     ResearchReportMapper researchReportMapper,
                                     RiskWarningMapper riskWarningMapper,
                                     RiskWarningDetailMapper riskWarningDetailMapper) {
        return newService(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper,
                mock(StringRedisTemplate.class)
        );
    }

    private QueryServices newService(ResearchTaskMapper researchTaskMapper,
                                     ResearchReportMapper researchReportMapper,
                                     RiskWarningMapper riskWarningMapper,
                                     RiskWarningDetailMapper riskWarningDetailMapper,
                                     StringRedisTemplate stringRedisTemplate) {
        ObjectMapper objectMapper = new ObjectMapper();
        StrategySignalMapper strategySignalMapper = mock(StrategySignalMapper.class);
        StrategySignalFactorMapper strategySignalFactorMapper = mock(StrategySignalFactorMapper.class);
        RiskWarningProjectionManager riskWarningProjectionManager = new RiskWarningProjectionManager(
                taskReadPort(researchTaskMapper),
                taskReportReadPort(researchReportMapper),
                riskWarningMapper,
                riskWarningDetailMapper,
                new com.quant.aiorchestrator.manager.RiskWarningFollowUpSummaryManager(),
                new RiskWarningRuleManager(objectMapper)
        );
        return new QueryServices(
                new RiskQueryServiceImpl(riskWarningProjectionManager),
                new ResearchWorkbenchQueryServiceImpl(
                        newResearchWorkbenchProjectionManager(
                                researchTaskMapper,
                                researchReportMapper,
                                riskWarningMapper,
                                riskWarningDetailMapper,
                                strategySignalMapper,
                                objectMapper
                        )
                ),
                new ReportQueryServiceImpl(
                        new ReportCenterProjectionManager(
                                taskIds -> toReportCenterTaskMap(researchTaskMapper.selectList(null)),
                                new com.quant.aiorchestrator.manager.ReportTaskPageReadManager(researchReportMapper),
                                taskIds -> toReportCenterRiskMap(riskWarningMapper.selectList(null)),
                                objectMapper
                        ),
                        new TaskReportProjectionManager(
                                new com.quant.aiorchestrator.manager.ReportTaskPageReadManager(researchReportMapper),
                                stringRedisTemplate,
                                objectMapper,
                                new com.quant.aiorchestrator.manager.TaskReportDomainHydrationManager(
                                        mock(ReportEvidenceRefMapper.class),
                                        reportId -> List.of(),
                                        mock(ResearchReportSectionMapper.class),
                                        objectMapper
                                ),
                                taskReportRiskProvider(riskWarningMapper, riskWarningDetailMapper)
                        ),
                        new ReportReviewStatsManager(researchReportMapper),
                        mock(TaskReportService.class),
                        mock(ReportVersionService.class)
                )
        );
    }

    private ResearchWorkbenchProjectionManager newResearchWorkbenchProjectionManager(ResearchTaskMapper researchTaskMapper,
                                                                                    ResearchReportMapper researchReportMapper,
                                                                                    RiskWarningMapper riskWarningMapper,
                                                                                    RiskWarningDetailMapper riskWarningDetailMapper,
                                                                                    StrategySignalMapper strategySignalMapper,
                                                                                    ObjectMapper objectMapper) {
        var taskQueryReadManager = new com.quant.aiorchestrator.manager.TaskQueryReadManager(researchTaskMapper, null, null);
        var workbenchReadManager = new com.quant.aiorchestrator.manager.ResearchWorkbenchReadManager(
                taskQueryReadManager,
                new com.quant.task.workbench.ResearchWorkbenchRiskProvider() {
                    @Override
                    public Map<String, com.quant.task.workbench.ResearchWorkbenchRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds) {
                        return riskWarningMapper.selectList(null).stream()
                                .filter(item -> taskIds == null || taskIds.contains(item.getTaskId()))
                                .collect(Collectors.toMap(
                                        RiskWarningDO::getTaskId,
                                        item -> new com.quant.task.workbench.ResearchWorkbenchRiskProjection(
                                                item.getTaskId(),
                                                item.getWarningId(),
                                                item.getWarningLevel(),
                                                item.getWarningSummary(),
                                                item.getWarningReason(),
                                                item.getSuggestAction(),
                                                item.getReviewStatus(),
                                                item.getReviewerId(),
                                                item.getReviewTime(),
                                                item.getCreatedAt()
                                        ),
                                        (left, right) -> left
                                ));
                    }

                    @Override
                    public Map<String, List<com.quant.task.workbench.ResearchWorkbenchRiskDetailProjection>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds) {
                        return riskWarningDetailMapper.selectList(null).stream()
                                .filter(item -> warningIds == null || warningIds.contains(item.getWarningId()))
                                .collect(Collectors.groupingBy(
                                        RiskWarningDetailDO::getWarningId,
                                        Collectors.mapping(
                                                item -> new com.quant.task.workbench.ResearchWorkbenchRiskDetailProjection(
                                                        item.getWarningId(),
                                                        item.getDetailDesc(),
                                                        item.getIndicatorName(),
                                                        item.getIndicatorValue()
                                                ),
                                                Collectors.toList()
                                        )
                                ));
                    }
                },
                taskIds -> strategySignalMapper.selectList(null).stream()
                        .filter(item -> taskIds == null || taskIds.contains(item.getTaskId()))
                        .collect(Collectors.toMap(
                                com.quant.aiorchestrator.domain.entity.StrategySignalDO::getTaskId,
                                item -> new com.quant.task.workbench.ResearchWorkbenchStrategyProjection(
                                        item.getTaskId(),
                                        item.getSignalDirection(),
                                        item.getSignalLevel(),
                                        item.getSignalScore(),
                                        item.getConfidenceScore(),
                                        item.getReasonSummary()
                                ),
                                (left, right) -> left
                        ))
        );
        var workbenchRuleManager = new com.quant.aiorchestrator.manager.ResearchWorkbenchRuleManager(objectMapper);
        return new ResearchWorkbenchProjectionManager(
                taskQueryReadManager,
                taskReportReadPort(researchReportMapper),
                workbenchReadManager,
                new com.quant.aiorchestrator.manager.ResearchWorkbenchDispositionManager(
                        workbenchReadManager,
                        new FollowUpTaskSummaryManager(),
                        workbenchRuleManager
                ),
                workbenchRuleManager,
                new com.quant.aiorchestrator.manager.ResearchWorkbenchItemAssembler(workbenchRuleManager)
        );
    }

    private static Map<String, ReportCenterTaskProjection> toReportCenterTaskMap(List<ResearchTaskDO> tasks) {
        if (tasks == null) {
            return Map.of();
        }
        return tasks.stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                task -> new ReportCenterTaskProjection(
                        task.getTaskId(),
                        task.getTaskTitle(),
                        task.getTaskType(),
                        task.getTargetCode(),
                        task.getTargetName(),
                        task.getPriority(),
                        task.getCreatedAt()
                ),
                (left, right) -> left
        ));
    }

    private static TaskReportReadPort taskReportReadPort(ResearchReportMapper researchReportMapper) {
        return new com.quant.aiorchestrator.manager.ReportTaskPageReadManager(researchReportMapper);
    }

    private static RiskWarningTaskReadPort taskReadPort(ResearchTaskMapper researchTaskMapper) {
        return new RiskWarningTaskReadPort() {
            @Override
            public Map<String, RiskWarningTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds) {
                List<ResearchTaskDO> tasks = researchTaskMapper.selectList(null);
                if (tasks == null) {
                    return Map.of();
                }
                return tasks.stream()
                        .filter(task -> taskIds == null || taskIds.contains(task.getTaskId()))
                        .collect(Collectors.toMap(
                                ResearchTaskDO::getTaskId,
                                TaskQueryServiceRiskWarningTests::toTaskProjection,
                                (left, right) -> left
                        ));
            }

            @Override
            public List<RiskWarningTaskProjection> loadRiskWarningFollowUpTasks() {
                return loadFollowUpTasksBySourceDomain("RISK_WARNING");
            }

            @Override
            public List<RiskWarningTaskProjection> loadFollowUpTasksBySourceDomain(String sourceDomain) {
                List<ResearchTaskDO> tasks = researchTaskMapper.selectList(null);
                if (tasks == null) {
                    return List.of();
                }
                return tasks.stream()
                        .filter(task -> sourceDomain == null || sourceDomain.equals(task.getSourceDomain()))
                        .map(TaskQueryServiceRiskWarningTests::toTaskProjection)
                        .toList();
            }
        };
    }

    private static RiskWarningTaskProjection toTaskProjection(ResearchTaskDO task) {
        return new RiskWarningTaskProjection(
                task.getId(),
                task.getTaskId(),
                task.getTaskType(),
                task.getTaskTitle(),
                task.getTargetCode(),
                task.getTargetName(),
                task.getPriority(),
                task.getStatus(),
                task.getCurrentStage(),
                task.getSourceTaskId(),
                task.getSourceReportId(),
                task.getSourceDomain(),
                task.getCreatedAt()
        );
    }

    private static Map<String, ReportCenterRiskProjection> toReportCenterRiskMap(List<RiskWarningDO> risks) {
        if (risks == null) {
            return Map.of();
        }
        return risks.stream().collect(Collectors.toMap(
                RiskWarningDO::getTaskId,
                risk -> new ReportCenterRiskProjection(
                        risk.getTaskId(),
                        risk.getWarningLevel(),
                        risk.getSuggestAction(),
                        risk.getReviewStatus(),
                        risk.getCreatedAt()
                ),
                (left, right) -> left
        ));
    }

    private static TaskReportRiskProjectionProvider taskReportRiskProvider(RiskWarningMapper riskWarningMapper,
                                                                           RiskWarningDetailMapper riskWarningDetailMapper) {
        return new TaskReportRiskProjectionProvider() {
            @Override
            public Map<String, TaskReportRiskProjection> loadLatestRiskWarningMapByTaskIds(java.util.Set<String> taskIds) {
                return toTaskReportRiskMap(riskWarningMapper.selectList(null));
            }

            @Override
            public Map<String, List<TaskReportRiskDetailProjection>> loadRiskWarningDetailMapByWarningIds(java.util.Set<String> warningIds) {
                List<RiskWarningDetailDO> details = riskWarningDetailMapper.selectList(null);
                if (details == null) {
                    return Map.of();
                }
                return details.stream().collect(Collectors.groupingBy(
                        RiskWarningDetailDO::getWarningId,
                        Collectors.mapping(
                                detail -> new TaskReportRiskDetailProjection(detail.getWarningId(), detail.getDetailDesc()),
                                Collectors.toList()
                        )
                ));
            }
        };
    }

    private static Map<String, TaskReportRiskProjection> toTaskReportRiskMap(List<RiskWarningDO> risks) {
        if (risks == null) {
            return Map.of();
        }
        return risks.stream().collect(Collectors.toMap(
                RiskWarningDO::getTaskId,
                risk -> new TaskReportRiskProjection(
                        risk.getWarningId(),
                        risk.getTaskId(),
                        risk.getWarningLevel(),
                        risk.getWarningSummary(),
                        risk.getWarningReason(),
                        risk.getSuggestAction(),
                        risk.getReviewStatus()
                ),
                (left, right) -> left
        ));
    }

    private static final class QueryServices {
        private final RiskQueryServiceImpl riskQueryService;
        private final ResearchWorkbenchQueryServiceImpl researchWorkbenchQueryService;
        private final ReportQueryServiceImpl reportQueryService;

        private QueryServices(RiskQueryServiceImpl riskQueryService,
                              ResearchWorkbenchQueryServiceImpl researchWorkbenchQueryService,
                              ReportQueryServiceImpl reportQueryService) {
            this.riskQueryService = riskQueryService;
            this.researchWorkbenchQueryService = researchWorkbenchQueryService;
            this.reportQueryService = reportQueryService;
        }

        private RiskWarningPageVO pageRiskWarnings(RiskWarningPageQueryDTO queryDTO) {
            return riskQueryService.pageRiskWarnings(queryDTO);
        }

        private RiskWarningStatsVO getRiskWarningStats() {
            return riskQueryService.getRiskWarningStats();
        }

        private ResearchWorkbenchVO getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO) {
            return researchWorkbenchQueryService.getResearchWorkbench(queryDTO);
        }

        private TaskReportVO getTaskReportOnly(String taskId) {
            return reportQueryService.getTaskReportOnly(taskId);
        }
    }

    private ResearchTaskDO buildTask(String taskId,
                                     String title,
                                     String targetCode,
                                     String targetName,
                                     String priority,
                                     LocalDateTime createdAt) {
        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId(taskId);
        task.setTaskTitle(title);
        task.setTaskType("RISK_ANALYSIS");
        task.setTargetCode(targetCode);
        task.setTargetName(targetName);
        task.setPriority(priority);
        task.setStatus(TaskStatusEnum.SUCCESS.name());
        task.setCurrentStage("FINISHED");
        task.setCreatedAt(createdAt);
        return task;
    }

    private ResearchReportDO buildReport(String reportId,
                                         String taskId,
                                         String summary,
                                         String riskWarnings,
                                         String riskPoints,
                                         Integer needHumanReview,
                                         String reviewStatus,
                                         LocalDateTime createdAt) {
        ResearchReportDO report = new ResearchReportDO();
        report.setReportId(reportId);
        report.setTaskId(taskId);
        report.setReportType("RISK_ANALYSIS");
        report.setFinalStatus(TaskStatusEnum.SUCCESS.name());
        report.setSummary(summary);
        report.setRiskWarnings(riskWarnings);
        report.setRiskPoints(riskPoints);
        report.setNeedHumanReview(needHumanReview);
        report.setReviewStatus(reviewStatus);
        report.setCreatedAt(createdAt);
        return report;
    }

    private RiskWarningDO buildRiskWarning(String warningId,
                                           String taskId,
                                           String warningLevel,
                                           String entityCode,
                                           String entityName,
                                           String warningSummary,
                                           String warningReason,
                                           String suggestAction,
                                           String reviewStatus,
                                           LocalDateTime createdAt) {
        RiskWarningDO warning = new RiskWarningDO();
        warning.setWarningId(warningId);
        warning.setTaskId(taskId);
        warning.setWarningType("LEVERAGE");
        warning.setWarningLevel(warningLevel);
        warning.setEntityType("STOCK");
        warning.setEntityCode(entityCode);
        warning.setEntityName(entityName);
        warning.setTriggerSource("AI_RESULT");
        warning.setWarningSummary(warningSummary);
        warning.setWarningReason(warningReason);
        warning.setSuggestAction(suggestAction);
        warning.setReviewStatus(reviewStatus);
        warning.setCreatedAt(createdAt);
        return warning;
    }
}
