package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.AgentConfigService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.configstore.ConfigStoreKey;
import com.quant.aiorchestrator.configstore.GovernedConfigStore;
import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.AgentConfigItemVO;
import com.quant.common.core.exception.BizException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AgentConfigServiceImpl implements AgentConfigService {

    private final ObjectMapper objectMapper;
    private final GovernedConfigStore governedConfigStore;

    public AgentConfigServiceImpl(
            ObjectMapper objectMapper,
            GovernedConfigStore governedConfigStore
    ) {
        this.objectMapper = objectMapper;
        this.governedConfigStore = governedConfigStore;
    }

    public List<AgentConfigItemVO> loadAgents() {
        List<Map<String, Object>> agents = readAgents();
        List<AgentConfigItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : agents) {
            result.add(toAgentItem(item));
        }
        return result;
    }

    public void saveAgent(String agentCode, AgentConfigUpdateDTO dto) {
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

        List<Map<String, Object>> agents = readAgents();
        boolean updated = false;
        List<String> changedFields = new ArrayList<>();
        for (Map<String, Object> item : agents) {
            if (Objects.equals(normalize(item.get("agentCode")), agentCode.trim())) {
                Map<String, Object> before = new LinkedHashMap<>(item);
                applyUpdate(item, dto);
                changedFields = diffFields(before, item);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new BizException("AGENT_NOT_FOUND", "Agent config not found: " + agentCode);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("agents", agents);

        governedConfigStore.writeRoot(ConfigStoreKey.AGENT, root, agentCode, dto.getAgentName(), "UPDATE", "更新 Agent 配置", changedFields);
    }

    public String resolveConfigPathForDisplay() {
        return governedConfigStore.displayPath(ConfigStoreKey.AGENT);
    }

    private void applyUpdate(Map<String, Object> item, AgentConfigUpdateDTO dto) {
        item.put("agentName", dto.getAgentName().trim());
        item.put("stageCode", dto.getStageCode().trim());
        item.put("executionOrder", dto.getExecutionOrder());
        item.put("enabled", !Boolean.FALSE.equals(dto.getEnabled()));
        item.put("timeoutSeconds", dto.getTimeoutSeconds());
        item.put("needHumanReview", Boolean.TRUE.equals(dto.getNeedHumanReview()));
        item.put("implementationMode", dto.getImplementationMode().trim());
        item.put("version", dto.getVersion().trim());
        item.put("toolWhitelist", sanitizeList(dto.getToolWhitelist()));
        item.put("inputKeys", sanitizeList(dto.getInputKeys()));
        item.put("outputKeys", sanitizeList(dto.getOutputKeys()));
        item.put("remark", normalize(dto.getRemark()));
    }

    private AgentConfigItemVO toAgentItem(Map<String, Object> item) {
        AgentConfigItemVO vo = new AgentConfigItemVO();
        vo.setAgentCode(normalize(item.get("agentCode")));
        vo.setAgentName(normalize(item.get("agentName")));
        vo.setStageCode(normalize(item.get("stageCode")));
        vo.setExecutionOrder(readInteger(item.get("executionOrder")));
        vo.setEnabled(readBoolean(item.get("enabled")));
        vo.setTimeoutSeconds(readInteger(item.get("timeoutSeconds")));
        vo.setNeedHumanReview(readBoolean(item.get("needHumanReview")));
        vo.setImplementationMode(normalize(item.get("implementationMode")));
        vo.setVersion(normalize(item.get("version")));
        vo.setToolWhitelist(castList(item.get("toolWhitelist")));
        vo.setInputKeys(castList(item.get("inputKeys")));
        vo.setOutputKeys(castList(item.get("outputKeys")));
        vo.setRemark(normalize(item.get("remark")));
        return vo;
    }

    private List<Map<String, Object>> readAgents() {
        try {
            Map<String, Object> emptyRoot = new LinkedHashMap<>();
            emptyRoot.put("agents", new ArrayList<>());
            Map<String, Object> root = governedConfigStore.readRoot(ConfigStoreKey.AGENT, emptyRoot);
            Object agents = root.get("agents");
            if (!(agents instanceof List<?> agentList)) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : agentList) {
                if (item instanceof Map<?, ?> rawItem) {
                    result.add(new LinkedHashMap<>(objectMapper.convertValue(
                            rawItem,
                            new TypeReference<LinkedHashMap<String, Object>>() {}
                    )));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BizException("AGENT_READ_FAILED", "Failed to read Agent config");
        }
    }

    private List<String> castList(Object value) {
        if (!(value instanceof List<?> items)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            String normalized = normalize(item);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> diffFields(Map<String, Object> before, Map<String, Object> after) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        fields.addAll(before.keySet());
        fields.addAll(after.keySet());
        List<String> result = new ArrayList<>();
        for (String field : fields) {
            if (!Objects.equals(before.get(field), after.get(field))) {
                result.add(field);
            }
        }
        return result;
    }

    private Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Boolean readBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}


