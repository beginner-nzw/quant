package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.report.TaskReportReadPort;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportTaskPageReadManager implements TaskReportReadPort {

    private final ResearchReportMapper researchReportMapper;

    @Override
    public Set<String> findTaskIdsByReviewFilter(Boolean onlyPendingReview,
                                                 String reportReviewStatus,
                                                 String reportReviewedBy) {
        LambdaQueryWrapper<ResearchReportDO> wrapper = new LambdaQueryWrapper<ResearchReportDO>()
                .eq(ResearchReportDO::getDeleted, 0);

        boolean needReportFilter = false;

        String targetReviewStatus;
        if (Boolean.TRUE.equals(onlyPendingReview)) {
            targetReviewStatus = ReportReviewStatusEnum.PENDING.name();
        } else if (reportReviewStatus != null && !reportReviewStatus.isBlank()) {
            targetReviewStatus = reportReviewStatus;
        } else {
            targetReviewStatus = null;
        }

        if (targetReviewStatus != null) {
            if (ReportReviewStatusEnum.PENDING.name().equals(targetReviewStatus)) {
                wrapper.and(query -> query
                        .isNull(ResearchReportDO::getReviewStatus)
                        .or()
                        .eq(ResearchReportDO::getReviewStatus, targetReviewStatus));
            } else {
                wrapper.eq(ResearchReportDO::getReviewStatus, targetReviewStatus);
            }
            needReportFilter = true;
        }

        if (reportReviewedBy != null && !reportReviewedBy.isBlank()) {
            wrapper.like(ResearchReportDO::getReviewedBy, reportReviewedBy);
            needReportFilter = true;
        }

        if (!needReportFilter) {
            return null;
        }

        return researchReportMapper.selectList(wrapper).stream()
                .map(ResearchReportDO::getTaskId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<TaskReportProjection> listReportsByTaskIds(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        return researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .in(ResearchReportDO::getTaskId, taskIds)
        ).stream().map(this::toTaskReportProjection).toList();
    }

    public ResearchReportDO selectCurrentReportByTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return null;
        }
        return researchReportMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getTaskId, taskId)
                        .eq(ResearchReportDO::getDeleted, 0)
                        .last("limit 1")
        );
    }

    @Override
    public List<TaskReportProjection> listReportsByTaskIdSet(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        return researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .in(ResearchReportDO::getTaskId, taskIds)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        ).stream().map(this::toTaskReportProjection).toList();
    }

    @Override
    public List<TaskReportProjection> listActiveReports() {
        return listReportCenterReports().stream()
                .map(this::toTaskReportProjection)
                .toList();
    }

    public List<ResearchReportDO> listReportCenterReports() {
        return researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );
    }

    public List<ResearchReportDO> listHumanReviewQueueReports() {
        return listReportCenterReports();
    }

    public List<ResearchReportDO> listAuditComplianceReports() {
        return listReportCenterReports();
    }

    private TaskReportProjection toTaskReportProjection(ResearchReportDO report) {
        if (report == null) {
            return null;
        }
        return new TaskReportProjection(
                report.getReportId(),
                report.getTaskId(),
                report.getTaskType(),
                report.getFinalStatus(),
                report.getSummary(),
                report.getConfidenceScore(),
                report.getNeedHumanReview(),
                report.getReportType(),
                report.getHighlights(),
                report.getRiskPoints(),
                report.getRiskWarnings(),
                report.getResultRef(),
                report.getCreatedAt(),
                report.getReviewStatus(),
                report.getReviewedBy(),
                report.getReviewedAt(),
                report.getRevisedSummary(),
                report.getRevisedHighlights(),
                report.getRevisedRiskPoints(),
                report.getReviewComment()
        );
    }
}
