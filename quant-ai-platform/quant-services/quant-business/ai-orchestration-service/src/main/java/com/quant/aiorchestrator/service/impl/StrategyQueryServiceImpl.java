package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalPageVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalStatsVO;
import com.quant.aiorchestrator.service.StrategyQueryService;
import com.quant.aiorchestrator.service.StrategySignalService;
import com.quant.aiorchestrator.service.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyQueryServiceImpl implements StrategyQueryService {

    private final TaskQueryService taskQueryService;
    private final StrategySignalService strategySignalService;

    @Override
    public StrategySignalPageVO pageStrategySignals(StrategySignalPageQueryDTO queryDTO) {
        return taskQueryService.pageStrategySignals(queryDTO);
    }

    @Override
    public StrategySignalStatsVO getStrategySignalStats() {
        return taskQueryService.getStrategySignalStats();
    }

    @Override
    public List<StrategySignalFactorItemVO> listStrategySignalFactors(String signalId) {
        return strategySignalService.listFactors(signalId);
    }
}
