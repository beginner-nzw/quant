package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.AgentConfigItemVO;
import com.quant.aiorchestrator.manager.AgentConfigPolicyManager;
import com.quant.aiorchestrator.manager.AgentConfigStoreManager;
import com.quant.aiorchestrator.service.AgentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentConfigServiceImpl implements AgentConfigService {

    private final AgentConfigStoreManager agentConfigStoreManager;
    private final AgentConfigPolicyManager agentConfigPolicyManager;

    @Override
    public List<AgentConfigItemVO> loadAgents() {
        return agentConfigStoreManager.loadAgents();
    }

    @Override
    public void saveAgent(String agentCode, AgentConfigUpdateDTO dto) {
        agentConfigPolicyManager.validateSave(agentCode, dto);
        agentConfigStoreManager.saveAgent(agentCode, dto);
    }

    @Override
    public String resolveConfigPathForDisplay() {
        return agentConfigStoreManager.resolveConfigPathForDisplay();
    }
}
