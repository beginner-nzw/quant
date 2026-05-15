package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchInsightVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.mapper.AiAgentExecutionMapper;
import com.quant.aiorchestrator.mapper.AiWorkflowInstanceMapper;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskStepMapper;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
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
import com.quant.aiorchestrator.service.impl.TaskQueryServiceImpl;
import com.quant.common.model.enums.MarketIntelligenceTypeEnum;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskQueryServiceRiskProjectionTests {

    @Test
    void pageReportCenterPrefersDomainRiskHumanReviewFlag() {
        TestDeps deps = new TestDeps();
        ResearchTaskDO task = buildTask("task-report", "Report Center Task", "600000", "PF Bank", "HIGH", LocalDateTime.of(2026, 5, 7, 13, 0));
        ResearchReportDO report = buildReport(
                "report-1",
                "task-report",
                "report summary",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 13, 5)
        );

        when(deps.researchTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(deps.researchReportMapper.selectList(any())).thenReturn(List.of(report));
        when(deps.riskWarningMapper.selectList(any())).thenReturn(List.of(buildRiskWarning(
                "warning-1",
                "task-report",
                RiskLevelEnum.HIGH.name(),
                "600000",
                "PF Bank",
                "domain warning",
                "liquidity pressure",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 13, 6)
        )));

        TaskQueryServiceImpl service = newService(deps);

        var page = service.pageReportCenter(new ReportCenterPageQueryDTO());

        assertEquals(Boolean.TRUE, page.getRecords().get(0).getNeedHumanReview());
        assertEquals(1L, service.getReportCenterStats().getHumanReviewCount());
    }

    @Test
    void pageStrategySignalsFallbackPrefersDomainRiskProjection() {
        TestDeps deps = new TestDeps();
        ResearchTaskDO task = buildTask("task-strategy-fallback", "Strategy Fallback Task", "000001", "PA Bank", "MEDIUM", LocalDateTime.of(2026, 5, 7, 14, 0));
        ResearchReportDO report = buildReport(
                "report-2",
                "task-strategy-fallback",
                "positive momentum",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 14, 5)
        );
        report.setConfidenceScore(new BigDecimal("0.92"));

        when(deps.researchTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(deps.researchReportMapper.selectList(any())).thenReturn(List.of(report));
        when(deps.riskWarningMapper.selectList(any())).thenReturn(List.of(buildRiskWarning(
                "warning-2",
                "task-strategy-fallback",
                RiskLevelEnum.HIGH.name(),
                "000001",
                "PA Bank",
                "domain risk",
                "short-term pressure",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 14, 6)
        )));
        when(deps.riskWarningDetailMapper.selectList(any())).thenReturn(List.of(buildRiskWarningDetail("warning-2", "cash flow risk")));

        TaskQueryServiceImpl service = newService(deps);

        var page = service.pageStrategySignals(new StrategySignalPageQueryDTO());
        var record = page.getRecords().get(0);

        assertEquals(Boolean.TRUE, record.getNeedHumanReview());
        assertEquals(SignalDirectionEnum.NEGATIVE.name(), record.getSignalDirection());
    }

    @Test
    void pageStrategySignalsDomainRecordsUseDomainRiskHumanReview() {
        TestDeps deps = new TestDeps();
        ResearchTaskDO task = buildTask("task-strategy-domain", "Strategy Domain Task", "600519", "Kweichow", "HIGH", LocalDateTime.of(2026, 5, 7, 15, 0));
        ResearchReportDO report = buildReport(
                "report-3",
                "task-strategy-domain",
                "positive outlook",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 15, 5)
        );
        StrategySignalDO signal = buildStrategySignal(
                "signal-1",
                "task-strategy-domain",
                "600519",
                "Kweichow",
                "positive outlook",
                SignalDirectionEnum.POSITIVE.name(),
                88,
                LocalDateTime.of(2026, 5, 7, 15, 6)
        );

        when(deps.researchTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(deps.researchReportMapper.selectList(any())).thenReturn(List.of(report));
        when(deps.strategySignalMapper.selectCount(any())).thenReturn(1L);
        when(deps.strategySignalMapper.selectList(any())).thenReturn(List.of(signal));
        when(deps.riskWarningMapper.selectList(any())).thenReturn(List.of(buildRiskWarning(
                "warning-3",
                "task-strategy-domain",
                RiskLevelEnum.HIGH.name(),
                "600519",
                "Kweichow",
                "domain review needed",
                "valuation stress",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 15, 7)
        )));
        when(deps.riskWarningDetailMapper.selectList(any())).thenReturn(List.of(buildRiskWarningDetail("warning-3", "valuation stress")));

        TaskQueryServiceImpl service = newService(deps);

        var page = service.pageStrategySignals(new StrategySignalPageQueryDTO());
        var record = page.getRecords().get(0);

        assertEquals(Boolean.TRUE, record.getNeedHumanReview());
        assertTrue(record.getSignalSourceTags().contains("HUMAN_REVIEW"));
    }

    @Test
    void pageStrategySignalsMergesDomainAndLegacyFallbackWithoutDuplicateTasks() {
        TestDeps deps = new TestDeps();
        ResearchTaskDO domainTask = buildTask("task-domain-signal", "Domain Signal Task", "600519", "Kweichow", "HIGH", LocalDateTime.of(2026, 5, 7, 15, 0));
        ResearchTaskDO legacyTask = buildTask("task-legacy-signal", "Legacy Signal Task", "000001", "PA Bank", "MEDIUM", LocalDateTime.of(2026, 5, 7, 14, 0));
        ResearchReportDO domainReport = buildReport(
                "report-domain-signal",
                "task-domain-signal",
                "old domain report signal",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 15, 5)
        );
        ResearchReportDO legacyReport = buildReport(
                "report-legacy-signal",
                "task-legacy-signal",
                "positive legacy momentum",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 14, 5)
        );
        legacyReport.setConfidenceScore(new BigDecimal("0.86"));
        StrategySignalDO domainSignal = buildStrategySignal(
                "signal-domain",
                "task-domain-signal",
                "600519",
                "Kweichow",
                "domain signal",
                SignalDirectionEnum.POSITIVE.name(),
                92,
                LocalDateTime.of(2026, 5, 7, 15, 6)
        );

        when(deps.researchTaskMapper.selectList(any())).thenReturn(List.of(domainTask, legacyTask));
        when(deps.researchReportMapper.selectList(any())).thenReturn(List.of(domainReport, legacyReport));
        when(deps.strategySignalMapper.selectList(any())).thenReturn(List.of(domainSignal));

        TaskQueryServiceImpl service = newService(deps);

        var page = service.pageStrategySignals(new StrategySignalPageQueryDTO());

        assertEquals(2L, page.getTotal());
        assertEquals(
                List.of("task-domain-signal", "task-legacy-signal"),
                page.getRecords().stream().map(item -> item.getTaskId()).toList()
        );
        assertEquals(2L, service.getStrategySignalStats().getTotalCount());
    }

    @Test
    void pageStrategySignalsDoesNotLeakLegacyFallbackForTaskAlreadyCoveredByDomainSignal() {
        TestDeps deps = new TestDeps();
        ResearchTaskDO task = buildTask("task-covered-signal", "Covered Signal Task", "300750", "CATL", "HIGH", LocalDateTime.of(2026, 5, 7, 16, 0));
        ResearchReportDO report = buildReport(
                "report-covered-signal",
                "task-covered-signal",
                "negative legacy fallback",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 16, 5)
        );
        report.setConfidenceScore(new BigDecimal("0.90"));
        StrategySignalDO signal = buildStrategySignal(
                "signal-covered",
                "task-covered-signal",
                "300750",
                "CATL",
                "domain positive signal",
                SignalDirectionEnum.POSITIVE.name(),
                91,
                LocalDateTime.of(2026, 5, 7, 16, 6)
        );

        when(deps.researchTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(deps.researchReportMapper.selectList(any())).thenReturn(List.of(report));
        when(deps.strategySignalMapper.selectList(any())).thenReturn(List.of(signal));

        TaskQueryServiceImpl service = newService(deps);

        var page = service.pageStrategySignals(new StrategySignalPageQueryDTO());

        assertEquals(1L, page.getTotal());
        assertEquals("task-covered-signal", page.getRecords().get(0).getTaskId());
        assertEquals(SignalDirectionEnum.POSITIVE.name(), page.getRecords().get(0).getSignalDirection());
    }

    @Test
    void pageMarketIntelligencePrefersDomainRiskProjection() {
        TestDeps deps = new TestDeps();
        ResearchTaskDO task = buildTask("task-intelligence", "Intelligence Task", "300750", "CATL", "HIGH", LocalDateTime.of(2026, 5, 7, 16, 0));
        ResearchReportDO report = buildReport(
                "report-4",
                "task-intelligence",
                "positive catalyst",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 16, 5)
        );
        report.setConfidenceScore(new BigDecimal("0.90"));

        when(deps.researchTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(deps.researchReportMapper.selectList(any())).thenReturn(List.of(report));
        when(deps.riskWarningMapper.selectList(any())).thenReturn(List.of(buildRiskWarning(
                "warning-4",
                "task-intelligence",
                RiskLevelEnum.HIGH.name(),
                "300750",
                "CATL",
                "domain risk",
                "concentration exposure",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 16, 6)
        )));
        when(deps.riskWarningDetailMapper.selectList(any())).thenReturn(List.of(buildRiskWarningDetail("warning-4", "supply chain pressure")));

        TaskQueryServiceImpl service = newService(deps);

        var page = service.pageMarketIntelligence(new MarketIntelligencePageQueryDTO());
        var record = page.getRecords().get(0);

        assertEquals(Boolean.TRUE, record.getNeedHumanReview());
        assertEquals(RiskLevelEnum.HIGH.name(), record.getRiskLevel());
        assertEquals(MarketIntelligenceTypeEnum.RISK_ALERT.name(), record.getIntelligenceType());
    }

    @Test
    void pageMarketIntelligencePrefersDomainStrategySignalWhenPresent() {
        TestDeps deps = new TestDeps();
        ResearchTaskDO task = buildTask("task-intelligence-signal", "Signal Intelligence Task", "600519", "Kweichow", "HIGH", LocalDateTime.of(2026, 5, 7, 17, 0));
        ResearchReportDO report = buildReport(
                "report-intelligence-signal",
                "task-intelligence-signal",
                "neutral note",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 17, 5)
        );
        report.setConfidenceScore(new BigDecimal("0.55"));
        StrategySignalDO signal = buildStrategySignal(
                "signal-intelligence",
                "task-intelligence-signal",
                "600519",
                "Kweichow",
                "domain strategy signal",
                SignalDirectionEnum.POSITIVE.name(),
                95,
                LocalDateTime.of(2026, 5, 7, 17, 6)
        );

        when(deps.researchTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(deps.researchReportMapper.selectList(any())).thenReturn(List.of(report));
        when(deps.strategySignalMapper.selectList(any())).thenReturn(List.of(signal));

        TaskQueryServiceImpl service = newService(deps);

        var page = service.pageMarketIntelligence(new MarketIntelligencePageQueryDTO());
        var record = page.getRecords().get(0);

        assertEquals(MarketIntelligenceTypeEnum.STRATEGY_SIGNAL.name(), record.getIntelligenceType());
        assertEquals(SignalDirectionEnum.POSITIVE.name(), record.getSignalDirection());
        assertEquals(0.91d, record.getConfidenceScore(), 0.0001d);
    }

    @Test
    void getResearchWorkbenchPrefersDomainStrategySignalForInsightAndDisposition() {
        TestDeps deps = new TestDeps();
        ResearchTaskDO signalTask = buildTask("task-workbench-signal", "Workbench Signal Task", "600519", "Kweichow", "HIGH", LocalDateTime.of(2026, 5, 7, 18, 0));
        ResearchTaskDO fallbackTask = buildTask("task-workbench-fallback", "Workbench Fallback Task", "600519", "Kweichow", "MEDIUM", LocalDateTime.of(2026, 5, 7, 17, 0));
        ResearchReportDO signalReport = buildReport(
                "report-workbench-signal",
                "task-workbench-signal",
                "negative pressure outlook",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 18, 5)
        );
        signalReport.setConfidenceScore(new BigDecimal("0.45"));
        ResearchReportDO fallbackReport = buildReport(
                "report-workbench-fallback",
                "task-workbench-fallback",
                "stable neutral note",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.APPROVED.name(),
                LocalDateTime.of(2026, 5, 7, 17, 5)
        );
        StrategySignalDO signal = buildStrategySignal(
                "signal-workbench",
                "task-workbench-signal",
                "600519",
                "Kweichow",
                "domain positive signal",
                SignalDirectionEnum.POSITIVE.name(),
                94,
                LocalDateTime.of(2026, 5, 7, 18, 6)
        );

        when(deps.researchTaskMapper.selectList(any())).thenReturn(List.of(signalTask, fallbackTask));
        when(deps.researchReportMapper.selectList(any())).thenReturn(List.of(signalReport, fallbackReport));
        when(deps.strategySignalMapper.selectList(any())).thenReturn(List.of(signal));

        TaskQueryServiceImpl service = newService(deps);

        ResearchWorkbenchQueryDTO queryDTO = new ResearchWorkbenchQueryDTO();
        queryDTO.setTargetCode("600519");
        queryDTO.setTargetName("Kweichow");
        ResearchWorkbenchVO workbench = service.getResearchWorkbench(queryDTO);
        ResearchWorkbenchInsightVO latestInsight = workbench.getLatestInsight();

        assertEquals(SignalDirectionEnum.POSITIVE.name(), latestInsight.getSignalDirection());
        assertEquals("STRONG", latestInsight.getSignalStrength());
        assertEquals(2L, workbench.getStrategySignalDispositionSummary().getTotalCount());
        assertEquals(2L, workbench.getStrategySignalDispositionSummary().getNotTrackedCount());
    }

    @Test
    void pageAuditCompliancePrefersDomainRiskHumanReviewForInterception() {
        TestDeps deps = new TestDeps();
        ResearchTaskDO task = buildTask("task-audit", "Audit Task", "688111", "Kingsoft", "MEDIUM", LocalDateTime.of(2026, 5, 7, 17, 0));
        ResearchReportDO report = buildReport(
                "report-5",
                "task-audit",
                "neutral summary",
                "[]",
                "[]",
                0,
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 17, 5)
        );

        when(deps.researchTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(deps.researchReportMapper.selectList(any())).thenReturn(List.of(report));
        when(deps.riskWarningMapper.selectList(any())).thenReturn(List.of(buildRiskWarning(
                "warning-5",
                "task-audit",
                RiskLevelEnum.HIGH.name(),
                "688111",
                "Kingsoft",
                "domain audit risk",
                "manual review required",
                "NEED_HUMAN_REVIEW",
                ReportReviewStatusEnum.PENDING.name(),
                LocalDateTime.of(2026, 5, 7, 17, 6)
        )));

        TaskQueryServiceImpl service = newService(deps);

        var page = service.pageAuditCompliance(new AuditCompliancePageQueryDTO());
        var record = page.getRecords().get(0);

        assertEquals(Boolean.TRUE, record.getNeedHumanReview());
        assertEquals(Boolean.TRUE, record.getIntercepted());
    }

    private TaskQueryServiceImpl newService(TestDeps deps) {
        return new TaskQueryServiceImpl(
                deps.researchTaskMapper,
                deps.researchTaskStepMapper,
                deps.aiWorkflowInstanceMapper,
                deps.aiAgentExecutionMapper,
                deps.auditRecordMapper,
                deps.stringRedisTemplate,
                new ObjectMapper(),
                deps.researchTaskRetryLogMapper,
                mock(TaskCacheVersionManager.class),
                deps.researchReportMapper,
                mock(AgentConfigService.class),
                mock(ConfigChangeAuditService.class),
                mock(EventAutoTriggerConfigService.class),
                mock(MarketEventIngestHistoryService.class),
                mock(EventSourceConfigService.class),
                mock(ModelStrategyConfigService.class),
                mock(PromptTemplateConfigService.class),
                mock(WorkflowConfigService.class),
                mock(RoleAccessConfigService.class),
                deps.riskWarningMapper,
                deps.riskWarningDetailMapper,
                deps.strategySignalMapper,
                deps.strategySignalFactorMapper,
                mock(ReportEvidenceRefMapper.class),
                mock(HumanReviewRecordMapper.class),
                mock(ResearchReportSectionMapper.class),
                new TaskStateManager()
        );
    }

    private static ResearchTaskDO buildTask(String taskId,
                                            String title,
                                            String targetCode,
                                            String targetName,
                                            String priority,
                                            LocalDateTime createdAt) {
        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId(taskId);
        task.setTaskTitle(title);
        task.setTaskType("RESEARCH");
        task.setTargetCode(targetCode);
        task.setTargetName(targetName);
        task.setPriority(priority);
        task.setStatus(TaskStatusEnum.SUCCESS.name());
        task.setCurrentStage("FINISHED");
        task.setCreatedAt(createdAt);
        return task;
    }

    private static ResearchReportDO buildReport(String reportId,
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
        report.setTaskType("RESEARCH");
        report.setReportType("RESEARCH");
        report.setFinalStatus(TaskStatusEnum.SUCCESS.name());
        report.setSummary(summary);
        report.setRiskWarnings(riskWarnings);
        report.setRiskPoints(riskPoints);
        report.setNeedHumanReview(needHumanReview);
        report.setReviewStatus(reviewStatus);
        report.setCreatedAt(createdAt);
        return report;
    }

    private static RiskWarningDO buildRiskWarning(String warningId,
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
        warning.setWarningSummary(warningSummary);
        warning.setWarningReason(warningReason);
        warning.setSuggestAction(suggestAction);
        warning.setReviewStatus(reviewStatus);
        warning.setCreatedAt(createdAt);
        return warning;
    }

    private static RiskWarningDetailDO buildRiskWarningDetail(String warningId, String detailDesc) {
        RiskWarningDetailDO detail = new RiskWarningDetailDO();
        detail.setWarningId(warningId);
        detail.setDetailDesc(detailDesc);
        return detail;
    }

    private static StrategySignalDO buildStrategySignal(String signalId,
                                                        String taskId,
                                                        String entityCode,
                                                        String entityName,
                                                        String reasonSummary,
                                                        String signalDirection,
                                                        Integer signalScore,
                                                        LocalDateTime createdAt) {
        StrategySignalDO signal = new StrategySignalDO();
        signal.setSignalId(signalId);
        signal.setTaskId(taskId);
        signal.setSignalType("RESEARCH");
        signal.setEntityCode(entityCode);
        signal.setEntityName(entityName);
        signal.setReasonSummary(reasonSummary);
        signal.setSignalDirection(signalDirection);
        signal.setSignalLevel("STRONG");
        signal.setSignalScore(signalScore);
        signal.setSignalDate(LocalDate.of(2026, 5, 7));
        signal.setConfidenceScore(new BigDecimal("0.91"));
        signal.setCreatedAt(createdAt);
        return signal;
    }

    private static final class TestDeps {
        private final ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        private final ResearchTaskStepMapper researchTaskStepMapper = mock(ResearchTaskStepMapper.class);
        private final AiWorkflowInstanceMapper aiWorkflowInstanceMapper = mock(AiWorkflowInstanceMapper.class);
        private final AiAgentExecutionMapper aiAgentExecutionMapper = mock(AiAgentExecutionMapper.class);
        private final AuditRecordMapper auditRecordMapper = mock(AuditRecordMapper.class);
        private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        private final ResearchTaskRetryLogMapper researchTaskRetryLogMapper = mock(ResearchTaskRetryLogMapper.class);
        private final ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        private final RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        private final RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);
        private final StrategySignalMapper strategySignalMapper = mock(StrategySignalMapper.class);
        private final StrategySignalFactorMapper strategySignalFactorMapper = mock(StrategySignalFactorMapper.class);

        private TestDeps() {
            when(researchTaskMapper.selectList(any())).thenReturn(List.of());
            when(researchReportMapper.selectList(any())).thenReturn(List.of());
            when(riskWarningMapper.selectList(any())).thenReturn(List.of());
            when(riskWarningDetailMapper.selectList(any())).thenReturn(List.of());
            when(strategySignalMapper.selectCount(any())).thenReturn(0L);
            when(strategySignalMapper.selectList(any())).thenReturn(List.of());
            when(strategySignalFactorMapper.selectList(any())).thenReturn(List.of());
            when(auditRecordMapper.selectList(any())).thenReturn(List.<AuditRecordDO>of());
            when(aiWorkflowInstanceMapper.selectList(any())).thenReturn(List.<AiWorkflowInstanceDO>of());
            when(aiAgentExecutionMapper.selectList(any())).thenReturn(List.<AiAgentExecutionDO>of());
        }
    }
}
