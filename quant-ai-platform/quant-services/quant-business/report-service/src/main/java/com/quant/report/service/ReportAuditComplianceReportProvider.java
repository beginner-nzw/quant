package com.quant.report.service;

import com.quant.aiorchestrator.audit.AuditComplianceReportProjection;
import com.quant.aiorchestrator.audit.AuditComplianceReportProvider;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.manager.ReportTaskPageReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportAuditComplianceReportProvider implements AuditComplianceReportProvider {

    private final ReportTaskPageReadManager reportTaskPageReadManager;

    @Override
    public List<AuditComplianceReportProjection> listAuditComplianceReports() {
        return reportTaskPageReadManager.listAuditComplianceReports()
                .stream()
                .map(this::toProjection)
                .toList();
    }

    private AuditComplianceReportProjection toProjection(ResearchReportDO report) {
        return new AuditComplianceReportProjection(
                report.getReportId(),
                report.getTaskId(),
                report.getTaskType(),
                report.getReportType(),
                report.getFinalStatus(),
                report.getReviewStatus(),
                report.getReviewedBy(),
                report.getReviewedAt(),
                report.getReviewComment(),
                report.getNeedHumanReview(),
                report.getSummary(),
                report.getRevisedSummary(),
                report.getHighlights(),
                report.getRevisedHighlights(),
                report.getRiskPoints(),
                report.getRevisedRiskPoints(),
                report.getRiskWarnings(),
                report.getCreatedAt()
        );
    }
}
