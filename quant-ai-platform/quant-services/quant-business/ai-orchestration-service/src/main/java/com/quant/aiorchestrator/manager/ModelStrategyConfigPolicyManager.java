package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.common.core.exception.BizException;
import org.springframework.stereotype.Component;

@Component
public class ModelStrategyConfigPolicyManager {

    public void validateSave(String strategyCode, ModelStrategyUpdateDTO dto) {
        if (dto == null) {
            throw new BizException("MODEL_STRATEGY_EMPTY", "妯″瀷绛栫暐鏇存柊鍐呭涓嶈兘涓虹┖");
        }
        if (!hasText(strategyCode)) {
            throw new BizException("MODEL_STRATEGY_CODE_EMPTY", "妯″瀷绛栫暐缂栫爜涓嶈兘涓虹┖");
        }
        if (!hasText(dto.getProvider())) {
            throw new BizException("MODEL_STRATEGY_PROVIDER_EMPTY", "妯″瀷鎻愪緵鏂逛笉鑳戒负绌?");
        }
        if (!hasText(dto.getModelName())) {
            throw new BizException("MODEL_STRATEGY_NAME_EMPTY", "妯″瀷鍚嶇О涓嶈兘涓虹┖");
        }
        if (!hasText(dto.getBaseUrl())) {
            throw new BizException("MODEL_STRATEGY_BASE_URL_EMPTY", "妯″瀷鍦板潃涓嶈兘涓虹┖");
        }
        if (!hasText(dto.getAccessMode())) {
            throw new BizException("MODEL_STRATEGY_ACCESS_MODE_EMPTY", "鎺ュ叆妯″紡涓嶈兘涓虹┖");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
