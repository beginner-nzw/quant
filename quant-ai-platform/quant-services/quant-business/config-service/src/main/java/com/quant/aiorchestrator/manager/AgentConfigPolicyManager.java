package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.common.core.exception.BizException;
import org.springframework.stereotype.Component;

@Component
public class AgentConfigPolicyManager {

    public void validateSave(String agentCode, AgentConfigUpdateDTO dto) {
        if (dto == null) {
            throw new BizException("AGENT_CONFIG_EMPTY", "Agent config update cannot be empty");
        }
        if (!hasText(agentCode)) {
            throw new BizException("AGENT_CODE_EMPTY", "Agent code cannot be empty");
        }
        if (!hasText(dto.getAgentName())) {
            throw new BizException("AGENT_NAME_EMPTY", "Agent name cannot be empty");
        }
        if (!hasText(dto.getStageCode())) {
            throw new BizException("AGENT_STAGE_EMPTY", "Agent stage code cannot be empty");
        }
        if (dto.getExecutionOrder() == null || dto.getExecutionOrder() < 1) {
            throw new BizException("AGENT_ORDER_INVALID", "Agent execution order must be greater than 0");
        }
        if (dto.getTimeoutSeconds() == null || dto.getTimeoutSeconds() < 1) {
            throw new BizException("AGENT_TIMEOUT_INVALID", "Agent timeout must be greater than 0");
        }
        if (!hasText(dto.getImplementationMode())) {
            throw new BizException("AGENT_MODE_EMPTY", "Agent implementation mode cannot be empty");
        }
        if (!hasText(dto.getVersion())) {
            throw new BizException("AGENT_VERSION_EMPTY", "Agent version cannot be empty");
        }
        if ("report_generation_agent".equals(agentCode) && Boolean.FALSE.equals(dto.getEnabled())) {
            throw new BizException("AGENT_REQUIRED", "report_generation_agent cannot be disabled");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
