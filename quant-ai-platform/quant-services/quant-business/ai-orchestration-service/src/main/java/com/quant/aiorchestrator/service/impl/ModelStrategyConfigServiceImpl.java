package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.aiorchestrator.domain.vo.ModelStrategyItemVO;
import com.quant.aiorchestrator.manager.ModelStrategyConfigCommandManager;
import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelStrategyConfigServiceImpl implements ModelStrategyConfigService {

    @Value("${quant.ai.model-strategy-config:../../../ai-config/model-strategies.json}")
    private String modelStrategyConfigPath;

    private final ModelStrategyConfigCommandManager commandManager;

    @Override
    public List<ModelStrategyItemVO> loadStrategies() {
        return commandManager.loadStrategies(modelStrategyConfigPath);
    }

    @Override
    public void saveStrategy(String strategyCode, ModelStrategyUpdateDTO dto) {
        commandManager.saveStrategy(modelStrategyConfigPath, strategyCode, dto);
    }

    @Override
    public String resolveConfigPathForDisplay() {
        return commandManager.resolveConfigPathForDisplay(modelStrategyConfigPath);
    }
}
