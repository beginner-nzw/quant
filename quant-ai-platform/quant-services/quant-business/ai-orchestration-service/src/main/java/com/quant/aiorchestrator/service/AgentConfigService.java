package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.AgentConfigItemVO;

import java.util.List;

public interface AgentConfigService {

    List<AgentConfigItemVO> loadAgents();

    void saveAgent(String agentCode, AgentConfigUpdateDTO dto);

    String resolveConfigPathForDisplay();
}
