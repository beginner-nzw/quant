package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.aiorchestrator.domain.vo.ModelStrategyItemVO;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ModelStrategyConfigCommandManager {

    private final ModelStrategyConfigStoreManager configStoreManager;
    private final ModelStrategyConfigPolicyManager policyManager;
    private final ModelStrategyConfigItemManager itemManager;
    private final ConfigChangeAuditService configChangeAuditService;

    public List<ModelStrategyItemVO> loadStrategies(String configPath) {
        return configStoreManager.readStrategies(configPath).stream()
                .map(itemManager::toStrategyItem)
                .toList();
    }

    public void saveStrategy(String configPath, String strategyCode, ModelStrategyUpdateDTO dto) {
        policyManager.validateSave(strategyCode, dto);

        Path resolvedConfigPath = configStoreManager.resolveConfigPath(configPath);
        List<Map<String, Object>> strategies = configStoreManager.readStrategies(configPath);
        boolean updated = false;
        for (Map<String, Object> item : strategies) {
            if (Objects.equals(normalize(item.get("strategyCode")), strategyCode.trim())) {
                Map<String, Object> before = new LinkedHashMap<>(item);
                itemManager.applyUpdate(item, dto);
                appendAudit(resolvedConfigPath, strategyCode, before, item);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new BizException("MODEL_STRATEGY_NOT_FOUND", "model strategy config not found: " + strategyCode);
        }

        configStoreManager.writeStrategies(resolvedConfigPath, strategies);
    }

    public String resolveConfigPathForDisplay(String configPath) {
        return configStoreManager.resolveConfigPath(configPath).toString();
    }

    private void appendAudit(
            Path configPath,
            String strategyCode,
            Map<String, Object> before,
            Map<String, Object> after
    ) {
        configChangeAuditService.appendAudit(
                "MODEL_STRATEGY",
                strategyCode,
                strategyCode,
                "UPDATE",
                configPath.toString(),
                "update model strategy config",
                itemManager.diffFields(before, after)
        );
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
