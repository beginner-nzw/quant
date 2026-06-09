package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.common.core.exception.BizException;
import org.springframework.stereotype.Component;

@Component
public class ModelStrategyConfigPolicyManager {

    public void validateSave(String strategyCode, ModelStrategyUpdateDTO dto) {
        if (dto == null) {
            throw new BizException("MODEL_STRATEGY_EMPTY", "Model strategy update cannot be empty");
        }
        if (!hasText(strategyCode)) {
            throw new BizException("MODEL_STRATEGY_CODE_EMPTY", "Model strategy code cannot be empty");
        }
        if (!hasText(dto.getProvider())) {
            throw new BizException("MODEL_STRATEGY_PROVIDER_EMPTY", "Model strategy provider cannot be empty");
        }
        if (!hasText(dto.getModelName())) {
            throw new BizException("MODEL_STRATEGY_NAME_EMPTY", "Model strategy model name cannot be empty");
        }
        if (!hasText(dto.getBaseUrl())) {
            throw new BizException("MODEL_STRATEGY_BASE_URL_EMPTY", "Model strategy base URL cannot be empty");
        }
        if (!hasText(dto.getAccessMode())) {
            throw new BizException("MODEL_STRATEGY_ACCESS_MODE_EMPTY", "Model strategy access mode cannot be empty");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
