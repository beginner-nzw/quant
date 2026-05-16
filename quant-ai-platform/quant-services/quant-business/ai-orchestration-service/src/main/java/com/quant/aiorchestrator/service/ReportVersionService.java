package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;

import java.util.List;

public interface ReportVersionService {

    void createSnapshot(ResearchReportDO report, String snapshotSource);

    List<ReportVersionVO> listVersions(String taskId);

    ReportVersionVO getVersion(String taskId, Integer versionNo);

    ReportVersionCompareVO compareVersions(String taskId, Integer fromVersionNo, Integer toVersionNo);
}
