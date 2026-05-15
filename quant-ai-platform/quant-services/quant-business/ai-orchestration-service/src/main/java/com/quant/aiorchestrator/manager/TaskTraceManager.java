package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskStepDO;
import com.quant.aiorchestrator.domain.vo.AgentConfigItemVO;
import com.quant.aiorchestrator.domain.vo.WorkflowConfigItemVO;
import com.quant.aiorchestrator.mapper.AiAgentExecutionMapper;
import com.quant.aiorchestrator.mapper.AiWorkflowInstanceMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskStepMapper;
import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.WorkflowConfigService;
import com.quant.common.model.enums.TaskStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskTraceManager {

    private final AiWorkflowInstanceMapper aiWorkflowInstanceMapper;
    private final AiAgentExecutionMapper aiAgentExecutionMapper;
    private final ResearchTaskStepMapper researchTaskStepMapper;
    private final AgentConfigService agentConfigService;
    private final WorkflowConfigService workflowConfigService;

    public void createWorkflowIfAbsent(String workflowInstanceId, String taskId, String taskType, String currentNode) {
        Long count = aiWorkflowInstanceMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getWorkflowInstanceId, workflowInstanceId)
        );
        if (count != null && count > 0) {
            return;
        }

        AiWorkflowInstanceDO workflow = new AiWorkflowInstanceDO();
        WorkflowConfigItemVO workflowConfig = workflowConfigService.resolveWorkflow(taskType);
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

        ResearchTaskStepDO step = new ResearchTaskStepDO();
        step.setTaskId(taskId);
        step.setStepCode(stage);
        step.setStepName(stage);
        step.setAgentCode(node);
        step.setExecutionOrder(order);
        step.setStatus(TaskStatusEnum.SUCCESS.name());
        step.setStartTime(LocalDateTime.now());
        step.setFinishTime(LocalDateTime.now());
        step.setDurationMs(0L);
        step.setDeleted(0);
        researchTaskStepMapper.insert(step);
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
        List<AgentConfigItemVO> agents = agentConfigService.loadAgents();
        return agents.stream()
                .filter(item -> nodeCode.equals(item.getAgentCode()))
                .map(AgentConfigItemVO::getExecutionOrder)
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

    private String resolveEntryAgent(WorkflowConfigItemVO workflowConfig) {
        List<String> configuredSequence = workflowConfig == null || workflowConfig.getNodeSequence() == null
                ? List.of()
                : workflowConfig.getNodeSequence();
        List<AgentConfigItemVO> agents = agentConfigService.loadAgents();
        for (String agentCode : configuredSequence) {
            if ("report_generation_agent".equals(agentCode)) {
                return agentCode;
            }
            AgentConfigItemVO config = agents.stream()
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
                .map(AgentConfigItemVO::getAgentCode)
                .findFirst()
                .orElse("planner_agent");
    }
}
