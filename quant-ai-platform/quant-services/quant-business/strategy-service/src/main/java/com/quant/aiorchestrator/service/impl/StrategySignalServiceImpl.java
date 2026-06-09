package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.manager.StrategySignalCommandManager;
import com.quant.aiorchestrator.service.StrategySignalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategySignalServiceImpl implements StrategySignalService {

    private final StrategySignalCommandManager strategySignalCommandManager;

    @Override
    public String createOrUpdate(StrategySignalCreateDTO dto) {
        return strategySignalCommandManager.createOrUpdate(dto);
    }

    @Override
    public List<StrategySignalFactorItemVO> listFactors(String signalId) {
        return strategySignalCommandManager.listFactors(signalId);
    }

    @Override
    public String updateStatus(String signalId, StrategySignalStatusUpdateDTO dto) {
        return strategySignalCommandManager.updateStatus(signalId, dto);
    }
}
