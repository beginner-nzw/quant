package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.RiskWarningPageVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningStatsVO;
import com.quant.aiorchestrator.service.RiskQueryService;
import com.quant.aiorchestrator.service.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskQueryServiceImpl implements RiskQueryService {

    private final TaskQueryService taskQueryService;

    @Override
    public RiskWarningPageVO pageRiskWarnings(RiskWarningPageQueryDTO queryDTO) {
        return taskQueryService.pageRiskWarnings(queryDTO);
    }

    @Override
    public RiskWarningStatsVO getRiskWarningStats() {
        return taskQueryService.getRiskWarningStats();
    }
}
