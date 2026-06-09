package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.AgentConfigItemVO;
import com.quant.config.port.AgentConfigQueryPort;

import java.util.List;

public interface AgentConfigService extends AgentConfigQueryPort {

    @Override
    List<AgentConfigItemVO> loadAgents();

    void saveAgent(String agentCode, AgentConfigUpdateDTO dto);

    String resolveConfigPathForDisplay();
}
