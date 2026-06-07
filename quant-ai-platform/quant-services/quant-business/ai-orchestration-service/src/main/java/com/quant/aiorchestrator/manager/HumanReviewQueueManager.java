package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.HumanReviewQueueQueryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueItemVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueuePageVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueStatsVO;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HumanReviewQueueManager {

    public static final String DOMAIN_REPORT = "REPORT";
    public static final String DOMAIN_RISK = "RISK";
    public static final String DOMAIN_COMPLIANCE = "COMPLIANCE";

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final ObjectMapper objectMapper;

    public HumanReviewQueuePageVO pageQueue(HumanReviewQueueQueryDTO queryDTO) {
        HumanReviewQueueQueryDTO safeQuery = queryDTO == null ? new HumanReviewQueueQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<HumanReviewQueueItemVO> records = listQueueItems(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, records.size());
        int toIndex = Math.min(fromIndex + pageSize, records.size());

        HumanReviewQueuePageVO vo = new HumanReviewQueuePageVO();
        vo.setTotal((long) records.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : records.subList(fromIndex, toIndex));
        return vo;
    }

    public HumanReviewQueueStatsVO getStats() {
        List<HumanReviewQueueItemVO> records = listQueueItems(new HumanReviewQueueQueryDTO());
        HumanReviewQueueStatsVO vo = new HumanReviewQueueStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setPendingCount(records.stream().filter(item -> isPending(item.getReviewStatus())).count());
        vo.setApprovedCount(records.stream().filter(item -> ReportReviewStatusEnum.APPROVED.name().equals(item.getReviewStatus())).count());
        vo.setRejectedCount(records.stream().filter(item -> ReportReviewStatusEnum.REJECTED.name().equals(item.getReviewStatus())).count());
        vo.setReportCount(records.stream().filter(item -> DOMAIN_REPORT.equals(item.getDomain())).count());
        vo.setRiskCount(records.stream().filter(item -> DOMAIN_RISK.equals(item.getDomain())).count());
        vo.setComplianceCount(records.stream().filter(item -> DOMAIN_COMPLIANCE.equals(item.getDomain())).count());
        return vo;
    }

    private List<HumanReviewQueueItemVO> listQueueItems(HumanReviewQueueQueryDTO queryDTO) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );
        List<RiskWarningDO> risks = riskWarningMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getDeleted, 0)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
        );

        List<String> taskIds = new ArrayList<>();
        reports.stream().map(ResearchReportDO::getTaskId).filter(this::hasText).forEach(taskIds::add);
        risks.stream().map(RiskWarningDO::getTaskId).filter(this::hasText).forEach(taskIds::add);
        Map<String, ResearchTaskDO> taskMap = loadTaskMap(taskIds);

        List<HumanReviewQueueItemVO> items = new ArrayList<>();
        for (ResearchReportDO report : reports) {
            ResearchTaskDO task = taskMap.get(report.getTaskId());
            if (task == null) {
                continue;
            }
            items.add(toReportQueueItem(report, task, DOMAIN_REPORT));
            if (needsComplianceReview(report)) {
                items.add(toReportQueueItem(report, task, DOMAIN_COMPLIANCE));
            }
        }
        for (RiskWarningDO risk : risks) {
            ResearchTaskDO task = taskMap.get(risk.getTaskId());
            if (task != null) {
                items.add(toRiskQueueItem(risk, task));
            }
        }
        return items.stream()
                .filter(item -> matches(item, queryDTO))
                .sorted(Comparator
                        .comparing(HumanReviewQueueItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HumanReviewQueueItemVO::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private HumanReviewQueueItemVO toReportQueueItem(ResearchReportDO report, ResearchTaskDO task, String domain) {
        HumanReviewQueueItemVO vo = new HumanReviewQueueItemVO();
        vo.setQueueId(domain + ":" + task.getTaskId());
        vo.setDomain(domain);
        hydrateTaskFields(vo, task);
        vo.setReportId(report.getReportId());
        vo.setReportType(firstText(report.getReportType(), report.getTaskType(), task.getTaskType()));
        vo.setRelatedObjectType("REPORT");
        vo.setRelatedObjectId(report.getReportId());
        vo.setReviewStatus(resolveReviewStatus(report.getReviewStatus()).name());
        vo.setNeedHumanReview(report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1);
        vo.setRevised(isReportRevised(report));
        vo.setRerunnable(true);
        vo.setCurrentNode("report_generation_agent");
        vo.setSummary(firstText(report.getRevisedSummary(), report.getSummary()));
        vo.setRiskPoints(readTextList(firstText(report.getRevisedRiskPoints(), report.getRiskPoints())));
        vo.setReviewComment(report.getReviewComment());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setCreatedAt(report.getCreatedAt());
        return vo;
    }

    private HumanReviewQueueItemVO toRiskQueueItem(RiskWarningDO risk, ResearchTaskDO task) {
        HumanReviewQueueItemVO vo = new HumanReviewQueueItemVO();
        vo.setQueueId(DOMAIN_RISK + ":" + task.getTaskId());
        vo.setDomain(DOMAIN_RISK);
        hydrateTaskFields(vo, task);
        vo.setRelatedObjectType("RISK_WARNING");
        vo.setRelatedObjectId(risk.getWarningId());
        vo.setReviewStatus(resolveReviewStatus(risk.getReviewStatus()).name());
        vo.setRiskLevel(risk.getWarningLevel());
        vo.setNeedHumanReview(isHighRisk(risk) || "NEED_HUMAN_REVIEW".equalsIgnoreCase(risk.getSuggestAction()));
        vo.setRevised(false);
        vo.setRerunnable(true);
        vo.setCurrentNode("risk_review_agent");
        vo.setSummary(firstText(risk.getWarningSummary(), risk.getWarningReason()));
        vo.setRiskPoints(readLines(risk.getWarningReason()));
        vo.setReviewComment(risk.getSuggestAction());
        vo.setReviewedBy(risk.getReviewerId());
        vo.setReviewedAt(risk.getReviewTime());
        vo.setCreatedAt(risk.getCreatedAt());
        return vo;
    }

    private void hydrateTaskFields(HumanReviewQueueItemVO vo, ResearchTaskDO task) {
        vo.setTaskId(task.getTaskId());
        vo.setTaskTitle(task.getTaskTitle());
        vo.setTaskType(task.getTaskType());
        vo.setTargetCode(task.getTargetCode());
        vo.setTargetName(task.getTargetName());
        vo.setPriority(task.getPriority());
    }

    private boolean matches(HumanReviewQueueItemVO item, HumanReviewQueueQueryDTO query) {
        if (query == null) {
            return true;
        }
        if (hasText(query.getDomain()) && !query.getDomain().equalsIgnoreCase(item.getDomain())) {
            return false;
        }
        if (hasText(query.getReviewStatus()) && !query.getReviewStatus().equalsIgnoreCase(item.getReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(query.getOnlyPending()) && !isPending(item.getReviewStatus())) {
            return false;
        }
        if (hasText(query.getTargetCode()) && !query.getTargetCode().equalsIgnoreCase(item.getTargetCode())) {
            return false;
        }
        return !hasText(query.getTargetName()) || containsIgnoreCase(item.getTargetName(), query.getTargetName());
    }

    private Map<String, ResearchTaskDO> loadTaskMap(List<String> taskIds) {
        List<String> uniqueIds = taskIds.stream().filter(this::hasText).distinct().toList();
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        return researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, uniqueIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private boolean needsComplianceReview(ResearchReportDO report) {
        return isPending(report.getReviewStatus()) && (
                report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1
                        || !readTextList(report.getRiskWarnings()).isEmpty()
        );
    }

    private boolean isReportRevised(ResearchReportDO report) {
        return hasText(report.getRevisedSummary())
                || hasText(report.getRevisedHighlights())
                || hasText(report.getRevisedRiskPoints());
    }

    private boolean isPending(String reviewStatus) {
        return ReportReviewStatusEnum.PENDING == resolveReviewStatus(reviewStatus);
    }

    private ReportReviewStatusEnum resolveReviewStatus(String reviewStatus) {
        ReportReviewStatusEnum resolved = ReportReviewStatusEnum.from(reviewStatus);
        return resolved == null ? ReportReviewStatusEnum.PENDING : resolved;
    }

    private boolean isHighRisk(RiskWarningDO risk) {
        return risk != null && "HIGH".equalsIgnoreCase(risk.getWarningLevel());
    }

    private List<String> readTextList(String rawJson) {
        if (!hasText(rawJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {})
                    .stream()
                    .filter(this::hasText)
                    .map(String::trim)
                    .toList();
        } catch (Exception ignored) {
            return readLines(rawJson);
        }
    }

    private List<String> readLines(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        return text.lines()
                .map(String::trim)
                .filter(this::hasText)
                .toList();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }
}
