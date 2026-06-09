package com.quant.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.audit.ReportHumanReviewRecordProjection;
import com.quant.aiorchestrator.audit.ReportHumanReviewRecordReadPort;
import com.quant.aiorchestrator.audit.ReportHumanReviewRecordWriteCommand;
import com.quant.aiorchestrator.audit.ReportHumanReviewRecordWritePort;
import com.quant.aiorchestrator.domain.entity.HumanReviewRecordDO;
import com.quant.aiorchestrator.manager.HumanReviewRecordWriteManager;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditReportHumanReviewRecordAdapter implements ReportHumanReviewRecordReadPort, ReportHumanReviewRecordWritePort {

    private final HumanReviewRecordMapper humanReviewRecordMapper;
    private final HumanReviewRecordWriteManager humanReviewRecordWriteManager;

    @Override
    public List<ReportHumanReviewRecordProjection> listReportReviewRecords(String reportId) {
        if (reportId == null || reportId.isBlank()) {
            return List.of();
        }
        List<HumanReviewRecordDO> records = humanReviewRecordMapper.selectList(
                new LambdaQueryWrapper<HumanReviewRecordDO>()
                        .eq(HumanReviewRecordDO::getRelatedObjectType, "REPORT")
                        .eq(HumanReviewRecordDO::getRelatedObjectId, reportId)
                        .eq(HumanReviewRecordDO::getDeleted, 0)
                        .orderByDesc(HumanReviewRecordDO::getId)
        );
        return records == null ? List.of() : records.stream().map(this::toProjection).toList();
    }

    @Override
    public void insertReportReviewRecord(ReportHumanReviewRecordWriteCommand command) {
        if (command == null) {
            return;
        }
        humanReviewRecordWriteManager.insertReviewRecord(
                command.taskId(),
                "REPORT",
                command.reportId(),
                command.reviewerId(),
                command.reviewerRole(),
                command.reviewResult(),
                command.reviewComment(),
                command.beforeSnapshot(),
                command.afterSnapshot(),
                command.traceId(),
                command.tenantId()
        );
    }

    private ReportHumanReviewRecordProjection toProjection(HumanReviewRecordDO record) {
        return new ReportHumanReviewRecordProjection(
                record.getReviewId(),
                record.getTaskId(),
                record.getRelatedObjectType(),
                record.getRelatedObjectId(),
                record.getReviewerId(),
                record.getReviewerRole(),
                record.getReviewResult(),
                record.getReviewComment(),
                record.getBeforeSnapshotRef(),
                record.getAfterSnapshotRef(),
                record.getBeforeSnapshot(),
                record.getAfterSnapshot(),
                record.getTraceId(),
                record.getTenantId(),
                record.getCreatedAt()
        );
    }
}
