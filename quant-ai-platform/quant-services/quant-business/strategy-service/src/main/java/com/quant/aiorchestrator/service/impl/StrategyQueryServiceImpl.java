package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalPageVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalStatsVO;
import com.quant.aiorchestrator.manager.StrategySignalProjectionManager;
import com.quant.aiorchestrator.service.StrategyQueryService;
import com.quant.aiorchestrator.service.StrategySignalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyQueryServiceImpl implements StrategyQueryService {

    private final StrategySignalProjectionManager strategySignalProjectionManager;
    private final StrategySignalService strategySignalService;

    @Override
    public StrategySignalPageVO pageStrategySignals(StrategySignalPageQueryDTO queryDTO) {
        return strategySignalProjectionManager.pageStrategySignals(queryDTO);
    }

    @Override
    public StrategySignalStatsVO getStrategySignalStats() {
        return strategySignalProjectionManager.getStrategySignalStats();
    }

    @Override
    public List<StrategySignalFactorItemVO> listStrategySignalFactors(String signalId) {
        return strategySignalService.listFactors(signalId);
    }
}
