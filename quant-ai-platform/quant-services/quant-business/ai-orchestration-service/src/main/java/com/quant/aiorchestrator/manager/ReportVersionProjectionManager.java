package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportVersionDO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportVersionProjectionManager {

    private final ReportVersionSnapshotManager reportVersionSnapshotManager;

    public ResearchReportVersionDO buildSnapshotEntity(ResearchReportDO report, String snapshotSource) {
        int versionNo = defaultVersionNo(report.getVersionNo());
        ResearchReportVersionDO entity = new ResearchReportVersionDO();
        entity.setVersionId(UUID.randomUUID().toString());
        entity.setReportId(report.getReportId());
        entity.setTaskId(report.getTaskId());
        entity.setVersionNo(versionNo);
        entity.setSnapshotSource(normalizeSnapshotSource(snapshotSource));
        entity.setSnapshotPayload(reportVersionSnapshotManager.toJson(
                reportVersionSnapshotManager.buildSnapshot(report, versionNo)
        ));
        entity.setDeleted(0);
        return entity;
    }

    public ReportVersionVO toVO(ResearchReportVersionDO entity) {
        ReportVersionVO vo = new ReportVersionVO();
        vo.setVersionId(entity.getVersionId());
        vo.setReportId(entity.getReportId());
        vo.setTaskId(entity.getTaskId());
        vo.setVersionNo(defaultVersionNo(entity.getVersionNo()));
        vo.setSnapshotSource(entity.getSnapshotSource());
        vo.setSnapshot(reportVersionSnapshotManager.readSnapshot(entity.getSnapshotPayload()));
        vo.setCreatedAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString());
        return vo;
    }

    public int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }

    private String normalizeSnapshotSource(String snapshotSource) {
        if (snapshotSource == null || snapshotSource.isBlank()) {
            return "UNKNOWN";
        }
        return snapshotSource.length() <= 32 ? snapshotSource : snapshotSource.substring(0, 32);
    }
}
