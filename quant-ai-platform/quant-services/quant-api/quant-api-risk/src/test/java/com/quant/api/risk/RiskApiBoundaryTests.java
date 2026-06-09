package com.quant.api.risk;

import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.RiskWarningListItemVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningPageVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningStatsVO;
import com.quant.aiorchestrator.risk.RiskStrategyStableContract;
import com.quant.aiorchestrator.service.RiskQueryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskApiBoundaryTests {

    @Test
    void riskApiOwnsRiskContractsAndReadModels() {
        assertEquals("com.quant.aiorchestrator.risk", RiskStrategyStableContract.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", RiskQueryService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.dto", RiskWarningPageQueryDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", RiskWarningPageVO.class.getPackageName());
        assertEquals(RiskWarningPageVO.class.getPackageName(), RiskWarningListItemVO.class.getPackageName());
        assertEquals(RiskWarningPageVO.class.getPackageName(), RiskWarningStatsVO.class.getPackageName());
    }
}
