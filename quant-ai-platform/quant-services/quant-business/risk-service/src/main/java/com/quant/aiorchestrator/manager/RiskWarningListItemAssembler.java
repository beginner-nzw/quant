package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.vo.RiskWarningListItemVO;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.task.risk.RiskWarningTaskProjection;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class RiskWarningListItemAssembler {

    private final RiskWarningRuleManager ruleManager;

    public RiskWarningListItemAssembler(RiskWarningRuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }

    public RiskWarningListItemVO toRiskWarningItem(RiskWarningDO warning,
                                                   RiskWarningTaskProjection task,
                                                   TaskReportProjection report,
                                                   RiskWarningFollowUpSummaryManager.FollowUpSummary followUpSummary,
                                                   List<RiskWarningDetailDO> details) {
        if (warning == null) {
            return null;
        }

        List<String> riskReasons = buildDomainRiskReasons(warning, details);
        RiskWarningRuleManager.RiskProjection riskProjection = ruleManager.resolveRiskProjection(report, warning, details);
        boolean needHumanReview = riskProjection.needHumanReview();
        ReportReviewStatusEnum reviewStatus = ruleManager.resolveReviewStatus(
                warning.getReviewStatus() == null && report != null ? report.getReviewStatus() : warning.getReviewStatus()
        );

        RiskWarningListItemVO vo = new RiskWarningListItemVO();
        vo.setTaskId(warning.getTaskId());
        vo.setTaskTitle(task == null ? warning.getWarningSummary() : task.taskTitle());
        vo.setTaskType(task == null ? null : task.taskType());
        vo.setTargetCode(warning.getEntityCode());
        vo.setTargetName(warning.getEntityName());
        vo.setPriority(task == null ? null : task.priority());
        vo.setTaskStatus(task == null ? null : task.status());
        vo.setCurrentStage(task == null ? null : task.currentStage());
        vo.setReportId(report == null ? null : report.getReportId());
        vo.setReportType(report == null ? null : report.getReportType());
        vo.setFinalStatus(report == null ? null : report.getFinalStatus());
        vo.setRiskLevel(riskProjection.riskLevel().name());
        vo.setWarningCount(1);
        vo.setRiskPointCount(details == null ? 0 : details.size());
        vo.setTotalRiskCount(1 + (details == null ? 0 : details.size()));
        vo.setNeedHumanReview(needHumanReview);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(warning.getReviewerId() == null && report != null ? report.getReviewedBy() : warning.getReviewerId());
        vo.setReportReviewedAt(warning.getReviewTime() == null && report != null ? report.getReviewedAt() : warning.getReviewTime());
        vo.setRevised(report != null && ruleManager.isReportRevised(report));
        vo.setSummaryRevised(report != null && ruleManager.isSummaryRevised(report));
        vo.setHighlightsRevised(report != null && ruleManager.isHighlightsRevised(report));
        vo.setRiskPointsRevised(report != null && ruleManager.isRiskPointsRevised(report));
        applyFollowUpSummary(vo, followUpSummary);
        vo.setReviewComment(report == null ? null : report.getReviewComment());
        vo.setSummary(warning.getWarningSummary());
        vo.setRiskReasons(riskReasons);
        vo.setRiskSourceTags(buildDomainRiskSourceTags(warning, needHumanReview, reviewStatus));
        vo.setCreatedAt(warning.getCreatedAt());
        return vo;
    }

    public RiskWarningListItemVO toRiskWarningItem(TaskReportProjection report,
                                                   RiskWarningTaskProjection task,
                                                   RiskWarningFollowUpSummaryManager.FollowUpSummary followUpSummary) {
        if (report == null || task == null) {
            return null;
        }

        List<String> warningList = ruleManager.readTextList(report.getRiskWarnings());
        List<String> riskPointList = ruleManager.readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints());
        boolean needHumanReview = report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1;

        if (!needHumanReview && warningList.isEmpty() && riskPointList.isEmpty()) {
            return null;
        }

        int totalRiskCount = warningList.size() + riskPointList.size();
        ReportReviewStatusEnum reviewStatus = ruleManager.resolveReviewStatus(report.getReviewStatus());
        RiskLevelEnum riskLevel = ruleManager.resolveRiskProjection(report, null).riskLevel();

        RiskWarningListItemVO vo = new RiskWarningListItemVO();
        vo.setTaskId(task.taskId());
        vo.setTaskTitle(task.taskTitle());
        vo.setTaskType(task.taskType());
        vo.setTargetCode(task.targetCode());
        vo.setTargetName(task.targetName());
        vo.setPriority(task.priority());
        vo.setTaskStatus(task.status());
        vo.setCurrentStage(task.currentStage());
        vo.setReportId(report.getReportId());
        vo.setReportType(report.getReportType());
        vo.setFinalStatus(report.getFinalStatus());
        vo.setRiskLevel(riskLevel.name());
        vo.setWarningCount(warningList.size());
        vo.setRiskPointCount(riskPointList.size());
        vo.setTotalRiskCount(totalRiskCount);
        vo.setNeedHumanReview(needHumanReview);
        vo.setReportReviewStatus(reviewStatus.name());
        vo.setReportReviewedBy(report.getReviewedBy());
        vo.setReportReviewedAt(report.getReviewedAt());
        vo.setRevised(ruleManager.isReportRevised(report));
        vo.setSummaryRevised(ruleManager.isSummaryRevised(report));
        vo.setHighlightsRevised(ruleManager.isHighlightsRevised(report));
        vo.setRiskPointsRevised(ruleManager.isRiskPointsRevised(report));
        applyFollowUpSummary(vo, followUpSummary);
        vo.setReviewComment(report.getReviewComment());
        vo.setSummary(ruleManager.resolveDisplaySummary(report.getRevisedSummary(), report.getSummary()));
        vo.setRiskReasons(mergeRiskReasons(warningList, riskPointList));
        vo.setRiskSourceTags(buildRiskSourceTags(warningList, riskPointList, needHumanReview, reviewStatus));
        vo.setCreatedAt(firstNonNull(report.getCreatedAt(), task.createdAt()));
        return vo;
    }

    public boolean matchesRiskWarningQuery(RiskWarningListItemVO item, RiskWarningPageQueryDTO queryDTO) {
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
        RiskLevelEnum riskLevel = RiskLevelEnum.from(queryDTO.getRiskLevel());
        if (riskLevel != null && !riskLevel.name().equals(item.getRiskLevel())) {
            return false;
        }
        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(queryDTO.getReportReviewStatus());
        if (reviewStatus != null && !reviewStatus.name().equals(item.getReportReviewStatus())) {
            return false;
        }
        return queryDTO.getNeedHumanReview() == null || queryDTO.getNeedHumanReview().equals(item.getNeedHumanReview());
    }

    public List<RiskWarningListItemVO> sortRiskWarningRecords(List<RiskWarningListItemVO> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .sorted(Comparator
                        .comparingInt((RiskWarningListItemVO item) -> riskLevelRank(item.getRiskLevel()))
                        .reversed()
                        .thenComparing(
                                RiskWarningListItemVO::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                RiskWarningListItemVO::getTaskId,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .toList();
    }

    private void applyFollowUpSummary(RiskWarningListItemVO vo,
                                      RiskWarningFollowUpSummaryManager.FollowUpSummary followUpSummary) {
        if (followUpSummary == null) {
            return;
        }
        vo.setFollowUpStatus(followUpSummary.followUpStatus());
        vo.setFollowUpTaskCount(followUpSummary.followUpTaskCount());
        vo.setLatestFollowUpTaskId(followUpSummary.latestFollowUpTaskId());
        vo.setLatestFollowUpTaskTitle(followUpSummary.latestFollowUpTaskTitle());
        vo.setLatestFollowUpTaskStatus(followUpSummary.latestFollowUpTaskStatus());
        vo.setLatestFollowUpCreatedAt(followUpSummary.latestFollowUpCreatedAt());
    }

    private List<String> buildDomainRiskReasons(RiskWarningDO warning, List<RiskWarningDetailDO> details) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if (warning.getWarningReason() != null && !warning.getWarningReason().isBlank()) {
            for (String item : warning.getWarningReason().split("\\R")) {
                if (item != null && !item.isBlank()) {
                    reasons.add(item.trim());
                }
            }
        }
        if (warning.getWarningSummary() != null && !warning.getWarningSummary().isBlank()) {
            reasons.add(warning.getWarningSummary().trim());
        }
        if (details != null) {
            for (RiskWarningDetailDO detail : details) {
                if (detail.getDetailDesc() != null && !detail.getDetailDesc().isBlank()) {
                    reasons.add(detail.getDetailDesc().trim());
                } else if (detail.getIndicatorValue() != null && !detail.getIndicatorValue().isBlank()) {
                    reasons.add(detail.getIndicatorValue().trim());
                }
            }
        }
        return new ArrayList<>(reasons);
    }

    private List<String> buildDomainRiskSourceTags(RiskWarningDO warning,
                                                   boolean needHumanReview,
                                                   ReportReviewStatusEnum reviewStatus) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("RISK_WARNING");
        if (warning.getWarningType() != null && !warning.getWarningType().isBlank()) {
            tags.add(warning.getWarningType().trim());
        }
        if (warning.getTriggerSource() != null && !warning.getTriggerSource().isBlank()) {
            tags.add(warning.getTriggerSource().trim());
        }
        if (warning.getTriggerEventId() != null && !warning.getTriggerEventId().isBlank()) {
            tags.add("EVENT_TRIGGERED");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }

    private List<String> mergeRiskReasons(List<String> warningList, List<String> riskPointList) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        reasons.addAll(warningList);
        reasons.addAll(riskPointList);
        return new ArrayList<>(reasons);
    }

    private int riskLevelRank(String riskLevel) {
        RiskLevelEnum resolved = RiskLevelEnum.from(riskLevel);
        if (resolved == null) {
            return 0;
        }
        return switch (resolved) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }

    private LocalDateTime firstNonNull(LocalDateTime left, LocalDateTime right) {
        return left != null ? left : right;
    }

    private List<String> buildRiskSourceTags(List<String> warningList,
                                             List<String> riskPointList,
                                             boolean needHumanReview,
                                             ReportReviewStatusEnum reviewStatus) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (warningList != null && !warningList.isEmpty()) {
            tags.add("WARNING_SIGNAL");
        }
        if (riskPointList != null && !riskPointList.isEmpty()) {
            tags.add("REPORT_RISK_POINT");
        }
        if (needHumanReview) {
            tags.add("HUMAN_REVIEW");
        }
        if (reviewStatus == ReportReviewStatusEnum.REJECTED) {
            tags.add("REVIEW_REJECTED");
        }
        return new ArrayList<>(tags);
    }
}
