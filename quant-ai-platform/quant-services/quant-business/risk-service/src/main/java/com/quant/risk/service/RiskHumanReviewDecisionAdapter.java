package com.quant.risk.service;

import com.quant.aiorchestrator.audit.HumanReviewRiskDecisionPort;
import com.quant.aiorchestrator.audit.HumanReviewRiskDecisionResult;
import com.quant.aiorchestrator.manager.RiskHumanReviewDecisionManager;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskHumanReviewDecisionAdapter implements HumanReviewRiskDecisionPort {

    private final RiskHumanReviewDecisionManager riskHumanReviewDecisionManager;

    @Override
    public HumanReviewRiskDecisionResult decideRisk(String taskId,
                                                    String reviewerId,
                                                    String reviewComment,
                                                    ReportReviewStatusEnum decision) {
        return riskHumanReviewDecisionManager.decideLatestRisk(taskId, reviewerId, reviewComment, decision);
    }
}
