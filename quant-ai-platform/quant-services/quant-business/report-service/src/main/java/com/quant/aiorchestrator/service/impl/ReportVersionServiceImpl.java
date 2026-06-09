package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.manager.ReportVersionCommandManager;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.report.service.ReportVersionSnapshotCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportVersionServiceImpl implements ReportVersionService, ReportVersionSnapshotCommand {

    private final ReportVersionCommandManager reportVersionCommandManager;

    @Override
    public void createSnapshot(ResearchReportSnapshot report, String snapshotSource) {
        reportVersionCommandManager.createSnapshot(toEntity(report), snapshotSource);
    }

    @Override
    public void createSnapshot(ResearchReportDO report, String snapshotSource) {
        reportVersionCommandManager.createSnapshot(report, snapshotSource);
    }

    private ResearchReportDO toEntity(ResearchReportSnapshot report) {
        if (report == null) {
            return null;
        }
        ResearchReportDO entity = new ResearchReportDO();
        entity.setReportId(report.getReportId());
        entity.setTaskId(report.getTaskId());
        entity.setVersionNo(report.getVersionNo());
        entity.setTaskType(report.getTaskType());
        entity.setFinalStatus(report.getFinalStatus());
        entity.setSummary(report.getSummary());
        entity.setConfidenceScore(report.getConfidenceScore());
        entity.setNeedHumanReview(report.getNeedHumanReview());
        entity.setReportType(report.getReportType());
        entity.setHighlights(report.getHighlights());
        entity.setRiskPoints(report.getRiskPoints());
        entity.setRiskWarnings(report.getRiskWarnings());
        entity.setResultRef(report.getResultRef());
        entity.setRawPayload(report.getRawPayload());
        entity.setReviewStatus(report.getReviewStatus());
        entity.setReviewedBy(report.getReviewedBy());
        entity.setRevisedSummary(report.getRevisedSummary());
        entity.setRevisedHighlights(report.getRevisedHighlights());
        entity.setRevisedRiskPoints(report.getRevisedRiskPoints());
        entity.setReviewComment(report.getReviewComment());
        return entity;
    }

    @Override
    public List<ReportVersionVO> listVersions(String taskId) {
        return reportVersionCommandManager.listVersions(taskId);
    }

    @Override
    public ReportVersionVO getVersion(String taskId, Integer versionNo) {
        return reportVersionCommandManager.getVersion(taskId, versionNo);
    }

    @Override
    public ReportVersionCompareVO compareVersions(String taskId, Integer fromVersionNo, Integer toVersionNo) {
        return reportVersionCommandManager.compareVersions(taskId, fromVersionNo, toVersionNo);
    }
}
