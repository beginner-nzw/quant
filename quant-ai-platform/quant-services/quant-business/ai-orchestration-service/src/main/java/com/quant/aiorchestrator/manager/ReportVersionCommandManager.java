package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportVersionDO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportVersionCommandManager {

    private final ReportVersionStoreManager reportVersionStoreManager;
    private final ReportVersionProjectionManager reportVersionProjectionManager;
    private final ReportVersionCompareManager reportVersionCompareManager;

    public void createSnapshot(ResearchReportDO report, String snapshotSource) {
        if (report == null || report.getReportId() == null || report.getTaskId() == null) {
            return;
        }
        int versionNo = reportVersionProjectionManager.defaultVersionNo(report.getVersionNo());
        ResearchReportVersionDO existing = reportVersionStoreManager.selectByReportAndVersion(report.getReportId(), versionNo);
        if (existing != null) {
            return;
        }

        reportVersionStoreManager.insert(reportVersionProjectionManager.buildSnapshotEntity(report, snapshotSource));
    }

    public List<ReportVersionVO> listVersions(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return List.of();
        }
        return reportVersionStoreManager.listByTaskId(taskId)
                .stream()
                .map(reportVersionProjectionManager::toVO)
                .toList();
    }

    public ReportVersionVO getVersion(String taskId, Integer versionNo) {
        if (taskId == null || taskId.isBlank() || versionNo == null || versionNo < 1) {
            return null;
        }
        ResearchReportVersionDO entity = reportVersionStoreManager.selectByTaskAndVersion(taskId, versionNo);
        return entity == null ? null : reportVersionProjectionManager.toVO(entity);
    }

    public ReportVersionCompareVO compareVersions(String taskId, Integer fromVersionNo, Integer toVersionNo) {
        if (taskId == null || taskId.isBlank()
                || fromVersionNo == null || fromVersionNo < 1
                || toVersionNo == null || toVersionNo < 1) {
            return null;
        }
        ResearchReportVersionDO fromEntity = reportVersionStoreManager.selectByTaskAndVersion(taskId, fromVersionNo);
        ResearchReportVersionDO toEntity = reportVersionStoreManager.selectByTaskAndVersion(taskId, toVersionNo);
        if (fromEntity == null || toEntity == null) {
            return null;
        }
        return reportVersionCompareManager.compare(
                taskId,
                reportVersionProjectionManager.toVO(fromEntity),
                reportVersionProjectionManager.toVO(toEntity)
        );
    }
}
