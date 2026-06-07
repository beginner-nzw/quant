package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.vo.ConfigChangeAuditItemVO;
import com.quant.aiorchestrator.manager.ConfigChangeAuditItemManager;
import com.quant.aiorchestrator.manager.ConfigChangeAuditStoreManager;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigChangeAuditServiceImpl implements ConfigChangeAuditService {

    @Value("${quant.ai.config-audit:../../../ai-config/config-change-audits.json}")
    private String configAuditPath;

    private final ConfigChangeAuditStoreManager storeManager;
    private final ConfigChangeAuditItemManager itemManager;

    @Override
    public void appendAudit(String configType,
                            String targetCode,
                            String targetName,
                            String operation,
                            String configPath,
                            String changeSummary,
                            List<String> changedFields) {
        List<Map<String, Object>> audits = storeManager.readAudits(configAuditPath);
        audits.add(0, itemManager.buildAuditItem(
                configType,
                targetCode,
                targetName,
                operation,
                configPath,
                changeSummary,
                changedFields
        ));
        if (audits.size() > 50) {
            audits = new ArrayList<>(audits.subList(0, 50));
        }
        storeManager.writeAudits(configAuditPath, audits);
    }

    @Override
    public List<ConfigChangeAuditItemVO> loadRecentAudits() {
        List<Map<String, Object>> audits = storeManager.readAudits(configAuditPath);
        List<ConfigChangeAuditItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : audits) {
            result.add(itemManager.toAuditItemVO(item));
        }
        return result;
    }
}
