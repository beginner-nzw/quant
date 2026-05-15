package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.RiskWarningPageVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningStatsVO;

public interface RiskQueryService {
    RiskWarningPageVO pageRiskWarnings(RiskWarningPageQueryDTO queryDTO);

    RiskWarningStatsVO getRiskWarningStats();
}
