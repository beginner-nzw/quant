package com.quant.aiorchestrator.audit;

import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;

public interface HumanReviewReportDecisionPort {
    void decideReport(String taskId,
                      HumanReviewDecisionDTO dto,
                      ReportReviewStatusEnum decision);
}
