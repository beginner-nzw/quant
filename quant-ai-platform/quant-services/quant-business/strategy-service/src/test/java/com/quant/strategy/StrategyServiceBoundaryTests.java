package com.quant.strategy;

import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalListItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalPageVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalStatsVO;
import com.quant.aiorchestrator.manager.StrategySignalCacheManager;
import com.quant.aiorchestrator.manager.StrategySignalCommandManager;
import com.quant.aiorchestrator.manager.StrategySignalFactorManager;
import com.quant.aiorchestrator.manager.StrategySignalGeneratedDomainEventManager;
import com.quant.aiorchestrator.manager.StrategySignalItemAssembler;
import com.quant.aiorchestrator.manager.StrategySignalProjectionManager;
import com.quant.aiorchestrator.manager.StrategySignalReadManager;
import com.quant.aiorchestrator.manager.StrategySignalRecordManager;
import com.quant.aiorchestrator.manager.StrategySignalRuleManager;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.risk.StrategySignalProjectionService;
import com.quant.aiorchestrator.service.StrategyQueryService;
import com.quant.aiorchestrator.service.StrategySignalService;
import com.quant.aiorchestrator.service.impl.StrategyQueryServiceImpl;
import com.quant.aiorchestrator.service.impl.StrategySignalServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrategyServiceBoundaryTests {

    @Test
    void strategyServiceOwnsStrategySignalPersistenceTypes() {
        assertEquals("com.quant.aiorchestrator.domain.entity", StrategySignalDO.class.getPackageName());
        assertEquals(StrategySignalDO.class.getPackageName(), StrategySignalFactorDO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.mapper", StrategySignalMapper.class.getPackageName());
        assertEquals(StrategySignalMapper.class.getPackageName(), StrategySignalFactorMapper.class.getPackageName());
    }

    @Test
    void strategyServiceOwnsStrategySignalApiModelsAndCommandRuntime() {
        assertEquals("com.quant.aiorchestrator.domain.dto", StrategySignalCreateDTO.class.getPackageName());
        assertEquals(StrategySignalCreateDTO.class.getPackageName(), StrategySignalPageQueryDTO.class.getPackageName());
        assertEquals(StrategySignalCreateDTO.class.getPackageName(), StrategySignalStatusUpdateDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", StrategySignalFactorItemVO.class.getPackageName());
        assertEquals(StrategySignalFactorItemVO.class.getPackageName(), StrategySignalListItemVO.class.getPackageName());
        assertEquals(StrategySignalFactorItemVO.class.getPackageName(), StrategySignalPageVO.class.getPackageName());
        assertEquals(StrategySignalFactorItemVO.class.getPackageName(), StrategySignalStatsVO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", StrategySignalCommandManager.class.getPackageName());
        assertEquals(StrategySignalCommandManager.class.getPackageName(), StrategySignalFactorManager.class.getPackageName());
        assertEquals(StrategySignalCommandManager.class.getPackageName(), StrategySignalCacheManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", StrategySignalService.class.getPackageName());
        assertEquals(StrategySignalService.class.getPackageName(), StrategyQueryService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", StrategySignalServiceImpl.class.getPackageName());
        assertEquals(StrategySignalServiceImpl.class.getPackageName(), StrategyQueryServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", StrategySignalProjectionManager.class.getPackageName());
        assertEquals(StrategySignalProjectionManager.class.getPackageName(), StrategySignalReadManager.class.getPackageName());
        assertEquals(StrategySignalProjectionManager.class.getPackageName(), StrategySignalRecordManager.class.getPackageName());
        assertEquals(StrategySignalProjectionManager.class.getPackageName(), StrategySignalItemAssembler.class.getPackageName());
        assertEquals(StrategySignalProjectionManager.class.getPackageName(), StrategySignalRuleManager.class.getPackageName());
        assertEquals(StrategySignalProjectionManager.class.getPackageName(), StrategySignalGeneratedDomainEventManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.risk", StrategySignalProjectionService.class.getPackageName());
    }
}
