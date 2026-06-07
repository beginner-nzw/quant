package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.vo.AgentConfigItemVO;
import com.quant.aiorchestrator.domain.vo.WorkflowConfigItemVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class WorkflowConfigAssemblyManager {

    public List<WorkflowConfigItemVO> applyAgentConfigsToWorkflows(List<WorkflowConfigItemVO> workflows,
                                                                   List<AgentConfigItemVO> agents) {
        if (workflows == null || workflows.isEmpty()) {
            return List.of();
        }
        Map<String, AgentConfigItemVO> agentMap = buildAgentMap(agents);

        List<WorkflowConfigItemVO> result = new ArrayList<>();
        for (WorkflowConfigItemVO item : workflows) {
            WorkflowConfigItemVO workflow = new WorkflowConfigItemVO();
            BeanUtils.copyProperties(item, workflow);

            List<String> currentSequence = item.getNodeSequence() == null ? List.of() : item.getNodeSequence();
            List<String> effectiveSequence = currentSequence.stream()
                    .filter(agentCode -> {
                        AgentConfigItemVO config = agentMap.get(agentCode);
                        if (config == null) {
                            return true;
                        }
                        if ("report_generation_agent".equals(agentCode)) {
                            return true;
                        }
                        return !Boolean.FALSE.equals(config.getEnabled());
                    })
                    .toList();

            if (effectiveSequence.isEmpty()) {
                effectiveSequence = currentSequence;
            }

            workflow.setNodeSequence(currentSequence);
            workflow.setNodeCount(currentSequence.size());
            workflow.setEntryAgent(effectiveSequence.isEmpty() ? item.getEntryAgent() : effectiveSequence.get(0));
            workflow.setEnabled(!Boolean.FALSE.equals(item.getEnabled()));
            workflow.setNodeTimeoutSummary(buildWorkflowTimeoutSummary(effectiveSequence, agentMap));
            result.add(workflow);
        }
        return result;
    }

    public Integer resolveWorkflowTimeoutSeconds(List<WorkflowConfigItemVO> workflows,
                                                 List<AgentConfigItemVO> agents,
                                                 int fallbackSeconds) {
        if (workflows == null || workflows.isEmpty()) {
            return fallbackSeconds;
        }
        Map<String, AgentConfigItemVO> agentMap = buildAgentMap(agents);

        WorkflowConfigItemVO workflow = workflows.stream()
                .filter(item -> !Boolean.FALSE.equals(item.getEnabled()))
                .filter(item -> Boolean.TRUE.equals(item.getDefaultSelected()))
                .findFirst()
                .orElseGet(() -> workflows.stream()
                        .filter(item -> !Boolean.FALSE.equals(item.getEnabled()))
                        .findFirst()
                        .orElse(workflows.get(0)));

        List<String> nodeSequence = (workflow.getNodeSequence() == null ? List.<String>of() : workflow.getNodeSequence()).stream()
                .filter(agentCode -> {
                    if ("report_generation_agent".equals(agentCode)) {
                        return true;
                    }
                    AgentConfigItemVO config = agentMap.get(agentCode);
                    return config == null || !Boolean.FALSE.equals(config.getEnabled());
                })
                .toList();
        int total = 0;
        for (String agentCode : nodeSequence) {
            AgentConfigItemVO config = agentMap.get(agentCode);
            if (config != null && config.getTimeoutSeconds() != null) {
                total += config.getTimeoutSeconds();
            }
        }
        return total > 0 ? total : fallbackSeconds;
    }

    private Map<String, AgentConfigItemVO> buildAgentMap(List<AgentConfigItemVO> agents) {
        return agents == null
                ? Collections.emptyMap()
                : agents.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getAgentCode() != null && !item.getAgentCode().isBlank())
                .collect(Collectors.toMap(AgentConfigItemVO::getAgentCode, item -> item, (left, right) -> right, LinkedHashMap::new));
    }

    private String buildWorkflowTimeoutSummary(List<String> nodeSequence, Map<String, AgentConfigItemVO> agentMap) {
        if (nodeSequence == null || nodeSequence.isEmpty()) {
            return "";
        }
        return nodeSequence.stream()
                .map(agentCode -> {
                    AgentConfigItemVO config = agentMap.get(agentCode);
                    Integer timeoutSeconds = config == null ? null : config.getTimeoutSeconds();
                    if (timeoutSeconds == null) {
                        return agentCode;
                    }
                    return agentCode + "=" + timeoutSeconds + "s";
                })
                .collect(Collectors.joining(", "));
    }
}
