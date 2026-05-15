package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.service.AiResultDomainProjectionService;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.redis.RedisKeyBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiResultDomainProjectionServiceTests {

    @Test
    void projectRefreshesStrategySignalRedisCache() throws Exception {
        TestDeps deps = new TestDeps();
        AiResultDomainProjectionService service = newService(deps);
        AiTaskResultMessage message = buildSuccessMessage();

        service.project(message, buildReport("report-1", "task-signal"));

        ArgumentCaptor<StrategySignalDO> signalCaptor = ArgumentCaptor.forClass(StrategySignalDO.class);
        verify(deps.strategySignalMapper).insert(signalCaptor.capture());
        StrategySignalDO signal = signalCaptor.getValue();
        assertEquals("signal-task-signal", signal.getSignalId());
        assertEquals("600519", signal.getEntityCode());

        ArgumentCaptor<String> cachePayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(deps.valueOperations).set(eq(RedisKeyBuilder.signalLatest("600519")), cachePayloadCaptor.capture());
        JsonNode cachePayload = new ObjectMapper().readTree(cachePayloadCaptor.getValue());
        assertEquals("signal-task-signal", cachePayload.get("signalId").asText());
        assertEquals("task-signal", cachePayload.get("taskId").asText());
        assertEquals("POSITIVE", cachePayload.get("signalDirection").asText());
        assertEquals(92, cachePayload.get("signalScore").asInt());

        verify(deps.zSetOperations).add(
                eq(RedisKeyBuilder.signalRanking(signal.getSignalDate().toString())),
                eq("signal-task-signal"),
                eq(92D)
        );
    }

    @Test
    void projectEvictsStrategySignalRedisCacheWhenSignalNoLongerValid() {
        TestDeps deps = new TestDeps();
        StrategySignalDO existing = new StrategySignalDO();
        existing.setSignalId("signal-task-signal");
        existing.setTaskId("task-signal");
        existing.setEntityCode("600519");
        existing.setSignalDate(LocalDate.of(2026, 5, 15));
        existing.setSignalScore(90);
        existing.setDeleted(0);
        when(deps.strategySignalMapper.selectOne(any())).thenReturn(existing);

        AiTaskResultMessage message = buildSuccessMessage();
        message.getPayload().setSummary(null);
        message.getPayload().setConfidenceScore(null);
        message.getPayload().setRiskWarnings(List.of());
        AiResultDomainProjectionService service = newService(deps);

        service.project(message, buildReport("report-1", "task-signal"));

        ArgumentCaptor<StrategySignalDO> signalCaptor = ArgumentCaptor.forClass(StrategySignalDO.class);
        verify(deps.strategySignalMapper).updateById(signalCaptor.capture());
        assertEquals(1, signalCaptor.getValue().getDeleted());
        verify(deps.stringRedisTemplate).delete(RedisKeyBuilder.signalLatest("600519"));
        verify(deps.zSetOperations).remove(RedisKeyBuilder.signalRanking("2026-05-15"), "signal-task-signal");
    }

    private AiResultDomainProjectionService newService(TestDeps deps) {
        return new AiResultDomainProjectionService(
                deps.riskWarningMapper,
                deps.riskWarningDetailMapper,
                deps.strategySignalMapper,
                deps.strategySignalFactorMapper,
                deps.reportEvidenceRefMapper,
                deps.researchReportSectionMapper,
                new ObjectMapper(),
                deps.stringRedisTemplate
        );
    }

    private static AiTaskResultMessage buildSuccessMessage() {
        AiTaskResultMessage message = new AiTaskResultMessage();
        message.setTaskId("task-signal");
        message.setTraceId("trace-signal");
        message.setTenantId("default");

        AiTaskResultMessage.ResultPayload payload = new AiTaskResultMessage.ResultPayload();
        payload.setFinalStatus(TaskStatusEnum.SUCCESS.name());
        payload.setTaskType("RESEARCH");
        payload.setTargetType("STOCK");
        payload.setTargetCode("600519");
        payload.setTargetName("Kweichow");
        payload.setSummary("high confidence upside");
        payload.setConfidenceScore(0.92D);
        payload.setNeedHumanReview(false);
        payload.setRiskWarnings(List.of());
        message.setPayload(payload);
        return message;
    }

    private static ResearchReportDO buildReport(String reportId, String taskId) {
        ResearchReportDO report = new ResearchReportDO();
        report.setReportId(reportId);
        report.setTaskId(taskId);
        report.setReportType("RESEARCH");
        report.setFinalStatus(TaskStatusEnum.SUCCESS.name());
        return report;
    }

    @SuppressWarnings("unchecked")
    private static final class TestDeps {
        private final RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        private final RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);
        private final StrategySignalMapper strategySignalMapper = mock(StrategySignalMapper.class);
        private final StrategySignalFactorMapper strategySignalFactorMapper = mock(StrategySignalFactorMapper.class);
        private final ReportEvidenceRefMapper reportEvidenceRefMapper = mock(ReportEvidenceRefMapper.class);
        private final ResearchReportSectionMapper researchReportSectionMapper = mock(ResearchReportSectionMapper.class);
        private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        private final ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);

        private TestDeps() {
            when(riskWarningMapper.selectOne(any())).thenReturn(null);
            when(strategySignalMapper.selectOne(any())).thenReturn(null);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        }
    }
}
