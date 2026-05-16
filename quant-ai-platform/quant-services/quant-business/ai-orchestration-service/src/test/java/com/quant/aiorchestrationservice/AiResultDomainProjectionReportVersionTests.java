package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.aiorchestrator.service.impl.AiResultDomainProjectionServiceImpl;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiResultDomainProjectionReportVersionTests {

    @Test
    void successfulProjectionCreatesReportVersionSnapshot() {
        TestDeps deps = new TestDeps();
        AiResultDomainProjectionServiceImpl service = newService(deps);
        ResearchReportDO report = buildReport(2);

        service.project(buildMessage(TaskStatusEnum.SUCCESS.name()), report);

        verify(deps.reportVersionService).createSnapshot(report, "AI_RESULT");
    }

    @Test
    void failedProjectionDoesNotCreateReportVersionSnapshot() {
        TestDeps deps = new TestDeps();
        AiResultDomainProjectionServiceImpl service = newService(deps);

        service.project(buildMessage(TaskStatusEnum.FAILED.name()), buildReport(1));

        verify(deps.reportVersionService, never()).createSnapshot(any(), eq("AI_RESULT"));
    }

    private static AiResultDomainProjectionServiceImpl newService(TestDeps deps) {
        AiResultDomainProjectionServiceImpl service = new AiResultDomainProjectionServiceImpl(
                deps.riskWarningMapper,
                deps.riskWarningDetailMapper,
                deps.strategySignalMapper,
                deps.strategySignalFactorMapper,
                deps.evidenceMapper,
                deps.sectionMapper,
                new ObjectMapper(),
                deps.redisTemplate
        );
        ReflectionTestUtils.setField(service, "reportVersionService", deps.reportVersionService);
        return service;
    }

    private static AiTaskResultMessage buildMessage(String finalStatus) {
        AiTaskResultMessage message = new AiTaskResultMessage();
        message.setTaskId("task-1");
        AiTaskResultMessage.ResultPayload payload = new AiTaskResultMessage.ResultPayload();
        payload.setFinalStatus(finalStatus);
        payload.setSummary(null);
        payload.setConfidenceScore(null);
        payload.setRiskWarnings(List.of());
        message.setPayload(payload);
        return message;
    }

    private static ResearchReportDO buildReport(int versionNo) {
        ResearchReportDO report = new ResearchReportDO();
        report.setReportId("report-1");
        report.setTaskId("task-1");
        report.setVersionNo(versionNo);
        report.setFinalStatus(TaskStatusEnum.SUCCESS.name());
        return report;
    }

    private static final class TestDeps {
        private final RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        private final RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);
        private final StrategySignalMapper strategySignalMapper = mock(StrategySignalMapper.class);
        private final StrategySignalFactorMapper strategySignalFactorMapper = mock(StrategySignalFactorMapper.class);
        private final ReportEvidenceRefMapper evidenceMapper = mock(ReportEvidenceRefMapper.class);
        private final ResearchReportSectionMapper sectionMapper = mock(ResearchReportSectionMapper.class);
        private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        private final ReportVersionService reportVersionService = mock(ReportVersionService.class);

        private TestDeps() {
            when(riskWarningMapper.selectOne(any())).thenReturn(null);
            when(strategySignalMapper.selectOne(any())).thenReturn(null);
        }
    }
}
