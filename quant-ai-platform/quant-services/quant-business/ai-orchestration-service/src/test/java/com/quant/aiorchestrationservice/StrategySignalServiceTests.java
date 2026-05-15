package com.quant.aiorchestrationservice;

import com.quant.aiorchestrator.service.impl.StrategySignalServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.service.StrategySignalService;
import com.quant.common.redis.RedisKeyBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrategySignalServiceTests {

    @Test
    void createOrUpdateInsertsSignalFactorsAndRefreshesRedisCache() throws Exception {
        TestDeps deps = new TestDeps();
        StrategySignalCreateDTO dto = new StrategySignalCreateDTO();
        dto.setSignalId("signal-manual-1");
        dto.setTaskId("task-manual-1");
        dto.setSignalType("MANUAL_RESEARCH");
        dto.setEntityCode("600519");
        dto.setEntityName("Kweichow");
        dto.setSignalDate(LocalDate.of(2026, 5, 15));
        dto.setSignalScore(88);
        dto.setSignalDirection("positive");
        dto.setReasonSummary("manual strategy signal");
        dto.setConfidenceScore(new BigDecimal("0.88"));
        StrategySignalCreateDTO.FactorDTO factor = new StrategySignalCreateDTO.FactorDTO();
        factor.setFactorCode("VALUATION");
        factor.setFactorName("Valuation");
        factor.setFactorValue("attractive");
        factor.setFactorWeight(new BigDecimal("0.6"));
        factor.setFactorConclusion("valuation supports upside");
        dto.setFactors(List.of(factor));

        StrategySignalService service = newService(deps);

        String signalId = service.createOrUpdate(dto);

        assertEquals("signal-manual-1", signalId);
        ArgumentCaptor<StrategySignalDO> signalCaptor = ArgumentCaptor.forClass(StrategySignalDO.class);
        verify(deps.strategySignalMapper).insert(signalCaptor.capture());
        StrategySignalDO signal = signalCaptor.getValue();
        assertEquals("POSITIVE", signal.getSignalDirection());
        assertEquals("STRONG", signal.getSignalLevel());
        assertEquals("ACTIVE", signal.getStatus());

        ArgumentCaptor<StrategySignalFactorDO> factorCaptor = ArgumentCaptor.forClass(StrategySignalFactorDO.class);
        verify(deps.strategySignalFactorMapper).insert(factorCaptor.capture());
        assertEquals("VALUATION", factorCaptor.getValue().getFactorCode());

        ArgumentCaptor<String> cachePayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(deps.valueOperations).set(eq(RedisKeyBuilder.signalLatest("600519")), cachePayloadCaptor.capture());
        JsonNode cachePayload = new ObjectMapper().readTree(cachePayloadCaptor.getValue());
        assertEquals("signal-manual-1", cachePayload.get("signalId").asText());
        assertEquals("ACTIVE", cachePayload.get("status").asText());
        verify(deps.zSetOperations).add(RedisKeyBuilder.signalRanking("2026-05-15"), "signal-manual-1", 88D);
    }

    @Test
    void listFactorsReturnsDomainFactors() {
        TestDeps deps = new TestDeps();
        when(deps.strategySignalMapper.selectOne(any())).thenReturn(buildSignal("signal-1"));
        StrategySignalFactorDO factor = new StrategySignalFactorDO();
        factor.setFactorId("factor-1");
        factor.setSignalId("signal-1");
        factor.setFactorCode("CONFIDENCE");
        factor.setFactorName("Confidence score");
        factor.setFactorValue("0.92");
        factor.setFactorWeight(new BigDecimal("0.5"));
        factor.setFactorConclusion("Model confidence projection");
        when(deps.strategySignalFactorMapper.selectList(any())).thenReturn(List.of(factor));

        StrategySignalService service = newService(deps);

        var factors = service.listFactors("signal-1");

        assertEquals(1, factors.size());
        assertEquals("factor-1", factors.get(0).getFactorId());
        assertEquals("CONFIDENCE", factors.get(0).getFactorCode());
    }

    @Test
    void updateStatusArchivedEvictsRedisCache() {
        TestDeps deps = new TestDeps();
        StrategySignalDO signal = buildSignal("signal-1");
        when(deps.strategySignalMapper.selectOne(any())).thenReturn(signal);
        StrategySignalStatusUpdateDTO dto = new StrategySignalStatusUpdateDTO();
        dto.setStatus("ARCHIVED");

        StrategySignalService service = newService(deps);

        String status = service.updateStatus("signal-1", dto);

        assertEquals("ARCHIVED", status);
        ArgumentCaptor<StrategySignalDO> signalCaptor = ArgumentCaptor.forClass(StrategySignalDO.class);
        verify(deps.strategySignalMapper).updateById(signalCaptor.capture());
        assertEquals("ARCHIVED", signalCaptor.getValue().getStatus());
        verify(deps.stringRedisTemplate).delete(RedisKeyBuilder.signalLatest("600519"));
        verify(deps.zSetOperations).remove(RedisKeyBuilder.signalRanking("2026-05-15"), "signal-1");
    }

    @Test
    void updateStatusActiveRefreshesRedisCache() throws Exception {
        TestDeps deps = new TestDeps();
        StrategySignalDO signal = buildSignal("signal-1");
        signal.setStatus("ARCHIVED");
        when(deps.strategySignalMapper.selectOne(any())).thenReturn(signal);
        StrategySignalStatusUpdateDTO dto = new StrategySignalStatusUpdateDTO();
        dto.setStatus("active");

        StrategySignalService service = newService(deps);

        String status = service.updateStatus("signal-1", dto);

        assertEquals("ACTIVE", status);
        ArgumentCaptor<String> cachePayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(deps.valueOperations).set(eq(RedisKeyBuilder.signalLatest("600519")), cachePayloadCaptor.capture());
        JsonNode cachePayload = new ObjectMapper().readTree(cachePayloadCaptor.getValue());
        assertEquals("signal-1", cachePayload.get("signalId").asText());
        assertEquals("ACTIVE", cachePayload.get("status").asText());
        verify(deps.zSetOperations).add(RedisKeyBuilder.signalRanking("2026-05-15"), "signal-1", 91D);
    }

    private StrategySignalService newService(TestDeps deps) {
        return new StrategySignalServiceImpl(
                deps.strategySignalMapper,
                deps.strategySignalFactorMapper,
                new ObjectMapper(),
                deps.stringRedisTemplate
        );
    }

    private static StrategySignalDO buildSignal(String signalId) {
        StrategySignalDO signal = new StrategySignalDO();
        signal.setSignalId(signalId);
        signal.setTaskId("task-1");
        signal.setSignalType("RESEARCH");
        signal.setEntityCode("600519");
        signal.setEntityName("Kweichow");
        signal.setSignalDate(LocalDate.of(2026, 5, 15));
        signal.setSignalScore(91);
        signal.setSignalLevel("STRONG");
        signal.setSignalDirection("POSITIVE");
        signal.setReasonSummary("domain signal");
        signal.setConfidenceScore(new BigDecimal("0.91"));
        signal.setStatus("ACTIVE");
        signal.setTenantId("default");
        signal.setDeleted(0);
        return signal;
    }

    @SuppressWarnings("unchecked")
    private static final class TestDeps {
        private final StrategySignalMapper strategySignalMapper = mock(StrategySignalMapper.class);
        private final StrategySignalFactorMapper strategySignalFactorMapper = mock(StrategySignalFactorMapper.class);
        private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        private final ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);

        private TestDeps() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(strategySignalFactorMapper.selectList(any())).thenReturn(List.of());
        }
    }
}
