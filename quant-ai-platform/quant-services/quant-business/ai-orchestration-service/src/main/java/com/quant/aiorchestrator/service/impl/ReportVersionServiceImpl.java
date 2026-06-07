package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.manager.ReportVersionCommandManager;
import com.quant.aiorchestrator.service.ReportVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportVersionServiceImpl implements ReportVersionService {

    private final ReportVersionCommandManager reportVersionCommandManager;

    @Override
    public void createSnapshot(ResearchReportDO report, String snapshotSource) {
        reportVersionCommandManager.createSnapshot(report, snapshotSource);
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
