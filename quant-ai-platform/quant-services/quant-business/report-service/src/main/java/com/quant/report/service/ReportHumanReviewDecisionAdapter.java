package com.quant.report.service;

import com.quant.aiorchestrator.audit.HumanReviewReportDecisionPort;
import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.service.TaskReportService;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportHumanReviewDecisionAdapter implements HumanReviewReportDecisionPort {

    private final TaskReportService taskReportService;

    @Override
    public void decideReport(String taskId, HumanReviewDecisionDTO dto, ReportReviewStatusEnum decision) {
        TaskReportReviewDTO reviewDTO = new TaskReportReviewDTO();
        reviewDTO.setReviewStatus(decision.name());
        reviewDTO.setReviewedBy(firstText(dto.getReviewedBy(), SecurityUtils.currentUserId(), "human-reviewer"));
        reviewDTO.setReviewComment(dto.getReviewComment());
        reviewDTO.setRevisedSummary(dto.getRevisedSummary());
        reviewDTO.setRevisedHighlights(dto.getRevisedHighlights());
        reviewDTO.setRevisedRiskPoints(dto.getRevisedRiskPoints());
        taskReportService.reviewReport(taskId, reviewDTO);
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
