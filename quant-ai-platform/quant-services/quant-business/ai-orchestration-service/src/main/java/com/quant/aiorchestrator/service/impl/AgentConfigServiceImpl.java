package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.AgentConfigItemVO;
import com.quant.common.core.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AgentConfigServiceImpl implements AgentConfigService {

    private final String agentConfigPath;
    private final ObjectMapper objectMapper;
    private final ConfigChangeAuditService configChangeAuditService;

    public AgentConfigServiceImpl(
            @Value("${quant.ai.agent-config:../../../ai-config/agent-configs.json}") String agentConfigPath,
            ObjectMapper objectMapper,
            ConfigChangeAuditService configChangeAuditService
    ) {
        this.agentConfigPath = agentConfigPath;
        this.objectMapper = objectMapper;
        this.configChangeAuditService = configChangeAuditService;
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
            throw new BizException("AGENT_CONFIG_EMPTY", "Agent 配置更新内容不能为空");
        }
        if (!hasText(agentCode)) {
            throw new BizException("AGENT_CODE_EMPTY", "Agent 编码不能为空");
        }
        if (!hasText(dto.getAgentName())) {
            throw new BizException("AGENT_NAME_EMPTY", "Agent 名称不能为空");
        }
        if (!hasText(dto.getStageCode())) {
            throw new BizException("AGENT_STAGE_EMPTY", "阶段编码不能为空");
        }
        if (dto.getExecutionOrder() == null || dto.getExecutionOrder() < 1) {
            throw new BizException("AGENT_ORDER_INVALID", "执行顺序必须大于 0");
        }
        if (dto.getTimeoutSeconds() == null || dto.getTimeoutSeconds() < 1) {
            throw new BizException("AGENT_TIMEOUT_INVALID", "超时时间必须大于 0");
        }
        if (!hasText(dto.getImplementationMode())) {
            throw new BizException("AGENT_MODE_EMPTY", "实现模式不能为空");
        }
        if (!hasText(dto.getVersion())) {
            throw new BizException("AGENT_VERSION_EMPTY", "版本不能为空");
        }
        if ("report_generation_agent".equals(agentCode) && Boolean.FALSE.equals(dto.getEnabled())) {
            throw new BizException("AGENT_REQUIRED", "报告生成节点不能被禁用");
        }

        Path configPath = resolveConfigPath();
        List<Map<String, Object>> agents = readAgents();
        boolean updated = false;
        for (Map<String, Object> item : agents) {
            if (Objects.equals(normalize(item.get("agentCode")), agentCode.trim())) {
                Map<String, Object> before = new LinkedHashMap<>(item);
                applyUpdate(item, dto);
                updated = true;
                configChangeAuditService.appendAudit(
                        "AGENT_CONFIG",
                        agentCode,
                        dto.getAgentName(),
                        "UPDATE",
                        configPath.toString(),
                        "更新 Agent 配置",
                        diffFields(before, item)
                );
                break;
            }
        }

        if (!updated) {
            throw new BizException("AGENT_NOT_FOUND", "未找到 Agent 配置: " + agentCode);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("agents", agents);

        try {
            Files.createDirectories(configPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(configPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("AGENT_SAVE_FAILED", "保存 Agent 配置失败: " + agentCode);
        }
    }

    public String resolveConfigPathForDisplay() {
        return resolveConfigPath().toString();
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
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    Files.readString(configPath, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
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
            throw new BizException("AGENT_READ_FAILED", "读取 Agent 配置失败");
        }
    }

    private Path resolveConfigPath() {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(agentConfigPath);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("ai-config").resolve("agent-configs.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("agent-configs.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("agent-configs.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("agent-configs.json").normalize());
            current = current.getParent();
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return candidates.iterator().next();
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
