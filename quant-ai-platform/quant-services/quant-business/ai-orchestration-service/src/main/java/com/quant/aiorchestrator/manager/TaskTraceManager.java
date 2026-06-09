package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.mapper.AiAgentExecutionMapper;
import com.quant.aiorchestrator.mapper.AiWorkflowInstanceMapper;
import com.quant.config.api.AgentConfigItem;
import com.quant.config.api.WorkflowConfigItem;
import com.quant.config.port.AgentConfigQueryPort;
import com.quant.config.port.WorkflowConfigQueryPort;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.task.port.TaskTraceStepAppender;
import com.quant.task.port.TaskWorkflowTraceFinisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskTraceManager implements TaskWorkflowTraceFinisher {

    private final AiWorkflowInstanceMapper aiWorkflowInstanceMapper;
    private final AiAgentExecutionMapper aiAgentExecutionMapper;
    private final TaskTraceStepAppender taskTraceStepAppender;
    private final AgentConfigQueryPort agentConfigQueryPort;
    private final WorkflowConfigQueryPort workflowConfigQueryPort;

    public void createWorkflowIfAbsent(String workflowInstanceId, String taskId, String taskType, String currentNode) {
        Long count = aiWorkflowInstanceMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getWorkflowInstanceId, workflowInstanceId)
        );
        if (count != null && count > 0) {
            return;
        }

        AiWorkflowInstanceDO workflow = new AiWorkflowInstanceDO();
        WorkflowConfigItem workflowConfig = workflowConfigQueryPort.resolveWorkflow(taskType);
        workflow.setWorkflowInstanceId(workflowInstanceId);
        workflow.setTaskId(taskId);
        workflow.setWorkflowCode(workflowConfig == null ? "stock_research_workflow" : workflowConfig.getWorkflowCode());
        workflow.setWorkflowVersion(workflowConfig == null ? "1.0.0" : workflowConfig.getWorkflowVersion());
        workflow.setEntryAgent(resolveEntryAgent(workflowConfig));
        workflow.setCurrentNode(currentNode);
        workflow.setStatus(TaskStatusEnum.RUNNING.name());
        workflow.setStartTime(LocalDateTime.now());
        workflow.setDeleted(0);
        aiWorkflowInstanceMapper.insert(workflow);
    }

    public void appendStep(String taskId, String stage, String node, int progress) {
        Integer order = resolveExecutionOrder(node);
        taskTraceStepAppender.appendSucceededStep(taskId, stage, node, order);
    }

    public void appendAgentExecution(String workflowInstanceId,
                                     String taskId,
                                     String agentCode,
                                     String agentName,
                                     String nodeCode,
                                     Double confidenceScore,
                                     boolean needHumanReview,
                                     Long durationMs) {

        AiAgentExecutionDO execution = new AiAgentExecutionDO();
        execution.setExecutionId(UUID.randomUUID().toString());
        execution.setWorkflowInstanceId(workflowInstanceId);
        execution.setTaskId(taskId);
        execution.setAgentCode(agentCode);
        execution.setAgentName(agentName);
        execution.setNodeCode(nodeCode);
        execution.setStatus(TaskStatusEnum.SUCCESS.name());
        execution.setConfidenceScore(confidenceScore == null ? null : BigDecimal.valueOf(confidenceScore));
        execution.setNeedHumanReview(needHumanReview ? 1 : 0);
        execution.setStartTime(LocalDateTime.now());
        execution.setFinishTime(LocalDateTime.now());
        execution.setDurationMs(durationMs == null ? 0L : durationMs);
        execution.setDeleted(0);
        aiAgentExecutionMapper.insert(execution);
    }

    public void finishWorkflow(String workflowInstanceId, String finalNode, String finalStatus) {
        AiWorkflowInstanceDO workflow = aiWorkflowInstanceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getWorkflowInstanceId, workflowInstanceId)
                        .last("limit 1")
        );
        if (workflow == null) {
            workflow = new AiWorkflowInstanceDO();
            workflow.setWorkflowInstanceId(workflowInstanceId);
            workflow.setTaskId(resolveTaskId(workflowInstanceId));
            workflow.setWorkflowCode("stock_research_workflow");
            workflow.setWorkflowVersion("1.0.0");
            workflow.setEntryAgent("planner_agent");
            workflow.setDeleted(0);
            workflow.setStartTime(LocalDateTime.now());
            workflow.setCurrentNode(finalNode);
            workflow.setStatus(finalStatus == null || finalStatus.isBlank() ? TaskStatusEnum.SUCCESS.name() : finalStatus);
            workflow.setFinishTime(LocalDateTime.now());
            aiWorkflowInstanceMapper.insert(workflow);
            return;
        }
        workflow.setCurrentNode(finalNode);
        workflow.setStatus(finalStatus == null || finalStatus.isBlank() ? TaskStatusEnum.SUCCESS.name() : finalStatus);
        workflow.setFinishTime(LocalDateTime.now());
        aiWorkflowInstanceMapper.updateById(workflow);
    }

    public void updateWorkflowProgress(String workflowInstanceId, String currentNode) {
        AiWorkflowInstanceDO workflow = aiWorkflowInstanceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getWorkflowInstanceId, workflowInstanceId)
                        .last("limit 1")
        );
        if (workflow == null) {
            return;
        }
        if (TaskStatusEnum.SUCCESS.name().equals(workflow.getStatus())
                || TaskStatusEnum.FAILED.name().equals(workflow.getStatus())
                || TaskStatusEnum.CANCELLED.name().equals(workflow.getStatus())) {
            return;
        }
        workflow.setCurrentNode(currentNode);
        workflow.setStatus(TaskStatusEnum.RUNNING.name());
        aiWorkflowInstanceMapper.updateById(workflow);
    }

    private String resolveTaskId(String workflowInstanceId) {
        if (workflowInstanceId == null || workflowInstanceId.isBlank()) {
            return workflowInstanceId;
        }
        if (workflowInstanceId.startsWith("wf-") && workflowInstanceId.length() > 3) {
            return workflowInstanceId.substring(3);
        }
        return workflowInstanceId;
    }

    private int resolveExecutionOrder(String nodeCode) {
        List<? extends AgentConfigItem> agents = agentConfigQueryPort.loadAgents();
        return agents.stream()
                .filter(item -> nodeCode.equals(item.getAgentCode()))
                .map(AgentConfigItem::getExecutionOrder)
                .filter(order -> order != null && order > 0)
                .findFirst()
                .orElseGet(() -> switch (nodeCode) {
                    case "planner_agent" -> 1;
                    case "intent_agent" -> 2;
                    case "financial_analysis_agent" -> 3;
                    case "risk_review_agent" -> 4;
                    case "report_generation_agent" -> 5;
                    default -> 99;
                });
    }

    private String resolveEntryAgent(WorkflowConfigItem workflowConfig) {
        List<String> configuredSequence = workflowConfig == null || workflowConfig.getNodeSequence() == null
                ? List.of()
                : workflowConfig.getNodeSequence();
        List<? extends AgentConfigItem> agents = agentConfigQueryPort.loadAgents();
        for (String agentCode : configuredSequence) {
            if ("report_generation_agent".equals(agentCode)) {
                return agentCode;
            }
            AgentConfigItem config = agents.stream()
                    .filter(item -> agentCode.equals(item.getAgentCode()))
                    .findFirst()
                    .orElse(null);
            if (config == null || !Boolean.FALSE.equals(config.getEnabled())) {
                return agentCode;
            }
        }
        return agents.stream()
                .filter(item -> !Boolean.FALSE.equals(item.getEnabled()))
                .sorted((left, right) -> Integer.compare(
                        left.getExecutionOrder() == null ? Integer.MAX_VALUE : left.getExecutionOrder(),
                        right.getExecutionOrder() == null ? Integer.MAX_VALUE : right.getExecutionOrder()
                ))
                .map(AgentConfigItem::getAgentCode)
                .findFirst()
                .orElse("planner_agent");
    }
}
