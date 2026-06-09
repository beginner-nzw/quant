package com.quant.aiorchestrator.audit;

import com.quant.common.model.enums.ReportReviewStatusEnum;

public interface HumanReviewRiskDecisionPort {
    HumanReviewRiskDecisionResult decideRisk(String taskId,
                                             String reviewerId,
                                             String reviewComment,
                                             ReportReviewStatusEnum decision);
}
