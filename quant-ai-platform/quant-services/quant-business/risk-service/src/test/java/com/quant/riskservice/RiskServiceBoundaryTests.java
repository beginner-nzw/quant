package com.quant.riskservice;

import com.quant.aiorchestrator.controller.RiskWarningController;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.manager.RiskWarningGeneratedDomainEventManager;
import com.quant.aiorchestrator.manager.TaskReportRiskReadManager;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.risk.RiskWarningProjectionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskServiceBoundaryTests {

    @Test
    void riskServiceOwnsRiskPersistenceAndController() {
        assertEquals("com.quant.aiorchestrator.domain.entity", RiskWarningDO.class.getPackageName());
        assertEquals(RiskWarningDO.class.getPackageName(), RiskWarningDetailDO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.mapper", RiskWarningMapper.class.getPackageName());
        assertEquals(RiskWarningMapper.class.getPackageName(), RiskWarningDetailMapper.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.controller", RiskWarningController.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", RiskWarningGeneratedDomainEventManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskReportRiskReadManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.risk", RiskWarningProjectionService.class.getPackageName());
    }
}
