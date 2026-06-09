package com.quant.report.service;

import com.quant.aiorchestrator.audit.HumanReviewQueueReportProjection;
import com.quant.aiorchestrator.audit.HumanReviewQueueReportProvider;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.manager.ReportTaskPageReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportHumanReviewQueueReportProvider implements HumanReviewQueueReportProvider {

    private final ReportTaskPageReadManager reportTaskPageReadManager;

    @Override
    public List<HumanReviewQueueReportProjection> listHumanReviewQueueReports() {
        return reportTaskPageReadManager.listHumanReviewQueueReports()
                .stream()
                .map(this::toProjection)
                .toList();
    }

    private HumanReviewQueueReportProjection toProjection(ResearchReportDO report) {
        return new HumanReviewQueueReportProjection(
                report.getReportId(),
                report.getTaskId(),
                report.getTaskType(),
                report.getReportType(),
                report.getReviewStatus(),
                report.getReviewedBy(),
                report.getReviewedAt(),
                report.getReviewComment(),
                report.getNeedHumanReview(),
                report.getSummary(),
                report.getRevisedSummary(),
                report.getRiskPoints(),
                report.getRevisedRiskPoints(),
                report.getRiskWarnings(),
                report.getCreatedAt()
        );
    }
}
