package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalPageVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalStatsVO;

import java.util.List;

public interface StrategyQueryService {
    StrategySignalPageVO pageStrategySignals(StrategySignalPageQueryDTO queryDTO);

    StrategySignalStatsVO getStrategySignalStats();

    List<StrategySignalFactorItemVO> listStrategySignalFactors(String signalId);
}
