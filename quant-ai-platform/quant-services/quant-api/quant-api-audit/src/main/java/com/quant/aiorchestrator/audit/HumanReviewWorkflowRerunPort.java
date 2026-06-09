package com.quant.aiorchestrator.audit;

import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;

public interface HumanReviewWorkflowRerunPort {
    void rerunWorkflow(String taskId, HumanReviewDecisionDTO dto);
}
