package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;

import java.util.List;

public interface StrategySignalService {

    String createOrUpdate(StrategySignalCreateDTO dto);

    List<StrategySignalFactorItemVO> listFactors(String signalId);

    String updateStatus(String signalId, StrategySignalStatusUpdateDTO dto);
}
