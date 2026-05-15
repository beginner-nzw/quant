package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.WorkflowConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.WorkflowConfigItemVO;
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
public class WorkflowConfigService {

    private static final String REQUIRED_FINAL_AGENT = "report_generation_agent";

    private final String workflowConfigPath;
    private final ObjectMapper objectMapper;
    private final ConfigChangeAuditService configChangeAuditService;

    public WorkflowConfigService(
            @Value("${quant.ai.workflow-config:../../../ai-config/workflow-configs.json}") String workflowConfigPath,
            ObjectMapper objectMapper,
            ConfigChangeAuditService configChangeAuditService
    ) {
        this.workflowConfigPath = workflowConfigPath;
        this.objectMapper = objectMapper;
        this.configChangeAuditService = configChangeAuditService;
    }

    public List<WorkflowConfigItemVO> loadWorkflows() {
        List<Map<String, Object>> workflows = readWorkflows();
        List<WorkflowConfigItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : workflows) {
            result.add(toWorkflowItem(item));
        }
        return result;
    }

    public WorkflowConfigItemVO resolveWorkflow(String taskType) {
        String normalizedTaskType = normalize(taskType);
        List<WorkflowConfigItemVO> workflows = loadWorkflows();
        List<WorkflowConfigItemVO> enabledWorkflows = workflows.stream()
                .filter(item -> !Boolean.FALSE.equals(item.getEnabled()))
                .toList();
        if (enabledWorkflows.isEmpty()) {
            return null;
        }

        if (normalizedTaskType != null) {
            for (WorkflowConfigItemVO workflow : enabledWorkflows) {
                if (workflow.getTaskTypes() != null && workflow.getTaskTypes().contains(normalizedTaskType)) {
                    return workflow;
                }
            }
        }

        for (WorkflowConfigItemVO workflow : enabledWorkflows) {
            if (Boolean.TRUE.equals(workflow.getDefaultSelected())) {
                return workflow;
            }
        }

        return enabledWorkflows.get(0);
    }

    public void saveWorkflow(String workflowCode, WorkflowConfigUpdateDTO dto) {
        if (dto == null) {
            throw new BizException("WORKFLOW_CONFIG_EMPTY", "工作流配置内容不能为空");
        }
        if (!hasText(workflowCode)) {
            throw new BizException("WORKFLOW_CODE_EMPTY", "工作流编码不能为空");
        }
        if (!hasText(dto.getWorkflowVersion())) {
            throw new BizException("WORKFLOW_VERSION_EMPTY", "工作流版本不能为空");
        }
        if (!hasText(dto.getWorkflowType())) {
            throw new BizException("WORKFLOW_TYPE_EMPTY", "工作流类型不能为空");
        }

        List<String> taskTypes = sanitizeList(dto.getTaskTypes());
        if (taskTypes.isEmpty()) {
            throw new BizException("WORKFLOW_TASK_TYPES_EMPTY", "至少需要绑定一个任务类型");
        }

        List<String> nodeSequence = sanitizeList(dto.getNodeSequence());
        if (nodeSequence.isEmpty()) {
            throw new BizException("WORKFLOW_NODE_SEQUENCE_EMPTY", "节点链路不能为空");
        }
        if (!REQUIRED_FINAL_AGENT.equals(nodeSequence.get(nodeSequence.size() - 1))) {
            throw new BizException("WORKFLOW_FINAL_NODE_INVALID", "工作流最后一个节点必须是 report_generation_agent");
        }
        if (!nodeSequence.contains(REQUIRED_FINAL_AGENT)) {
            throw new BizException("WORKFLOW_FINAL_NODE_MISSING", "工作流必须包含 report_generation_agent");
        }

        Path configPath = resolveConfigPath();
        List<Map<String, Object>> workflows = readWorkflows();
        boolean updated = false;
        for (Map<String, Object> item : workflows) {
            if (Objects.equals(normalize(item.get("workflowCode")), workflowCode.trim())) {
                Map<String, Object> before = new LinkedHashMap<>(item);
                applyUpdate(item, dto, taskTypes, nodeSequence);
                updated = true;
                configChangeAuditService.appendAudit(
                        "WORKFLOW_CONFIG",
                        workflowCode,
                        workflowCode,
                        "UPDATE",
                        configPath.toString(),
                        "更新工作流配置",
                        diffFields(before, item)
                );
                break;
            }
        }

        if (!updated) {
            throw new BizException("WORKFLOW_NOT_FOUND", "未找到工作流配置: " + workflowCode);
        }

        if (Boolean.TRUE.equals(dto.getDefaultSelected())) {
            for (Map<String, Object> item : workflows) {
                item.put("defaultSelected", Objects.equals(normalize(item.get("workflowCode")), workflowCode.trim()));
            }
        } else if (workflows.stream().noneMatch(item -> Boolean.TRUE.equals(item.get("defaultSelected")) && !Boolean.FALSE.equals(item.get("enabled")))) {
            workflows.stream()
                    .filter(item -> !Boolean.FALSE.equals(item.get("enabled")))
                    .findFirst()
                    .ifPresent(item -> item.put("defaultSelected", true));
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("workflows", workflows);

        try {
            Files.createDirectories(configPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(configPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("WORKFLOW_SAVE_FAILED", "保存工作流配置失败: " + workflowCode);
        }
    }

    public String resolveConfigPathForDisplay() {
        return resolveConfigPath().toString();
    }

    private void applyUpdate(Map<String, Object> item,
                             WorkflowConfigUpdateDTO dto,
                             List<String> taskTypes,
                             List<String> nodeSequence) {
        item.put("workflowVersion", dto.getWorkflowVersion().trim());
        item.put("workflowType", dto.getWorkflowType().trim());
        item.put("taskTypes", taskTypes);
        item.put("entryAgent", nodeSequence.get(0));
        item.put("enabled", !Boolean.FALSE.equals(dto.getEnabled()));
        item.put("defaultSelected", Boolean.TRUE.equals(dto.getDefaultSelected()));
        item.put("nodeSequence", nodeSequence);
        item.put("remark", normalize(dto.getRemark()));
    }

    private WorkflowConfigItemVO toWorkflowItem(Map<String, Object> item) {
        WorkflowConfigItemVO vo = new WorkflowConfigItemVO();
        List<String> nodeSequence = sanitizeList(castList(item.get("nodeSequence")));
        vo.setWorkflowCode(normalize(item.get("workflowCode")));
        vo.setWorkflowVersion(normalize(item.get("workflowVersion")));
        vo.setWorkflowType(normalize(item.get("workflowType")));
        vo.setTaskTypes(sanitizeList(castList(item.get("taskTypes"))));
        vo.setEntryAgent(nodeSequence.isEmpty() ? normalize(item.get("entryAgent")) : nodeSequence.get(0));
        vo.setNodeCount(nodeSequence.size());
        vo.setEnabled(readBoolean(item.get("enabled")));
        vo.setDefaultSelected(readBoolean(item.get("defaultSelected")));
        vo.setNodeSequence(nodeSequence);
        vo.setNodeTimeoutSummary("");
        vo.setRemark(normalize(item.get("remark")));
        return vo;
    }

    private List<Map<String, Object>> readWorkflows() {
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    Files.readString(configPath, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
            Object workflows = root.get("workflows");
            if (!(workflows instanceof List<?> workflowList)) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : workflowList) {
                if (item instanceof Map<?, ?> rawItem) {
                    result.add(new LinkedHashMap<>(objectMapper.convertValue(
                            rawItem,
                            new TypeReference<LinkedHashMap<String, Object>>() {}
                    )));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BizException("WORKFLOW_READ_FAILED", "读取工作流配置失败");
        }
    }

    private Path resolveConfigPath() {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(workflowConfigPath);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("ai-config").resolve("workflow-configs.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("workflow-configs.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("workflow-configs.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("workflow-configs.json").normalize());
            current = current.getParent();
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return candidates.iterator().next();
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
