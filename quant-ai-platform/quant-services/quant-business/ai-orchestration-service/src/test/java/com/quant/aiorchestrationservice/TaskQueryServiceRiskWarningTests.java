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
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;

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

        TaskQueryServiceImpl service = newService(
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

        TaskQueryServiceImpl service = newService(
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

        TaskQueryServiceImpl service = newService(
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

        TaskQueryServiceImpl service = newService(
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

        TaskQueryServiceImpl service = newService(
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

        TaskQueryServiceImpl service = newService(
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

    private TaskQueryServiceImpl newService(ResearchTaskMapper researchTaskMapper,
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

    private TaskQueryServiceImpl newService(ResearchTaskMapper researchTaskMapper,
                                            ResearchReportMapper researchReportMapper,
                                            RiskWarningMapper riskWarningMapper,
                                            RiskWarningDetailMapper riskWarningDetailMapper,
                                            StringRedisTemplate stringRedisTemplate) {
        return new TaskQueryServiceImpl(
                researchTaskMapper,
                mock(ResearchTaskStepMapper.class),
                mock(AiWorkflowInstanceMapper.class),
                mock(AiAgentExecutionMapper.class),
                mock(AuditRecordMapper.class),
                stringRedisTemplate,
                new ObjectMapper(),
                mock(ResearchTaskRetryLogMapper.class),
                mock(TaskCacheVersionManager.class),
                researchReportMapper,
                mock(AgentConfigService.class),
                mock(ConfigChangeAuditService.class),
                mock(EventAutoTriggerConfigService.class),
                mock(MarketEventIngestHistoryService.class),
                mock(EventSourceConfigService.class),
                mock(ModelStrategyConfigService.class),
                mock(PromptTemplateConfigService.class),
                mock(WorkflowConfigService.class),
                mock(RoleAccessConfigService.class),
                riskWarningMapper,
                riskWarningDetailMapper,
                mock(StrategySignalMapper.class),
                mock(StrategySignalFactorMapper.class),
                mock(ReportEvidenceRefMapper.class),
                mock(HumanReviewRecordMapper.class),
                mock(ResearchReportSectionMapper.class),
                new TaskStateManager()
        );
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
