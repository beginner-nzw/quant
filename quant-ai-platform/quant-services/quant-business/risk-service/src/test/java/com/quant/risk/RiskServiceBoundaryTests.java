package com.quant.risk;

import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.manager.RiskWarningListItemAssembler;
import com.quant.aiorchestrator.manager.RiskWarningProjectionManager;
import com.quant.aiorchestrator.manager.RiskWarningReadManager;
import com.quant.aiorchestrator.manager.RiskWarningRuleManager;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.service.impl.RiskQueryServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskServiceBoundaryTests {

    @Test
    void riskServiceOwnsRiskWarningPersistenceTypes() {
        assertEquals("com.quant.aiorchestrator.domain.entity", RiskWarningDO.class.getPackageName());
        assertEquals(RiskWarningDO.class.getPackageName(), RiskWarningDetailDO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.mapper", RiskWarningMapper.class.getPackageName());
        assertEquals(RiskWarningMapper.class.getPackageName(), RiskWarningDetailMapper.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", RiskQueryServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", RiskWarningProjectionManager.class.getPackageName());
        assertEquals(RiskWarningProjectionManager.class.getPackageName(), RiskWarningReadManager.class.getPackageName());
        assertEquals(RiskWarningProjectionManager.class.getPackageName(), RiskWarningListItemAssembler.class.getPackageName());
        assertEquals(RiskWarningProjectionManager.class.getPackageName(), RiskWarningRuleManager.class.getPackageName());
    }
}
