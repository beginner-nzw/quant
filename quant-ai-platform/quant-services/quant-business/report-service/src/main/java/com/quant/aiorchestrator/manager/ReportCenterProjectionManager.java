package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.vo.ReportCenterListItemVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterPageVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterStatsVO;
import com.quant.aiorchestrator.report.ReportCenterRiskProjection;
import com.quant.aiorchestrator.report.ReportCenterRiskProjectionProvider;
import com.quant.aiorchestrator.report.ReportCenterTaskProjection;
import com.quant.aiorchestrator.report.ReportCenterTaskProjectionProvider;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReportCenterProjectionManager {

    private final ReportCenterTaskProjectionProvider reportCenterTaskProjectionProvider;
    private final ReportTaskPageReadManager reportTaskPageReadManager;
    private final ReportCenterRiskProjectionProvider reportCenterRiskProjectionProvider;
    private final ReportCenterItemAssembler reportCenterItemAssembler;

    public ReportCenterProjectionManager(ReportCenterTaskProjectionProvider reportCenterTaskProjectionProvider,
                                         ReportTaskPageReadManager reportTaskPageReadManager,
                                         ReportCenterRiskProjectionProvider reportCenterRiskProjectionProvider,
                                         ReportCenterItemAssembler reportCenterItemAssembler) {
        this.reportCenterTaskProjectionProvider = reportCenterTaskProjectionProvider;
        this.reportTaskPageReadManager = reportTaskPageReadManager;
        this.reportCenterRiskProjectionProvider = reportCenterRiskProjectionProvider;
        this.reportCenterItemAssembler = reportCenterItemAssembler;
    }

    public ReportCenterProjectionManager(ReportCenterTaskProjectionProvider reportCenterTaskProjectionProvider,
                                         ReportTaskPageReadManager reportTaskPageReadManager,
                                         ReportCenterRiskProjectionProvider reportCenterRiskProjectionProvider,
                                         ObjectMapper objectMapper) {
        this(
                reportCenterTaskProjectionProvider,
                reportTaskPageReadManager,
                reportCenterRiskProjectionProvider,
                new ReportCenterItemAssembler(objectMapper)
        );
    }

    public ReportCenterPageVO pageReportCenter(ReportCenterPageQueryDTO queryDTO) {
        ReportCenterPageQueryDTO safeQuery = queryDTO == null ? new ReportCenterPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<ReportCenterListItemVO> matchedRecords = listReportCenterRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        ReportCenterPageVO vo = new ReportCenterPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    public ReportCenterStatsVO getReportCenterStats() {
        List<ReportCenterListItemVO> records = listReportCenterRecords(new ReportCenterPageQueryDTO());
        ReportCenterStatsVO vo = new ReportCenterStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setHighConfidenceCount(records.stream().filter(item -> isHighConfidence(item.getConfidenceScore())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        vo.setApprovedCount(records.stream().filter(item -> ReportReviewStatusEnum.APPROVED.name().equals(item.getReviewStatus())).count());
        vo.setHumanReviewCount(records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedHumanReview())).count());
        return vo;
    }

    private List<ReportCenterListItemVO> listReportCenterRecords(ReportCenterPageQueryDTO queryDTO) {
        List<ResearchReportDO> reports = reportTaskPageReadManager.listReportCenterReports();

        if (reports.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ReportCenterTaskProjection> taskMap = reportCenterTaskProjectionProvider.loadTaskMapByTaskIds(taskIds);
        Map<String, ReportCenterRiskProjection> riskWarningMap = reportCenterRiskProjectionProvider.loadLatestRiskWarningMapByTaskIds(taskIds);

        return reports.stream()
                .map(report -> reportCenterItemAssembler.toReportCenterItem(
                        report,
                        taskMap.get(report.getTaskId()),
                        riskWarningMap.get(report.getTaskId())
                ))
                .filter(Objects::nonNull)
                .filter(item -> matchesReportCenterQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(ReportCenterListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ReportCenterListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private boolean matchesReportCenterQuery(ReportCenterListItemVO item, ReportCenterPageQueryDTO queryDTO) {
        if (item == null) {
            return false;
        }
        if (queryDTO == null) {
            return true;
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()
                && !queryDTO.getTargetCode().equalsIgnoreCase(item.getTargetCode())) {
            return false;
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()
                && !containsIgnoreCase(item.getTargetName(), queryDTO.getTargetName())) {
            return false;
        }
        if (queryDTO.getReportType() != null && !queryDTO.getReportType().isBlank()
                && !queryDTO.getReportType().equalsIgnoreCase(item.getReportType())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyHighConfidence()) && !isHighConfidence(item.getConfidenceScore())) {
            return false;
        }
        if (queryDTO.getNeedHumanReview() != null
                && !queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview())) {
            return false;
        }
        return true;
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }

    private boolean isHighConfidence(Double confidenceScore) {
        return confidenceScore != null && confidenceScore >= 0.8D;
    }
}
