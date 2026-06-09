package com.quant.task.service;

import com.quant.aiorchestrator.audit.HumanReviewWorkflowRerunPort;
import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
import com.quant.aiorchestrator.service.TaskControlService;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResearchTaskHumanReviewWorkflowRerunAdapter implements HumanReviewWorkflowRerunPort {

    private final TaskControlService taskControlService;

    @Override
    public void rerunWorkflow(String taskId, HumanReviewDecisionDTO dto) {
        TaskWorkflowControlDTO controlDTO = new TaskWorkflowControlDTO();
        controlDTO.setOperatorId(firstText(dto.getReviewedBy(), SecurityUtils.currentUserId(), "human-reviewer"));
        controlDTO.setReason(firstText(dto.getReviewComment(), "human review requested rerun"));
        controlDTO.setNodeName(firstText(dto.getRerunNodeName(), "report_generation_agent"));
        taskControlService.rerunNode(taskId, controlDTO);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
