package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.aiorchestrator.domain.vo.ModelStrategyItemVO;

import java.util.List;

public interface ModelStrategyConfigService {

    List<ModelStrategyItemVO> loadStrategies();

    void saveStrategy(String strategyCode, ModelStrategyUpdateDTO dto);

    String resolveConfigPathForDisplay();
}
