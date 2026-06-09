package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.vo.ModelAgentConfigCenterVO;
import com.quant.aiorchestrator.manager.ModelAgentConfigDashboardManager;
import com.quant.aiorchestrator.service.ModelAgentConfigDashboardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModelAgentConfigDashboardQueryServiceImpl implements ModelAgentConfigDashboardQueryService {

    private final ModelAgentConfigDashboardManager modelAgentConfigDashboardManager;

    @Override
    public ModelAgentConfigCenterVO getModelAgentConfigCenter() {
        return modelAgentConfigDashboardManager.getModelAgentConfigCenter();
    }
}
