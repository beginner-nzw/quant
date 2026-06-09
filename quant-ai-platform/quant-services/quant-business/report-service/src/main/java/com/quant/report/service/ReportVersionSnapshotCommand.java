package com.quant.report.service;

import com.quant.aiorchestrator.domain.entity.ResearchReportDO;

public interface ReportVersionSnapshotCommand {

    void createSnapshot(ResearchReportDO report, String snapshotSource);
}
